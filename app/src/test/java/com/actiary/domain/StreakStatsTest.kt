package com.actiary.domain

import com.actiary.data.db.DailyRecordEntity
import com.actiary.data.db.DayStatus
import com.actiary.data.db.DayWithDetails
import com.actiary.data.db.ExerciseEntryEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class StreakStatsTest {
    @Test fun emptyInputReturnsZeros() {
        val today = LocalDate.of(2026, 4, 15) // Wednesday
        assertEquals(HomeStreakSummary(0, 0), computeHomeStreakSummary(emptyList(), today, DayOfWeek.MONDAY))
        assertEquals(0, computeWeekExercisedDays(emptySet(), today, DayOfWeek.SUNDAY))
        assertEquals(0, computeCurrentStreak(emptySet(), today))
    }

    @Test fun weekExercisedDaysRespectsMondayVsSundayFirstDay() {
        // 2026-04-15 is Wednesday; Sunday of that week is 2026-04-12, Monday is 2026-04-13.
        val wednesday = LocalDate.of(2026, 4, 15)
        val exercised = setOf(
            LocalDate.of(2026, 4, 11).toEpochDay(), // Saturday (prior week)
            LocalDate.of(2026, 4, 12).toEpochDay(), // Sunday
            LocalDate.of(2026, 4, 13).toEpochDay(), // Monday
            LocalDate.of(2026, 4, 15).toEpochDay(), // Wednesday (today)
            LocalDate.of(2026, 4, 16).toEpochDay(), // Thursday (future, after today)
        )

        assertEquals(2, computeWeekExercisedDays(exercised, wednesday, DayOfWeek.MONDAY))
        assertEquals(3, computeWeekExercisedDays(exercised, wednesday, DayOfWeek.SUNDAY))
    }

    @Test fun streakCountsTodayAndConsecutivePreviousDays() {
        val today = LocalDate.of(2026, 4, 15)
        val exercised = setOf(
            today.toEpochDay(),
            today.minusDays(1).toEpochDay(),
            today.minusDays(2).toEpochDay(),
            today.minusDays(4).toEpochDay(), // gap at -3
        )

        assertEquals(3, computeCurrentStreak(exercised, today))
    }

    @Test fun streakPreservedFromYesterdayWhenTodayIsUnrecorded() {
        val today = LocalDate.of(2026, 4, 15)
        val exercised = setOf(
            today.minusDays(1).toEpochDay(),
            today.minusDays(2).toEpochDay(),
            today.minusDays(3).toEpochDay(),
        )

        assertEquals(3, computeCurrentStreak(exercised, today))
    }

    @Test fun restDaysPreserveStreakWithoutIncrementingAndGapBreaks() {
        val today = LocalDate.of(2026, 4, 15)

        // EX, EX, REST, EX(today) -> streak == 3 (rest bridges without incrementing)
        val restBridgesToToday = listOf(
            exercisedDay(today),
            restDay(today.minusDays(1)),
            exercisedDay(today.minusDays(2)),
            exercisedDay(today.minusDays(3)),
        )
        assertEquals(
            HomeStreakSummary(weekExercisedDays = 2, currentStreakDays = 3),
            computeHomeStreakSummary(restBridgesToToday, today, DayOfWeek.MONDAY),
        )

        // EX, EX, REST(today) -> streak == 2
        val restTodayPreservesPrior = listOf(
            restDay(today),
            exercisedDay(today.minusDays(1)),
            exercisedDay(today.minusDays(2)),
        )
        assertEquals(
            HomeStreakSummary(weekExercisedDays = 2, currentStreakDays = 2),
            computeHomeStreakSummary(restTodayPreservesPrior, today, DayOfWeek.MONDAY),
        )

        // EX, EX, REST(yesterday), unrecorded(today) -> streak == 2
        val restYesterdayUnrecordedToday = listOf(
            restDay(today.minusDays(1)),
            exercisedDay(today.minusDays(2)),
            exercisedDay(today.minusDays(3)),
        )
        assertEquals(
            HomeStreakSummary(weekExercisedDays = 1, currentStreakDays = 2),
            computeHomeStreakSummary(restYesterdayUnrecordedToday, today, DayOfWeek.MONDAY),
        )

        // REST, REST(today) -> streak == 0
        val onlyRestDays = listOf(
            restDay(today),
            restDay(today.minusDays(1)),
        )
        assertEquals(
            HomeStreakSummary(weekExercisedDays = 0, currentStreakDays = 0),
            computeHomeStreakSummary(onlyRestDays, today, DayOfWeek.MONDAY),
        )

        // EX, unrecorded, REST(today) -> streak == 0 (unrecorded breaks streak)
        val unrecordedBreaksBeforeRestToday = listOf(
            restDay(today),
            exercisedDay(today.minusDays(2)),
        )
        assertEquals(
            HomeStreakSummary(weekExercisedDays = 1, currentStreakDays = 0),
            computeHomeStreakSummary(unrecordedBreaksBeforeRestToday, today, DayOfWeek.MONDAY),
        )

        val daysWithGap = listOf(
            exercisedDay(today.minusDays(2)),
            exercisedDay(today.minusDays(3)),
        )
        assertEquals(
            HomeStreakSummary(weekExercisedDays = 1, currentStreakDays = 0),
            computeHomeStreakSummary(daysWithGap, today, DayOfWeek.MONDAY),
        )
    }

    private fun exercisedDay(date: LocalDate): DayWithDetails = DayWithDetails(
        record = DailyRecordEntity(date.toEpochDay(), DayStatus.EXERCISED, "", 1L),
        entries = listOf(ExerciseEntryEntity(1L, date.toEpochDay(), 1L, 30, null, null, null, 0)),
        photos = emptyList(),
    )

    private fun restDay(date: LocalDate): DayWithDetails = DayWithDetails(
        record = DailyRecordEntity(date.toEpochDay(), DayStatus.REST, "", 1L),
        entries = emptyList(),
        photos = emptyList(),
    )
}
