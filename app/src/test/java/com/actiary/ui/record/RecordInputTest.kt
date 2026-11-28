package com.actiary.ui.record

import com.actiary.data.db.WorkoutPlanEntity
import com.actiary.data.db.WorkoutPlanItemEntity
import com.actiary.data.db.WorkoutPlanWithItems
import com.actiary.domain.DraftEntry
import com.actiary.domain.DraftStrengthItem
import com.actiary.domain.WorkoutMetric
import com.actiary.domain.parseWeightKg
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecordInputTest {
    @Test fun minutes() {
        assertEquals(45, parseMinutes("45"))
        assertEquals(0, parseMinutes("0"))
        assertNull(parseMinutes(""))
        assertNull(parseMinutes("-5"))
        assertNull(parseMinutes("4.5"))
        assertNull(parseMinutes("abc"))
        assertNull(parseMinutes("99999999999"))
    }

    @Test fun km() {
        assertEquals(5000, parseKmToMeters("5"))
        assertEquals(5200, parseKmToMeters("5.2"))
        assertEquals(5200, parseKmToMeters("5,2"))
        assertEquals(500, parseKmToMeters(".5"))
        assertEquals(5000, parseKmToMeters("5."))
        assertNull(parseKmToMeters(""))
        assertNull(parseKmToMeters("."))
        assertNull(parseKmToMeters("-1"))
        assertEquals(5250, parseKmToMeters("5.25"))
        assertNull(parseKmToMeters("5.251"))
        assertNull(parseKmToMeters("1.2.3"))
        assertNull(parseKmToMeters("abc"))
        assertNull(parseKmToMeters("99999999"))
    }

    @Test fun sanitize() {
        assertEquals("5.2", sanitizeKm("5,2"))
        assertEquals("5.25", sanitizeKm("5.251"))
        assertEquals("5.23", sanitizeKm("5.2.3"))
        assertEquals("12", sanitizeKm("1a2"))
        assertEquals("45", sanitizeMinutes("4x5"))
        assertEquals("100000", sanitizeMinutes("100000"))
        assertEquals("99999", sanitizeMinutes("999999"))
        assertEquals("10000.0", sanitizeKm("10000.0"))
        assertEquals("1000", sanitizeKm("10001"))
        assertEquals("10000", sanitizeKm("10000.5").substringBefore('.'))
        assertEquals("5.", sanitizeKm("5."))
        assertEquals(".", sanitizeKm("."))
    }

    @Test fun format() {
        assertEquals("5", formatKm(5000))
        assertEquals("5.2", formatKm(5200))
        assertEquals("4.23", formatKm(4230))
        assertEquals("0.1", formatKm(100))
        assertEquals("", formatKm(null))
        assertEquals("30", formatMinutes(30))
        assertEquals("", formatMinutes(null))
    }

    @Test fun calories() {
        assertEquals(1, parseCalories("1"))
        assertEquals(9999, parseCalories("9999"))
        assertNull(parseCalories("0"))
        assertNull(parseCalories("10000"))
        assertNull(parseCalories(""))
        assertNull(parseCalories("abc"))
        assertNull(parseCalories("4.5"))
        assertEquals("1234", sanitizeCalories("1a2b3c4d5"))
        assertEquals("12", sanitizeCalories("1a2"))
        assertEquals("0", sanitizeCalories("0"))
    }

    @Test fun heartRate() {
        assertEquals(30, parseHeartRate("30"))
        assertEquals(230, parseHeartRate("230"))
        assertNull(parseHeartRate("29"))
        assertNull(parseHeartRate("231"))
        assertNull(parseHeartRate(""))
        assertNull(parseHeartRate("abc"))
        assertEquals("230", sanitizeHeartRate("2a3b0"))
        assertEquals("29", sanitizeHeartRate("29"))
        assertEquals("3", sanitizeHeartRate("3"))
        assertEquals("23", sanitizeHeartRate("23"))
    }

    @Test fun distanceByType() {
        assertEquals("5000", formatDistance(5000, "swimming", true))
        assertEquals("5", formatDistance(5000, "running", true))
        assertEquals("", formatDistance(5000, "running", false))
        assertEquals("", formatDistance(null, "swimming", true))
        assertEquals(350, parseMeters("350"))
        assertNull(parseMeters("0"))
        assertNull(parseMeters(""))
        assertNull(parseMeters("abc"))
    }

    @Test fun formatCaloriesHeartRateMeters() {
        assertEquals("120", formatCalories(120))
        assertEquals("", formatCalories(null))
        assertEquals("75", formatHeartRate(75))
        assertEquals("", formatHeartRate(null))
        assertEquals("5000", formatMeters(5000))
        assertEquals("", formatMeters(null))
    }

    @Test fun metricCentimetersPreserveFractionalMeters() {
        assertEquals(2505, parseCentimeters("25,05"))
        assertEquals("25.05", formatCentimeters(2505))
        assertEquals(0, parseCentimeters("0"))
        assertNull(parseCentimeters("1.125"))
        assertNull(parseCentimeters("."))
        assertNull(parseCentimeters("-1"))
        assertNull(parseCentimeters("21474836.48"))
    }

    @Test fun workoutMetricLimits() {
        assertEquals(300, parseWorkoutMetric(WorkoutMetric.CADENCE, "300"))
        assertNull(parseWorkoutMetric(WorkoutMetric.CADENCE, "301"))
        assertEquals(200_000, parseWorkoutMetric(WorkoutMetric.STEPS, "200000"))
        assertNull(parseWorkoutMetric(WorkoutMetric.STEPS, "200001"))
        assertNull(parseWorkoutMetric(WorkoutMetric.LAPS, "10001"))
        assertNull(parseWorkoutMetric(WorkoutMetric.STROKES, "100001"))
        assertEquals(2_000_000, parseWorkoutMetric(WorkoutMetric.ELEVATION_GAIN, "20000"))
        assertNull(parseWorkoutMetric(WorkoutMetric.ELEVATION_GAIN, "20000.01"))
        assertEquals(100_000, parseWorkoutMetric(WorkoutMetric.POOL_LENGTH, "1000"))
        assertNull(parseWorkoutMetric(WorkoutMetric.POOL_LENGTH, "1000,01"))
    }

    @Test fun precisePaceUsesExactTimeAndPositiveDistance() {
        assertEquals("1:01:01", formatWorkoutClock(3661))
        assertEquals("5:22", formatWorkoutClock(paceSeconds(1932, 6000, 1000)!!))
        assertEquals("1:40", formatWorkoutClock(paceSeconds(1500, 1500, 100)!!))
        assertNull(paceSeconds(null, 6000, 1000))
        assertNull(paceSeconds(1932, 0, 1000))
    }

    @Test fun strengthInputs() {
        assertEquals(4, parseStrengthSets("4"))
        assertEquals(1, parseStrengthSets("1"))
        assertEquals(99, parseStrengthSets("99"))
        assertNull(parseStrengthSets(""))
        assertNull(parseStrengthSets("0"))
        assertNull(parseStrengthSets("100"))
        assertNull(parseStrengthSets("abc"))

        assertEquals(10, parseStrengthReps("10"))
        assertEquals(1, parseStrengthReps("1"))
        assertEquals(999, parseStrengthReps("999"))
        assertNull(parseStrengthReps(""))
        assertNull(parseStrengthReps("0"))
        assertNull(parseStrengthReps("1000"))
        assertNull(parseStrengthReps("abc"))

        assertNull(parseWeightKg(""))
        assertNull(parseWeightKg("0"))
        assertNull(parseWeightKg("0.4"))
        assertEquals(0.5f, parseWeightKg("0.5"))
        assertEquals(22.5f, parseWeightKg("22.5"))
        assertEquals(500f, parseWeightKg("500"))
        assertNull(parseWeightKg("500.1"))
        assertNull(parseWeightKg("abc"))

        assertEquals("22.5", sanitizeStrengthWeight("22,58"))
        assertEquals("12", sanitizeStrengthSets("1a23"))
        assertEquals("123", sanitizeStrengthReps("1a234"))
    }

    @Test fun stepStrengthSetsAndRepsClampAndRespectTimedMode() {
        assertEquals(5, stepStrengthSets(4, 1))
        assertEquals(3, stepStrengthSets(4, -1))
        assertEquals(1, stepStrengthSets(1, -1))
        assertEquals(99, stepStrengthSets(99, 1))

        assertEquals(11, stepStrengthReps(10, 1, isTimed = false))
        assertEquals(9, stepStrengthReps(10, -1, isTimed = false))
        assertEquals(1, stepStrengthReps(1, -1, isTimed = false))
        assertEquals(999, stepStrengthReps(999, 1, isTimed = false))

        assertEquals(35, stepStrengthReps(30, 1, isTimed = true))
        assertEquals(25, stepStrengthReps(30, -1, isTimed = true))
        assertEquals(5, stepStrengthReps(8, -1, isTimed = true))
        assertEquals(5, stepStrengthReps(5, -1, isTimed = true))
        assertEquals(999, stepStrengthReps(997, 1, isTimed = true))
    }

    @Test fun resolveOverwritePlanTargetHandlesSingleMultiLegacyAndEmpty() {
        val pushPlan = WorkoutPlanWithItems(
            plan = WorkoutPlanEntity(id = 1L, builtinKey = "push", name = null, sortOrder = 0),
            items = listOf(
                WorkoutPlanItemEntity(id = 11L, planId = 1L, exerciseId = 101L, defaultSets = 4, defaultReps = 10, sortOrder = 0),
                WorkoutPlanItemEntity(id = 12L, planId = 1L, exerciseId = 102L, defaultSets = 3, defaultReps = 12, sortOrder = 1),
            ),
        )
        val pullPlan = WorkoutPlanWithItems(
            plan = WorkoutPlanEntity(id = 2L, builtinKey = null, name = "Custom Pull", sortOrder = 1),
            items = listOf(
                WorkoutPlanItemEntity(id = 21L, planId = 2L, exerciseId = 201L, defaultSets = 4, defaultReps = 8, sortOrder = 0),
            ),
        )
        val plans = listOf(pushPlan, pullPlan)

        // 1. Single selected plan with modified reps/sets/weight -> returns that plan.
        val singleModified = DraftEntry(
            typeId = 8L,
            planId = 1L,
            strengthItems = listOf(
                DraftStrengthItem(exerciseId = 101L, sets = 5, reps = 8, weightKg = 60f, localId = 1L, sourcePlanId = 1L),
                DraftStrengthItem(exerciseId = 102L, sets = 4, reps = 10, weightKg = 20f, localId = 2L, sourcePlanId = 1L),
            ),
        )
        assertEquals(pushPlan, resolveOverwritePlanTarget(singleModified, plans))

        // 2. Single selected plan with extra added exercise (sourcePlanId == null) -> still returns that plan.
        val singleWithExtra = singleModified.copy(
            strengthItems = singleModified.strengthItems + DraftStrengthItem(
                exerciseId = 999L, sets = 3, reps = 15, weightKg = null, localId = 3L, sourcePlanId = null,
            ),
        )
        assertEquals(pushPlan, resolveOverwritePlanTarget(singleWithExtra, plans))

        // 3. Multiple selected plans merged -> returns null.
        val mergedPlans = DraftEntry(
            typeId = 8L,
            planId = 2L,
            strengthItems = listOf(
                DraftStrengthItem(exerciseId = 101L, sets = 4, reps = 10, localId = 1L, sourcePlanId = 1L),
                DraftStrengthItem(exerciseId = 201L, sets = 4, reps = 8, localId = 2L, sourcePlanId = 2L),
            ),
        )
        assertNull(resolveOverwritePlanTarget(mergedPlans, plans))

        // 4. No selected plan (sourcePlanId == null, planId == null) -> returns null.
        val freeformOnly = DraftEntry(
            typeId = 8L,
            planId = null,
            strengthItems = listOf(
                DraftStrengthItem(exerciseId = 101L, sets = 4, reps = 10, localId = 1L, sourcePlanId = null),
            ),
        )
        assertNull(resolveOverwritePlanTarget(freeformOnly, plans))

        // 5. Empty strengthItems -> returns null.
        val emptyEntry = DraftEntry(typeId = 8L, planId = 1L, strengthItems = emptyList())
        assertNull(resolveOverwritePlanTarget(emptyEntry, plans))

        // 6. Legacy planId set with sourcePlanId == null items matching the plan -> returns that plan.
        val legacyEntry = DraftEntry(
            typeId = 8L,
            planId = 2L,
            strengthItems = listOf(
                DraftStrengthItem(exerciseId = 201L, sets = 5, reps = 5, weightKg = 80f, localId = 1L, sourcePlanId = null),
            ),
        )
        assertEquals(pullPlan, resolveOverwritePlanTarget(legacyEntry, plans))

        // 7. Deleted/missing plan -> returns null.
        val deletedPlanEntry = DraftEntry(
            typeId = 8L,
            planId = 99L,
            strengthItems = listOf(
                DraftStrengthItem(exerciseId = 101L, sets = 4, reps = 10, localId = 1L, sourcePlanId = 99L),
            ),
        )
        assertNull(resolveOverwritePlanTarget(deletedPlanEntry, plans))
    }
}
