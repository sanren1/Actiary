package com.actiary.domain

fun isTotalDaysMilestone(totalExercisedDays: Int): Boolean =
    totalExercisedDays == 1 || totalExercisedDays == 10 || (totalExercisedDays >= 30 && totalExercisedDays % 30 == 0)

fun isStreakMilestone(streakDays: Int): Boolean =
    streakDays == 7 || streakDays == 14 || (streakDays >= 30 && streakDays % 30 == 0)

sealed interface CheckInKind {
    data object Daily : CheckInKind
    data class TotalMilestone(val days: Int) : CheckInKind
    data class StreakMilestone(val days: Int) : CheckInKind
}

fun resolveCheckInKind(totalExercisedDays: Int, streakDays: Int): CheckInKind = when {
    isTotalDaysMilestone(totalExercisedDays) -> CheckInKind.TotalMilestone(totalExercisedDays)
    isStreakMilestone(streakDays) -> CheckInKind.StreakMilestone(streakDays)
    else -> CheckInKind.Daily
}

data class CheckInCelebrationData(
    val kind: CheckInKind,
    val streakDays: Int,
    val weekDays: Int,
    val totalDays: Int,
    val quoteIndex: Int,
    val workoutSummary: String?,
)
