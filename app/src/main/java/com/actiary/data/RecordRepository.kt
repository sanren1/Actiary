package com.actiary.data

import com.actiary.data.db.DailyRecordEntity
import com.actiary.data.db.DayStatus
import com.actiary.data.db.DayWithDetails
import com.actiary.data.db.ExerciseEntryEntity
import com.actiary.data.db.ExerciseTypeEntity
import com.actiary.data.db.PhotoAttachmentEntity
import com.actiary.data.db.RecordDao
import com.actiary.data.photo.PhotoStore
import com.actiary.domain.DayDraft
import com.actiary.domain.DayState
import com.actiary.domain.DraftPhoto
import com.actiary.domain.toDayState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
) {
    fun observeMonth(month: YearMonth): Flow<Map<LocalDate, DayState>> =
        dao.observeRange(month.atDay(1).toEpochDay(), month.atEndOfMonth().toEpochDay()).map { days ->
            days.associate { LocalDate.ofEpochDay(it.record.epochDay) to it.record.toDayState() }
        }

    fun observeDay(date: LocalDate): Flow<DayWithDetails?> = dao.observeDay(date.toEpochDay())

    val types: Flow<List<ExerciseTypeEntity>> get() = dao.observeTypes()

    val allTypes: Flow<List<ExerciseTypeEntity>> get() = dao.observeAllTypes()

    suspend fun save(draft: DayDraft) = withContext(io) {
        val epochDay = draft.date.toEpochDay()
        val original = dao.get(epochDay)
        val copied = mutableListOf<String>()
        val names = try {
            val names = draft.photos.map {
                when (it) {
                    is DraftPhoto.Saved -> it.fileName
                    is DraftPhoto.Picked -> photos.copyIn { openUri(it.uri) }.also(copied::add)
                }
            }
            val now = clock()
            dao.replaceDay(
                DailyRecordEntity(epochDay, draft.status, draft.note, now),
                draft.entries.mapIndexed { i, e ->
                    ExerciseEntryEntity(0, epochDay, e.typeId, e.durationMinutes, e.distanceMeters, e.calories, e.avgHeartRate, i)
                },
                names.mapIndexed { i, n -> PhotoAttachmentEntity(0, epochDay, n, i, now) },
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

    suspend fun addType(name: String, hasDistance: Boolean, iconKey: String?): Long = withContext(io) {
        dao.appendType(
            ExerciseTypeEntity(
                builtinKey = null,
                name = name.trim(),
                sortOrder = 0,
                hasDistance = hasDistance,
                iconKey = iconKey,
            ),
        )
    }

    suspend fun moveType(id: Long, offset: Int) = withContext(io) {
        dao.moveType(id, offset)
    }

    suspend fun removeType(id: Long) = withContext(io) {
        dao.removeType(id)
    }

    suspend fun quickCheckIn(date: LocalDate) {
        dao.insertRecordIfAbsent(DailyRecordEntity(date.toEpochDay(), DayStatus.EXERCISED, "", clock()))
    }

    suspend fun clearDay(date: LocalDate) = withContext(io) {
        val epochDay = date.toEpochDay()
        val files = dao.get(epochDay)?.photos.orEmpty()
        dao.deleteDay(epochDay)
        files.forEach { photos.delete(it.fileName) }
    }
}
