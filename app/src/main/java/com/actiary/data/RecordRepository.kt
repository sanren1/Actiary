package com.actiary.data

import com.actiary.data.db.DailyRecordEntity
import com.actiary.data.db.DayWithDetails
import com.actiary.data.db.ExerciseEntryEntity
import com.actiary.data.db.ExerciseTypeEntity
import com.actiary.data.db.PhotoAttachmentEntity
import com.actiary.data.db.RecordDao
import com.actiary.data.db.StrengthLogItemEntity
import com.actiary.data.photo.PhotoStore
import com.actiary.domain.DayDraft
import com.actiary.domain.DayState
import com.actiary.domain.DraftPhoto
import com.actiary.domain.toDayState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.time.LocalDate
import java.time.YearMonth

class RecordRepository(
    private val dao: RecordDao,
    private val photos: PhotoStore,
    private val openUri: (String) -> InputStream,
    private val clock: () -> Long = System::currentTimeMillis,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val onChanged: () -> Unit = {},
) {
    fun observeMonth(month: YearMonth): Flow<Map<LocalDate, DayState>> =
        dao.observeRange(month.atDay(1).toEpochDay(), month.atEndOfMonth().toEpochDay()).map { days ->
            days.associate { LocalDate.ofEpochDay(it.record.epochDay) to it.record.toDayState() }
        }

    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<DayWithDetails>> =
        dao.observeRange(from.toEpochDay(), to.toEpochDay())

    fun observeDay(date: LocalDate): Flow<DayWithDetails?> = dao.observeDay(date.toEpochDay())

    val types: Flow<List<ExerciseTypeEntity>> get() = dao.observeTypes()

    val allTypes: Flow<List<ExerciseTypeEntity>> get() = dao.observeAllTypes()

    internal suspend fun stagePickedPhotos(uris: List<String>): List<DraftPhoto.Staged> {
        val staged = mutableListOf<DraftPhoto.Staged>()
        try {
            return withContext(io) {
                for (uri in uris) {
                    ensureActive()
                    staged += DraftPhoto.Staged(photos.stage { openUri(uri) })
                }
                staged
            }
        } catch (failure: Throwable) {
            // Also handles cancellation when withContext drops a completed result on return.
            withContext(io + NonCancellable) {
                staged.forEach { photos.stagedFile(it.fileName).delete() }
            }
            throw failure
        }
    }

    suspend fun save(draft: DayDraft) = withContext(io) {
        photos.mutations.withLock {
            val epochDay = draft.date.toEpochDay()
            val original = dao.get(epochDay)
            val copied = mutableListOf<String>()
            val names = try {
                val names = draft.photos.map {
                    when (it) {
                        is DraftPhoto.Saved -> it.fileName
                        is DraftPhoto.Picked -> photos.copyIn { openUri(it.uri) }.also(copied::add)
                        is DraftPhoto.Staged -> photos.copyRaw(photos.stagedFile(it.fileName)::inputStream).also(copied::add)
                    }
                }
                val now = clock()
                val strengthLogsByEntryIndex = buildMap<Int, List<StrengthLogItemEntity>> {
                    draft.entries.forEachIndexed { entryIndex, entry ->
                        if (entry.strengthItems.isNotEmpty()) {
                            put(
                                entryIndex,
                                entry.strengthItems.mapIndexed { itemIndex, item ->
                                    StrengthLogItemEntity(
                                        id = 0,
                                        entryId = 0,
                                        exerciseId = item.exerciseId,
                                        sets = item.sets,
                                        reps = item.reps,
                                        weightKg = item.weightKg,
                                        sortOrder = itemIndex,
                                    )
                                },
                            )
                        }
                    }
                }
                dao.replaceDay(
                    DailyRecordEntity(epochDay, draft.status, draft.note, now),
                    draft.entries.mapIndexed { i, e ->
                        ExerciseEntryEntity(
                            0, epochDay, e.typeId, e.durationMinutes, e.distanceMeters, e.calories, e.avgHeartRate, i, e.metrics, e.planId,
                        )
                    },
                    names.mapIndexed { i, n -> PhotoAttachmentEntity(0, epochDay, n, i, now) },
                    strengthLogsByEntryIndex,
                )
                names
            } catch (e: Throwable) {
                copied.forEach(photos::delete)
                throw e
            }
            // Only after the commit: drop files the draft no longer references.
            val kept = names.toSet()
            original?.photos?.forEach { if (it.fileName !in kept) photos.delete(it.fileName) }
        }
        onChanged()
    }

    suspend fun addType(name: String, hasDistance: Boolean, iconKey: String?, extraMetricMask: Int? = null): Long = withContext(io) {
        dao.appendType(
            ExerciseTypeEntity(
                builtinKey = null,
                name = name.trim(),
                sortOrder = 0,
                hasDistance = hasDistance,
                iconKey = iconKey,
                extraMetricMask = extraMetricMask,
            ),
        )
    }

    suspend fun moveType(id: Long, offset: Int) = withContext(io) {
        dao.moveType(id, offset)
    }

    internal suspend fun updateType(
        id: Long, name: String?, hasDistance: Boolean, iconKey: String?, extraMetricMask: Int? = null,
    ) = withContext(io) {
        check(dao.updateType(id, name?.trim()?.takeIf(String::isNotEmpty), hasDistance, iconKey, extraMetricMask) == 1)
        onChanged()
    }

    suspend fun removeType(id: Long) = withContext(io) {
        dao.removeType(id)
        onChanged()
    }

    suspend fun setBuiltinTypeHidden(id: Long, hidden: Boolean) = withContext(io) {
        dao.setBuiltinTypeHidden(id, hidden)
        onChanged()
    }

    suspend fun clearDay(date: LocalDate) = withContext(io) {
        photos.mutations.withLock {
            val epochDay = date.toEpochDay()
            val files = dao.get(epochDay)?.photos.orEmpty()
            dao.deleteDay(epochDay)
            files.forEach { photos.delete(it.fileName) }
        }
        onChanged()
    }
}
