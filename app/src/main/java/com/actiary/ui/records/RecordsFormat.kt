package com.actiary.ui.records

import com.actiary.data.db.DayStatus
import com.actiary.data.db.DayWithDetails
import com.actiary.domain.DateRange
import java.time.LocalDate
import java.time.YearMonth

sealed interface TimelineItem {
    data class Month(val month: YearMonth) : TimelineItem
    data class Day(val day: DayWithDetails) : TimelineItem
}

fun splitMinutes(total: Int): Pair<Int, Int> = total / 60 to total % 60

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
