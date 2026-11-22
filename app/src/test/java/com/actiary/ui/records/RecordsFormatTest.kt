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

    @Test fun emptyTypeKeepsRangeAndChartSlotsWithAnEmptyTimeline() {
        val result = recordsPresentation(year, days, 3)

        assertEquals(year, result.range)
        assertEquals(3L, result.filter)
        assertEquals(0, result.stats.entryCount)
        assertEquals((1..12).toList(), result.stats.buckets.map { it.start.monthValue })
        assertEquals(emptyList<TimelineItem>(), result.timeline)
    }

    @Test fun chartAxisTicksForWeekMonthYear() {
        assertEquals(listOf(0, 2, 4, 6), chartAxisTicks(RangeKind.WEEK, 7))
        assertEquals(listOf(0, 9, 19, 27), chartAxisTicks(RangeKind.MONTH, 28))
        assertEquals(listOf(0, 9, 19, 28), chartAxisTicks(RangeKind.MONTH, 29))
        assertEquals(listOf(0, 9, 19, 29), chartAxisTicks(RangeKind.MONTH, 30))
        assertEquals(listOf(0, 9, 19, 30), chartAxisTicks(RangeKind.MONTH, 31))
        assertEquals(listOf(0, 2, 5, 8, 11), chartAxisTicks(RangeKind.YEAR, 12))
        assertEquals(emptyList<Int>(), chartAxisTicks(RangeKind.WEEK, 0))
        assertEquals(listOf(0), chartAxisTicks(RangeKind.WEEK, 1))
    }

    @Test fun matchedBarIndicesAreStrictlyIncreasingAcrossAllPeriodTransitions() {
        for ((small, large) in listOf(7 to 12, 7 to 28, 7 to 30, 7 to 31, 12 to 28, 12 to 31)) {
            val indices = matchedBarIndices(small, large)
            assertEquals(small, indices.size)
            assertEquals(indices.distinct(), indices)
            assertEquals(indices.sorted(), indices)
        }
    }

    @Test fun interpolateBarsMorphsMatchedBarsAndReturnsUnmatchedToBaseline() {
        val week = buildBarSpecs(listOf(10, 20, 30, 40, 50, 60, 70), maxIndex = 6, rtl = false)
        val month = buildBarSpecs(List(30) { (it + 1) * 10 }, maxIndex = 29, rtl = false)

        val midExpand = interpolateBars(week, month, 0.5f)
        assertEquals(30, midExpand.size)
        assertEquals(7, midExpand.count { it.alpha == 1f })
        assertEquals(23, midExpand.count { it.alpha == 0.5f })
        assertEquals(month, interpolateBars(week, month, 1f))

        val midShrink = interpolateBars(month, week, 0.5f)
        assertEquals(30, midShrink.size)
        assertEquals(7, midShrink.count { it.alpha == 1f })
        assertEquals(23, midShrink.count { it.alpha == 0.5f })
        assertEquals(week, interpolateBars(month, week, 1f))
    }
}
