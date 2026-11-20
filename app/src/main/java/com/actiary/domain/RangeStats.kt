package com.actiary.domain

import com.actiary.data.db.DayStatus
import com.actiary.data.db.DayWithDetails
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

enum class RangeKind { WEEK, MONTH, YEAR }

data class DateRange(val kind: RangeKind, val from: LocalDate, val to: LocalDate)

data class Bucket(val start: LocalDate, val value: Int)

data class RangeStats(
    val exerciseDays: Int,
    val entryCount: Int,
    val totalMinutes: Int?,
    val totalCalories: Int?,
    val byType: List<Pair<Long, Int>>,
    val buckets: List<Bucket>,
)

fun rangeOf(anchor: LocalDate, kind: RangeKind, firstDayOfWeek: DayOfWeek): DateRange = when (kind) {
    RangeKind.WEEK -> anchor.with(TemporalAdjusters.previousOrSame(firstDayOfWeek)).let { DateRange(kind, it, it.plusDays(6)) }
    RangeKind.MONTH -> YearMonth.from(anchor).let { DateRange(kind, it.atDay(1), it.atEndOfMonth()) }
    RangeKind.YEAR -> DateRange(kind, anchor.withDayOfYear(1), anchor.with(TemporalAdjusters.lastDayOfYear()))
}

/** Null when the target range starts after [today], i.e. beyond the range containing today. */
fun shiftRange(range: DateRange, delta: Int, today: LocalDate, firstDayOfWeek: DayOfWeek): DateRange? {
    val anchor = when (range.kind) {
        RangeKind.WEEK -> range.from.plusWeeks(delta.toLong())
        RangeKind.MONTH -> range.from.plusMonths(delta.toLong())
        RangeKind.YEAR -> range.from.plusYears(delta.toLong())
    }
    return rangeOf(anchor, range.kind, firstDayOfWeek).takeIf { !it.from.isAfter(today) }
}

fun computeStats(days: List<DayWithDetails>, range: DateRange, typeFilter: Long?): RangeStats {
    val inRange = days.filter { d ->
        LocalDate.ofEpochDay(d.record.epochDay).let { !it.isBefore(range.from) && !it.isAfter(range.to) }
    }
    fun DayWithDetails.entriesF() =
        if (record.status != DayStatus.EXERCISED) emptyList() else entries.filter { typeFilter == null || it.typeId == typeFilter }
    fun DayWithDetails.counts() =
        record.status == DayStatus.EXERCISED && (typeFilter == null || entriesF().isNotEmpty())

    val entries = inRange.flatMap { it.entriesF() }
    val byType = entries.groupingBy { it.typeId }.eachCount().toList()
        .sortedWith(compareByDescending<Pair<Long, Int>> { it.second }.thenBy { it.first })
    val buckets = if (range.kind == RangeKind.YEAR) {
        (1..12).map { m ->
            Bucket(
                LocalDate.of(range.from.year, m, 1),
                inRange.count { it.counts() && LocalDate.ofEpochDay(it.record.epochDay).monthValue == m },
            )
        }
    } else {
        val byDay = inRange.associateBy { it.record.epochDay }
        generateSequence(range.from) { it.plusDays(1) }.takeWhile { !it.isAfter(range.to) }
            .map { d -> Bucket(d, byDay[d.toEpochDay()]?.entriesF()?.size ?: 0) }
            .toList()
    }
    return RangeStats(
        exerciseDays = inRange.count { it.counts() },
        entryCount = entries.size,
        totalMinutes = entries.mapNotNull { it.durationMinutes }.takeIf { it.isNotEmpty() }?.sum(),
        totalCalories = entries.mapNotNull { it.calories }.takeIf { it.isNotEmpty() }?.sum(),
        byType = byType,
        buckets = buckets,
    )
}
