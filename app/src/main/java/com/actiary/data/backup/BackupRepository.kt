package com.actiary.data.backup

import androidx.room.withTransaction
import com.actiary.data.appearance.AppearanceRepository
import com.actiary.data.db.ActiaryDatabase
import com.actiary.data.db.DailyRecordEntity
import com.actiary.data.db.ExerciseEntryEntity
import com.actiary.data.db.ExerciseTypeEntity
import com.actiary.data.db.PhotoAttachmentEntity
import com.actiary.data.photo.PhotoStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files

enum class ImportMode { OVERWRITE, MERGE }

data class ExportSummary(val days: Int, val photos: Int)
data class ImportSummary(val written: Int, val skipped: Int)

class BackupRepository(
    private val db: ActiaryDatabase,
    private val photos: PhotoStore,
    private val appearance: AppearanceRepository,
    private val appVersion: String,
    cacheDir: File,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val dao = db.recordDao()
    private val importCache by lazy {
        // The app owns one repository per process. Existing sessions here belong to an earlier process.
        File(cacheDir, "import").also { directory ->
            directory.deleteRecursively()
            Files.createDirectories(directory.toPath())
        }
    }

    suspend fun export(out: OutputStream): ExportSummary = withContext(io) {
        photos.mutations.withLock {
            val backup = db.withTransaction {
                BackupData(
                    BACKUP_FORMAT, appVersion, System.currentTimeMillis(), appearance.exportAppearance(),
                    dao.getAllTypes().map {
                        BackupType(it.id, it.builtinKey, it.name, it.sortOrder, it.archived, it.hasDistance, it.iconKey,
                            it.extraMetricMask)
                    },
                    dao.getAllRecords().map { BackupRecord(it.epochDay, it.status, it.note, it.updatedAt) },
                    dao.getAllEntries().map {
                        BackupEntry(it.id, it.epochDay, it.typeId, it.durationMinutes, it.distanceMeters,
                            it.calories, it.avgHeartRate, it.sortOrder, it.metrics)
                    },
                    dao.getAllPhotos().map { BackupPhoto(it.epochDay, it.fileName, it.sortOrder, it.addedAt) },
                )
            }
            val files = backup.photos.map { it.fileName }.distinct().mapNotNull { name ->
                name.takeIf(::isPhotoFileName)?.let { photos.file(it).takeIf(File::isFile) }?.let { name to it }
            }.toMap()
            val attachments = backup.photos.filter { it.fileName in files }
            writeBackup(out, backup.copy(photos = attachments), files)
            ExportSummary(backup.records.size, attachments.size)
        }
    }

    suspend fun load(input: InputStream): LoadedBackup {
        var loaded: LoadedBackup? = null
        try {
            return withContext(io) { unpackBackup(input, importCache).also { loaded = it } }
        } catch (failure: Throwable) {
            // A canceled withContext can discard the result after the archive has been staged.
            loaded?.let { withContext(io + NonCancellable) { it.close() } }
            throw failure
        }
    }

    suspend fun apply(data: LoadedBackup, mode: ImportMode): ImportSummary = withContext(io) {
        photos.mutations.withLock {
            ensureActive()
            // Observe the transaction's actual outcome before compensating the file and DataStore writes.
            withContext(NonCancellable) { applyInTransaction(data, mode) }
        }
    }

    private suspend fun applyInTransaction(data: LoadedBackup, mode: ImportMode): ImportSummary {
        val backup = data.backup
        val overwrite = mode == ImportMode.OVERWRITE
        val copies = mutableListOf<String>()
        var oldPhotoNames = emptyList<String>()
        var oldAppearance: Map<String, String>? = null
        val summary = try {
            db.withTransaction {
                val localDays = if (overwrite) emptySet() else dao.getAllRecords().map { it.epochDay }.toSet()
                val local = LocalSnapshot(dao.getAllTypes(), localDays)
                val plan = if (overwrite) planOverwrite(local, backup) else planMerge(local, backup)
                if (overwrite) {
                    oldPhotoNames = dao.getAllPhotoFileNames()
                    dao.deleteAllEntries()
                    dao.deleteAllPhotoRows()
                    dao.deleteAllRecords()
                    dao.deleteCustomTypes()
                }
                val createdIds = mutableMapOf<Long, Long>()
                val typesById = backup.types.associateBy { it.id }
                val typeIds = plan.typeMap.mapValues { (oldId, target) ->
                    when (target) {
                        is TypeTarget.Create -> createdIds.getOrPut(target.spec.id) {
                            val type = target.spec
                            dao.insertType(ExerciseTypeEntity(
                                builtinKey = type.builtinKey, name = if (type.builtinKey == null) type.name?.trim() else type.name,
                                sortOrder = type.sortOrder, archived = type.archived, hasDistance = type.hasDistance,
                                iconKey = type.iconKey, extraMetricMask = type.extraMetricMask,
                            ))
                        }
                        is TypeTarget.Existing -> {
                            if (overwrite) {
                                val type = typesById.getValue(oldId)
                                dao.updateBuiltinType(requireNotNull(type.builtinKey), type.name, type.sortOrder,
                                    type.archived, type.hasDistance, type.iconKey, type.extraMetricMask)
                            }
                            target.id
                        }
                    }
                }
                val days = plan.daysToWrite.toSet()
                dao.insertRecords(backup.records.filter { it.epochDay in days }.map {
                    DailyRecordEntity(it.epochDay, it.status, it.note, it.updatedAt)
                })
                dao.insertEntries(backup.entries.filter { it.epochDay in days }.map {
                    ExerciseEntryEntity(epochDay = it.epochDay, typeId = typeIds.getValue(it.typeId),
                        durationMinutes = it.durationMinutes, distanceMeters = it.distanceMeters,
                        calories = it.calories, avgHeartRate = it.avgHeartRate, sortOrder = it.sortOrder, metrics = it.metrics)
                })
                dao.insertPhotos(backup.photos.filter { it.epochDay in days }.mapNotNull {
                    val source = data.photo(it.fileName) ?: return@mapNotNull null
                    val name = photos.copyIn(source::inputStream).also(copies::add)
                    PhotoAttachmentEntity(epochDay = it.epochDay, fileName = name, sortOrder = it.sortOrder, addedAt = it.addedAt)
                })
                if (overwrite) {
                    val previous = appearance.exportAppearance()
                    appearance.importAppearance(backup.appearance, replace = true)
                    oldAppearance = previous
                }
                ImportSummary(days.size, plan.skippedDays)
            }
        } catch (failure: Throwable) {
            copies.forEach(photos::delete)
            oldAppearance?.let { previous ->
                runCatching { appearance.restoreAppearance(previous) }.onFailure(failure::addSuppressed)
            }
            throw failure
        }
        // Restored attachments own new local names; only superseded attachments are removed.
        oldPhotoNames.filter(::isPhotoFileName).forEach(photos::delete)
        return summary
    }

    suspend fun orphanPhotoCount(): Int = withContext(io) {
        photos.mutations.withLock { orphanPhotoNames().size }
    }

    suspend fun deleteOrphanPhotos(): Int = withContext(io) {
        photos.mutations.withLock { orphanPhotoNames().count(photos::delete) }
    }

    private suspend fun orphanPhotoNames(): List<String> {
        val referenced = dao.getAllPhotoFileNames().toSet()
        return photos.listFileNames().filter { it !in referenced }
    }
}
