package com.actiary.domain

import com.actiary.data.db.DayStatus
import com.actiary.data.db.DayWithDetails
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class HomeStreakSummary(
    val weekExercisedDays: Int,
    val currentStreakDays: Int,
)

fun computeWeekExercisedDays(
    exercisedEpochDays: Set<Long>,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek,
): Int {
    val weekStart = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    var count = 0
    for (epochDay in weekStart.toEpochDay()..today.toEpochDay()) {
        if (epochDay in exercisedEpochDays) count++
    }
    return count
}

fun computeCurrentStreak(
    exercisedEpochDays: Set<Long>,
    today: LocalDate,
): Int {
    var cursor = if (today.toEpochDay() in exercisedEpochDays) today else today.minusDays(1)
    var streak = 0
    while (cursor.toEpochDay() in exercisedEpochDays) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}

fun computeHomeStreakSummary(
    days: List<DayWithDetails>,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek,
): HomeStreakSummary {
    val exercisedEpochDays = days
        .filter { it.record.status == DayStatus.EXERCISED }
        .mapTo(HashSet()) { it.record.epochDay }
    return HomeStreakSummary(
        weekExercisedDays = computeWeekExercisedDays(exercisedEpochDays, today, firstDayOfWeek),
        currentStreakDays = computeCurrentStreak(exercisedEpochDays, today),
    )
}
