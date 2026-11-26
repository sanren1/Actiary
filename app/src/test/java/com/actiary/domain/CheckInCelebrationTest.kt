package com.actiary.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckInCelebrationTest {
    @Test
    fun totalDaysMilestoneMatchesExpectedNumbers() {
        for (days in listOf(1, 10, 30, 60, 90, 120)) {
            assertTrue("Expected $days to be a total-days milestone", isTotalDaysMilestone(days))
        }
        for (days in listOf(0, -1, 2, 5, 15, 29, 31, 45, 50)) {
            assertFalse("Expected $days not to be a total-days milestone", isTotalDaysMilestone(days))
        }
    }

    @Test
    fun streakMilestoneMatchesExpectedNumbers() {
        for (days in listOf(7, 14, 30, 60)) {
            assertTrue("Expected $days to be a streak milestone", isStreakMilestone(days))
        }
        for (days in listOf(0, 1, 6, 8, 13, 15, 29, 31, 45)) {
            assertFalse("Expected $days not to be a streak milestone", isStreakMilestone(days))
        }
    }

    @Test
    fun resolveCheckInKindPrioritizesTotalThenStreakThenDaily() {
        assertEquals(CheckInKind.TotalMilestone(1), resolveCheckInKind(totalExercisedDays = 1, streakDays = 1))
        assertEquals(CheckInKind.TotalMilestone(30), resolveCheckInKind(totalExercisedDays = 30, streakDays = 7))
        assertEquals(CheckInKind.TotalMilestone(60), resolveCheckInKind(totalExercisedDays = 60, streakDays = 60))
        assertEquals(CheckInKind.StreakMilestone(7), resolveCheckInKind(totalExercisedDays = 12, streakDays = 7))
        assertEquals(CheckInKind.StreakMilestone(14), resolveCheckInKind(totalExercisedDays = 20, streakDays = 14))
        assertEquals(CheckInKind.StreakMilestone(30), resolveCheckInKind(totalExercisedDays = 35, streakDays = 30))
        assertEquals(CheckInKind.Daily, resolveCheckInKind(totalExercisedDays = 12, streakDays = 5))
    }
}
