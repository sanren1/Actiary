package com.actiary.ui.record

import com.actiary.R
import com.actiary.data.db.DailyRecordEntity
import com.actiary.data.db.DayStatus
import com.actiary.data.db.DayWithDetails
import com.actiary.data.db.ExerciseEntryEntity
import com.actiary.data.db.ExerciseTypeEntity
import com.actiary.data.db.PhotoAttachmentEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordDetailModelTest {
    private val swimming = ExerciseTypeEntity(1, "swimming", null, 0, hasDistance = true)
    private val running = ExerciseTypeEntity(2, "running", null, 1, hasDistance = true)
    private val types = listOf(swimming, running).associateBy { it.id }

    private fun model(
        status: DayStatus = DayStatus.EXERCISED,
        entries: List<ExerciseEntryEntity> = emptyList(),
        note: String = "",
        photos: List<PhotoAttachmentEntity> = emptyList(),
    ): DetailModel = buildDetailModel(DayWithDetails(DailyRecordEntity(0, status, note, 0), entries, photos), types) { unit, value ->
        when (unit) {
            R.string.records_min -> "$value min"
            R.string.records_kcal -> "$value kcal"
            R.string.record_detail_bpm -> "$value bpm"
            R.string.records_m -> "$value m"
            R.string.records_km -> "$value km"
            else -> error("Unexpected metric format $unit")
        }
    }

    @Test fun restDayIsNotAnEmptyWorkout() {
        val detail = model(status = DayStatus.REST)
        assertEquals(DayStatus.REST, detail.status)
        assertFalse(detail.empty)
    }

    @Test fun bareCheckInHasEmptyWorkoutMessage() {
        assertTrue(model().empty)
    }

    @Test fun sameTypeSessionsKeepTheirOwnPopulatedMetrics() {
        val detail = model(entries = listOf(
            ExerciseEntryEntity(2, 0, running.id, null, null, 92, 131, 1),
            ExerciseEntryEntity(1, 0, running.id, 0, null, null, null, 0),
        ))
        assertEquals(
            listOf(
                DetailEntry(running.id, listOf(R.string.records_stat_duration to "0 min")),
                DetailEntry(running.id, listOf(
                    R.string.records_stat_calories to "92 kcal",
                    R.string.record_heart_rate to "131 bpm",
                )),
            ),
            detail.entries,
        )
        assertFalse(detail.empty)
    }

    @Test fun swimmingUsesMetersAndRunningUsesKilometers() {
        val detail = model(entries = listOf(
            ExerciseEntryEntity(1, 0, swimming.id, null, 1250, null, null, 0),
            ExerciseEntryEntity(2, 0, running.id, null, 1250, null, null, 1),
        ))
        assertEquals(
            listOf(
                DetailEntry(swimming.id, listOf(R.string.record_detail_distance to "1250 m")),
                DetailEntry(running.id, listOf(R.string.record_detail_distance to "1.25 km")),
            ),
            detail.entries,
        )
    }

    @Test fun keepsFullNoteAndAllPhotosInSavedOrder() {
        val note = "Warm-up\n" + "Long note. ".repeat(30)
        val photos = List(9) { PhotoAttachmentEntity(epochDay = 0, fileName = "photo-$it.jpg", sortOrder = it, addedAt = 0) }
        val detail = model(note = note, photos = photos.reversed())
        assertEquals(note, detail.note)
        assertEquals(List(9) { "photo-$it.jpg" }, detail.photos)
        assertFalse(detail.empty)
    }
}
