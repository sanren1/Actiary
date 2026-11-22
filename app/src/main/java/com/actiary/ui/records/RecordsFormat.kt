package com.actiary.ui.records

import com.actiary.R
import com.actiary.data.db.DayStatus
import com.actiary.data.db.DayWithDetails
import com.actiary.data.db.ExerciseEntryEntity
import com.actiary.data.db.ExerciseTypeEntity
import com.actiary.domain.DateRange
import com.actiary.domain.RangeKind
import com.actiary.domain.RangeStats
import com.actiary.domain.WorkoutMetric
import com.actiary.domain.computeStats
import com.actiary.ui.common.labelId
import com.actiary.ui.record.formatDistance
import com.actiary.ui.record.formatWorkoutClock
import com.actiary.ui.record.paceSeconds
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

internal fun chartAxisTicks(kind: RangeKind, count: Int): List<Int> {
    if (count <= 0) return emptyList()
    if (count == 1) return listOf(0)
    val last = count - 1
    val raw = when (kind) {
        RangeKind.WEEK -> listOf(0, 2, 4, last)
        RangeKind.MONTH -> listOf(0, 9, 19, last)
        RangeKind.YEAR -> listOf(0, 2, 5, 8, last)
    }
    return raw.filter { it in 0..last }.distinct().sorted()
}

internal data class ChartBarSpec(
    val centerNorm: Float,
    val widthNorm: Float,
    val fraction: Float,
    val isMax: Boolean,
    val alpha: Float = 1f,
)

internal fun buildBarSpecs(values: List<Int?>, maxIndex: Int?, rtl: Boolean): List<ChartBarSpec> {
    if (values.isEmpty()) return emptyList()
    val max = values.filterNotNull().maxOrNull()
    val count = values.size
    val slotNorm = 1f / count
    val widthNorm = slotNorm * 0.6f
    return values.indices.map { i ->
        val visualIndex = if (rtl) count - 1 - i else i
        val v = values[i]
        ChartBarSpec(
            centerNorm = (visualIndex + 0.5f) * slotNorm,
            widthNorm = widthNorm,
            fraction = if (v == null || max == null || max <= 0) 0f else v.toFloat() / max,
            isMax = i == maxIndex,
        )
    }
}

/** Picks [small] strictly increasing indices in `0 until large` distributed evenly by position. */
internal fun matchedBarIndices(small: Int, large: Int): List<Int> {
    if (small <= 0 || large <= 0) return emptyList()
    require(small <= large)
    return List(small) { i -> ((i + 0.5f) * large / small).toInt().coerceIn(0, large - 1) }
}

internal fun interpolateBars(from: List<ChartBarSpec>, to: List<ChartBarSpec>, progress: Float): List<ChartBarSpec> {
    if (to.isEmpty()) return emptyList()
    if (from.isEmpty() || progress >= 1f) return to
    val p = progress.coerceIn(0f, 1f)
    fun lerp(a: Float, b: Float) = a + (b - a) * p
    return when {
        from.size == to.size -> from.indices.map { i ->
            val a = from[i]
            val b = to[i]
            ChartBarSpec(
                centerNorm = lerp(a.centerNorm, b.centerNorm),
                widthNorm = lerp(a.widthNorm, b.widthNorm),
                fraction = lerp(a.fraction, b.fraction),
                isMax = b.isMax,
                alpha = lerp(a.alpha, b.alpha),
            )
        }
        from.size < to.size -> {
            val targetToSource = matchedBarIndices(from.size, to.size)
                .withIndex().associate { (srcIdx, dstIdx) -> dstIdx to srcIdx }
            to.indices.map { j ->
                val b = to[j]
                val srcIdx = targetToSource[j]
                if (srcIdx != null) {
                    val a = from[srcIdx]
                    ChartBarSpec(
                        centerNorm = lerp(a.centerNorm, b.centerNorm),
                        widthNorm = lerp(a.widthNorm, b.widthNorm),
                        fraction = lerp(a.fraction, b.fraction),
                        isMax = b.isMax,
                        alpha = 1f,
                    )
                } else {
                    b.copy(fraction = b.fraction * p, alpha = p)
                }
            }
        }
        else -> {
            val sourceToTarget = matchedBarIndices(to.size, from.size)
                .withIndex().associate { (dstIdx, srcIdx) -> srcIdx to dstIdx }
            from.indices.map { i ->
                val a = from[i]
                val dstIdx = sourceToTarget[i]
                if (dstIdx != null) {
                    val b = to[dstIdx]
                    ChartBarSpec(
                        centerNorm = lerp(a.centerNorm, b.centerNorm),
                        widthNorm = lerp(a.widthNorm, b.widthNorm),
                        fraction = lerp(a.fraction, b.fraction),
                        isMax = b.isMax,
                        alpha = 1f,
                    )
                } else {
                    a.copy(fraction = a.fraction * (1f - p), alpha = a.alpha * (1f - p))
                }
            }
        }
    }
}

internal fun entryMetrics(
    entry: ExerciseEntryEntity,
    type: ExerciseTypeEntity,
    includeOptional: Boolean = false,
    format: (Int, Any) -> String,
): List<Pair<Int, String>> = buildList {
    val seconds = entry.metrics.durationSeconds
    if (seconds != null) {
        add(R.string.records_stat_duration to formatWorkoutClock(seconds.toLong()))
    } else {
        entry.durationMinutes?.let { add(R.string.records_stat_duration to format(R.string.records_min, it)) }
    }
    entry.calories?.let { add(R.string.records_stat_calories to format(R.string.records_kcal, it)) }
    entry.avgHeartRate?.let { add(R.string.record_heart_rate to format(R.string.record_detail_bpm, it)) }
    val distance = formatDistance(entry.distanceMeters, type.builtinKey, includeOptional || type.hasDistance)
    if (distance.isNotEmpty()) {
        val unit = if (type.builtinKey == "swimming") R.string.records_m else R.string.records_km
        add(R.string.record_detail_distance to format(unit, distance))
    }
    if (includeOptional) {
        WorkoutMetric.entries.forEach { metric ->
            val value = metric.value(entry.metrics) ?: return@forEach
            val display = when (metric) {
                WorkoutMetric.CADENCE -> format(R.string.workout_metric_steps_per_min, value)
                WorkoutMetric.ELEVATION_GAIN, WorkoutMetric.POOL_LENGTH ->
                    format(R.string.workout_metric_decimal_meters, value / 100.0)
                else -> value.toString()
            }
            add(metric.labelId to display)
        }
        if (type.builtinKey in setOf("running", "swimming")) {
            val swimming = type.builtinKey == "swimming"
            val pace = paceSeconds(seconds, entry.distanceMeters, if (swimming) 100 else 1000)
            if (pace != null) {
                val unit = if (swimming) R.string.workout_metric_pace_100m else R.string.workout_metric_pace_km
                add(R.string.workout_metric_pace to format(unit, formatWorkoutClock(pace)))
            }
        } else if (seconds != null && seconds > 0 && entry.distanceMeters != null && entry.distanceMeters > 0) {
            add(R.string.workout_metric_speed to format(R.string.workout_metric_speed_kph, entry.distanceMeters * 3.6 / seconds))
        }
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
