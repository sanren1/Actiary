package com.actiary.domain

import com.actiary.data.db.DailyRecordEntity
import com.actiary.data.db.DayStatus
import com.actiary.data.db.DayWithDetails
import com.actiary.data.db.ExerciseEntryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class RangeStatsTest {
    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)
    private fun entry(typeId: Long, min: Int? = null, cal: Int? = null) =
        ExerciseEntryEntity(0, 0, typeId, min, null, cal, null, 0)
    private fun day(date: LocalDate, status: DayStatus = DayStatus.EXERCISED, vararg e: ExerciseEntryEntity) =
        DayWithDetails(DailyRecordEntity(date.toEpochDay(), status, "", 0), e.toList(), emptyList())

    private val march = rangeOf(d(2025, 3, 15), RangeKind.MONTH, DayOfWeek.MONDAY)

    @Test fun empty() {
        val s = computeStats(emptyList(), march, null)
        assertEquals(RangeStats(0, 0, null, null, emptyList(), s.buckets), s)
        assertEquals(31, s.buckets.size)
        assertEquals(0, s.buckets.sumOf { it.value })
    }

    @Test fun noDurationIsNull_mixedSumsOnlyPresent() {
        val none = computeStats(listOf(day(d(2025, 3, 1), DayStatus.EXERCISED, entry(1))), march, null)
        assertNull(none.totalMinutes)
        assertNull(none.totalCalories)
        val mixed = computeStats(
            listOf(day(d(2025, 3, 1), DayStatus.EXERCISED, entry(1, 30), entry(2), entry(1, 20, 100))), march, null,
        )
        assertEquals(50, mixed.totalMinutes)
        assertEquals(100, mixed.totalCalories)
        assertEquals(3, mixed.entryCount)
    }

    @Test fun typeFilterAndByTypeOrder() {
        val days = listOf(
            day(d(2025, 3, 1), DayStatus.EXERCISED, entry(2, 10), entry(1, 5)),
            day(d(2025, 3, 2), DayStatus.EXERCISED, entry(2, 10)),
            day(d(2025, 3, 3), DayStatus.EXERCISED, entry(3)),
            day(d(2025, 3, 4), DayStatus.EXERCISED, entry(3)),
        )
        assertEquals(listOf(2L to 2, 3L to 2, 1L to 1), computeStats(days, march, null).byType)
        val f = computeStats(days, march, 2L)
        assertEquals(2, f.exerciseDays)
        assertEquals(2, f.entryCount)
        assertEquals(20, f.totalMinutes)
        assertEquals(listOf(2L to 2), f.byType)
        assertEquals(1, f.buckets[0].value)
        assertEquals(0, f.buckets[2].value)
    }

    @Test fun restNotCounted_exercisedWithoutEntriesCounts() {
        val s = computeStats(
            listOf(day(d(2025, 3, 1), DayStatus.REST), day(d(2025, 3, 2), DayStatus.EXERCISED)), march, null,
        )
        assertEquals(1, s.exerciseDays)
        assertEquals(0, s.entryCount)
        assertEquals(0, computeStats(listOf(day(d(2025, 3, 2))), march, 1L).exerciseDays)
    }

    @Test fun weekStart() {
        val wed = d(2025, 3, 5)
        assertEquals(DateRange(RangeKind.WEEK, d(2025, 3, 3), d(2025, 3, 9)), rangeOf(wed, RangeKind.WEEK, DayOfWeek.MONDAY))
        assertEquals(DateRange(RangeKind.WEEK, d(2025, 3, 2), d(2025, 3, 8)), rangeOf(wed, RangeKind.WEEK, DayOfWeek.SUNDAY))
        assertEquals(d(2025, 3, 3), rangeOf(d(2025, 3, 3), RangeKind.WEEK, DayOfWeek.MONDAY).from)
    }

    @Test fun weekAcrossYear() {
        val r = rangeOf(d(2025, 1, 1), RangeKind.WEEK, DayOfWeek.MONDAY)
        assertEquals(d(2024, 12, 30), r.from)
        assertEquals(d(2025, 1, 5), r.to)
        assertEquals(7, computeStats(emptyList(), r, null).buckets.size)
    }

    @Test fun shift() {
        val today = d(2025, 3, 5)
        val mon = DayOfWeek.MONDAY
        val w = rangeOf(today, RangeKind.WEEK, mon)
        assertNull(shiftRange(w, 1, today, mon))
        assertEquals(d(2025, 2, 24), shiftRange(w, -1, today, mon)!!.from)
        val m = rangeOf(today, RangeKind.MONTH, mon)
        assertNull(shiftRange(m, 1, today, mon))
        assertEquals(DateRange(RangeKind.MONTH, d(2025, 2, 1), d(2025, 2, 28)), shiftRange(m, -1, today, mon))
        val y = rangeOf(today, RangeKind.YEAR, mon)
        assertNull(shiftRange(y, 1, today, mon))
        assertEquals(d(2024, 1, 1), shiftRange(y, -1, today, mon)!!.from)
        assertEquals(m, shiftRange(shiftRange(m, -1, today, mon)!!, 1, today, mon))
    }

    @Test fun yearMonthBuckets() {
        val y = rangeOf(d(2025, 6, 1), RangeKind.YEAR, DayOfWeek.MONDAY)
        val s = computeStats(
            listOf(
                day(d(2025, 1, 5), DayStatus.EXERCISED, entry(1), entry(1)),
                day(d(2025, 1, 9), DayStatus.EXERCISED, entry(1)),
                day(d(2025, 12, 31)),
                day(d(2025, 2, 1), DayStatus.REST),
                day(d(2024, 12, 31), DayStatus.EXERCISED, entry(1)),
            ),
            y, null,
        )
        assertEquals(12, s.buckets.size)
        assertEquals(Bucket(d(2025, 1, 1), 2), s.buckets[0])
        assertEquals(0, s.buckets[1].value)
        assertEquals(1, s.buckets[11].value)
        assertEquals(3, s.exerciseDays)
    }
}
