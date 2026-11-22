package com.actiary.ui.records

import com.actiary.R
import com.actiary.data.db.DayStatus
import com.actiary.data.db.DayWithDetails
import com.actiary.data.db.ExerciseEntryEntity
import com.actiary.data.db.ExerciseTypeEntity
import com.actiary.domain.DateRange
import com.actiary.domain.RangeStats
import com.actiary.domain.computeStats
import com.actiary.ui.record.formatDistance
import java.time.LocalDate
import java.time.YearMonth

sealed interface TimelineItem {
    data class Month(val month: YearMonth) : TimelineItem
    data class Day(val day: DayWithDetails) : TimelineItem
}

internal data class RecordsPresentation(
    val range: DateRange,
    val filter: Long?,
    val stats: RangeStats,
    val timeline: List<TimelineItem>,
)

internal fun recordsPresentation(range: DateRange, days: List<DayWithDetails>, filter: Long?): RecordsPresentation =
    RecordsPresentation(range, filter, computeStats(days, range, filter), timelineItems(days, range, filter))

fun splitMinutes(total: Int): Pair<Int, Int> = total / 60 to total % 60

internal fun entryMetrics(
    entry: ExerciseEntryEntity,
    type: ExerciseTypeEntity,
    format: (Int, Any) -> String,
): List<Pair<Int, String>> = buildList {
    entry.durationMinutes?.let { add(R.string.records_stat_duration to format(R.string.records_min, it)) }
    entry.calories?.let { add(R.string.records_stat_calories to format(R.string.records_kcal, it)) }
    entry.avgHeartRate?.let { add(R.string.record_heart_rate to format(R.string.record_detail_bpm, it)) }
    val distance = formatDistance(entry.distanceMeters, type.builtinKey, type.hasDistance)
    if (distance.isNotEmpty()) {
        val unit = if (type.builtinKey == "swimming") R.string.records_m else R.string.records_km
        add(R.string.record_detail_distance to format(unit, distance))
    }
}

/** Newest first, grouped by month. With a type filter only exercised days with that type remain, showing only its entries. */
fun timelineItems(days: List<DayWithDetails>, range: DateRange, typeFilter: Long?): List<TimelineItem> {
    val shown = days.mapNotNull { d ->
        val date = LocalDate.ofEpochDay(d.record.epochDay)
        when {
            date.isBefore(range.from) || date.isAfter(range.to) -> null
            typeFilter == null -> d
            d.record.status != DayStatus.EXERCISED -> null
            else -> d.copy(entries = d.entries.filter { it.typeId == typeFilter }).takeIf { it.entries.isNotEmpty() }
        }
    }.sortedByDescending { it.record.epochDay }
    val out = mutableListOf<TimelineItem>()
    var month: YearMonth? = null
    for (d in shown) {
        val m = YearMonth.from(LocalDate.ofEpochDay(d.record.epochDay))
        if (m != month) {
            out += TimelineItem.Month(m)
            month = m
        }
        out += TimelineItem.Day(d)
    }
    return out
}
