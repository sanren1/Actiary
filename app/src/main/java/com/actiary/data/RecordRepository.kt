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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.InputStream
import java.time.LocalDate
import java.time.YearMonth

class RecordRepository(
    private val dao: RecordDao,
    private val photos: PhotoStore,
    private val openUri: (String) -> InputStream,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    fun observeMonth(month: YearMonth): Flow<Map<LocalDate, DayState>> =
        dao.observeRange(month.atDay(1).toEpochDay(), month.atEndOfMonth().toEpochDay()).map { days ->
            days.associate { LocalDate.ofEpochDay(it.record.epochDay) to it.record.toDayState() }
        }

    fun observeDay(date: LocalDate): Flow<DayWithDetails?> = dao.observeDay(date.toEpochDay())

    val types: Flow<List<ExerciseTypeEntity>> get() = dao.observeTypes()

    suspend fun save(draft: DayDraft) {
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
                    ExerciseEntryEntity(0, epochDay, e.typeId, e.durationMinutes, e.distanceMeters, i)
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

    suspend fun quickCheckIn(date: LocalDate) {
        val epochDay = date.toEpochDay()
        if (dao.get(epochDay) != null) return
        dao.replaceDay(DailyRecordEntity(epochDay, DayStatus.EXERCISED, "", clock()), emptyList(), emptyList())
    }

    suspend fun clearDay(date: LocalDate) {
        val epochDay = date.toEpochDay()
        val files = dao.get(epochDay)?.photos.orEmpty()
        dao.deleteDay(epochDay)
        files.forEach { photos.delete(it.fileName) }
    }
}
