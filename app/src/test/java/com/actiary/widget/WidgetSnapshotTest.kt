package com.actiary.widget

import com.actiary.data.db.DailyRecordEntity
import com.actiary.data.db.DayStatus
import com.actiary.data.db.DayWithDetails
import com.actiary.data.db.ExerciseEntryEntity
import com.actiary.data.db.ExerciseTypeEntity
import com.actiary.data.db.PhotoAttachmentEntity
import com.actiary.domain.DayState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WidgetSnapshotTest {
    private val today = LocalDate.of(2026, 11, 22)

    private fun day(
        date: LocalDate,
        status: DayStatus,
        note: String = "",
        typeIds: List<Long> = emptyList(),
        photos: Int = 0,
    ) = DayWithDetails(
        record = DailyRecordEntity(date.toEpochDay(), status, note, 0L),
        entries = typeIds.mapIndexed { idx, id ->
            ExerciseEntryEntity(0, date.toEpochDay(), id, 30, null, null, null, idx)
        },
        photos = List(photos) { idx ->
            PhotoAttachmentEntity(0, date.toEpochDay(), "p$idx.jpg", idx, 0L)
        },
    )

    @Test
    fun decideWidgetRestActionProtectsExistingWorkoutsAndNotes() {
        assertEquals(WidgetRestAction.MARK_REST, decideWidgetRestAction(null))
        assertEquals(WidgetRestAction.CLEAR_REST, decideWidgetRestAction(day(today, DayStatus.REST)))
        assertEquals(
            WidgetRestAction.OPEN_EDITOR,
            decideWidgetRestAction(day(today, DayStatus.REST, note = "recovering")),
        )
        assertEquals(
            WidgetRestAction.OPEN_EDITOR,
            decideWidgetRestAction(day(today, DayStatus.REST, photos = 1)),
        )
        assertEquals(
            WidgetRestAction.OPEN_EDITOR,
            decideWidgetRestAction(day(today, DayStatus.EXERCISED, typeIds = listOf(1L))),
        )
    }

    @Test
    fun buildWidgetSnapshotBuildsSevenDayStripAndDistinctLabels() {
        val types = mapOf(
            1L to ExerciseTypeEntity(id = 1L, builtinKey = "running", name = "Running", sortOrder = 0),
            2L to ExerciseTypeEntity(id = 2L, builtinKey = "cycling", name = "Cycling", sortOrder = 1),
        )
        val recent = listOf(
            day(today.minusDays(6), DayStatus.EXERCISED, typeIds = listOf(1L)),
            day(today.minusDays(3), DayStatus.REST),
            day(today, DayStatus.EXERCISED, typeIds = listOf(1L, 2L, 1L)),
        )

        val snapshot = buildWidgetSnapshot(today, recent, types) { it.name.orEmpty() }

        assertEquals(DayState.EXERCISED, snapshot.todayState)
        assertEquals(2, snapshot.recentExercisedDays)
        assertEquals(listOf("Running", "Cycling"), snapshot.typeLabels)
        assertEquals(7, snapshot.weekDots.size)
        assertEquals(today.minusDays(6), snapshot.weekDots.first().date)
        assertEquals(today, snapshot.weekDots.last().date)
        assertTrue(snapshot.weekDots.last().isToday)
        assertFalse(snapshot.weekDots.first().isToday)
        assertEquals(
            listOf(
                DayState.EXERCISED,
                DayState.UNRECORDED,
                DayState.UNRECORDED,
                DayState.REST,
                DayState.UNRECORDED,
                DayState.UNRECORDED,
                DayState.EXERCISED,
            ),
            snapshot.weekDots.map { it.state },
        )
    }
}
