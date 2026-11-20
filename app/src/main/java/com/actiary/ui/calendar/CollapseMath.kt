package com.actiary.ui.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** Grid height at collapse progress [p]: [full] at 0, one [row] at 1. */
fun collapsedHeight(full: Float, row: Float, p: Float): Float = full + (row - full) * p

/** Week row (0-based) of the month grid that holds [selected]; 0 when [selected] is outside [month]. */
fun keptRowIndex(month: YearMonth, selected: LocalDate, firstDayOfWeek: DayOfWeek): Int {
    if (YearMonth.from(selected) != month) return 0
    val lead = (month.atDay(1).dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    return (lead + selected.dayOfMonth - 1) / 7
}

/** Vertical translation that slides row [index] up to the top of the grid as [p] goes 0..1. */
fun rowOffset(index: Int, row: Float, p: Float): Float = -index * row * p

fun snapTarget(p: Float): Float = if (p < 0.5f) 0f else 1f
