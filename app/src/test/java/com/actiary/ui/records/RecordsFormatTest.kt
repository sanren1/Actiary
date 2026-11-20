package com.actiary.ui.records

import com.actiary.data.db.DailyRecordEntity
import com.actiary.data.db.DayStatus
import com.actiary.data.db.DayWithDetails
import com.actiary.data.db.ExerciseEntryEntity
import com.actiary.domain.RangeKind
import com.actiary.domain.rangeOf
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class RecordsFormatTest {
    private fun day(date: LocalDate, status: DayStatus, vararg typeIds: Long) = DayWithDetails(
        DailyRecordEntity(date.toEpochDay(), status, "", 0),
        typeIds.map { ExerciseEntryEntity(0, date.toEpochDay(), it, null, null, null, null, 0) },
        emptyList(),
    )

    private val year = rangeOf(LocalDate.of(2025, 3, 15), RangeKind.YEAR, DayOfWeek.MONDAY)
    private val days = listOf(
        day(LocalDate.of(2025, 2, 27), DayStatus.EXERCISED, 1),
        day(LocalDate.of(2025, 3, 1), DayStatus.REST),
        day(LocalDate.of(2025, 3, 2), DayStatus.EXERCISED, 1, 2),
        day(LocalDate.of(2026, 1, 1), DayStatus.EXERCISED, 1),
    )

    @Test fun splitMinutes() {
        assertEquals(0 to 59, splitMinutes(59))
        assertEquals(1 to 0, splitMinutes(60))
        assertEquals(2 to 5, splitMinutes(125))
    }

    @Test fun newestFirstGroupedByMonthAndRangeBounded() {
        val items = timelineItems(days, year, null)
        assertEquals(
            listOf(YearMonth.of(2025, 3), YearMonth.of(2025, 2)),
            items.filterIsInstance<TimelineItem.Month>().map { it.month },
        )
        assertEquals(
            listOf(LocalDate.of(2025, 3, 2), LocalDate.of(2025, 3, 1), LocalDate.of(2025, 2, 27)).map { it.toEpochDay() },
            items.filterIsInstance<TimelineItem.Day>().map { it.day.record.epochDay },
        )
    }

    @Test fun filterDropsRestAndOtherTypes() {
        val days = timelineItems(days, year, 2).filterIsInstance<TimelineItem.Day>()
        assertEquals(1, days.size)
        assertEquals(listOf(2L), days[0].day.entries.map { it.typeId })
    }
}
