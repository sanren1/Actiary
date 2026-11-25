package com.actiary.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StrengthModelTest {

    @Test
    fun builtinCatalogCoversAll14MusclesAndUniqueKeys() {
        val exercises = BuiltinStrengthCatalog.exercises
        assertTrue("Expected at least 118 built-in exercises, got ${exercises.size}", exercises.size >= 118)

        val expectedBodyweightKeys = listOf(
            "wide_push_up", "incline_push_up", "decline_push_up", "knee_push_up", "archer_push_up", "clap_push_up",
            "towel_doorway_row", "prone_towel_lat_pulldown", "prone_ytw_raise", "inverted_row", "superman_extension", "scapular_pull_up",
            "pike_push_up", "wall_handstand_push_up", "wall_angel", "prone_reverse_fly",
            "diamond_push_up", "towel_self_resistance_curl", "bodyweight_triceps_extension", "dead_hang",
            "bodyweight_squat", "pause_bodyweight_squat", "alternating_reverse_lunge", "single_leg_glute_bridge",
            "single_leg_calf_raise", "glute_bridge", "jump_squat", "wall_sit", "side_lunge",
            "dead_bug", "bird_dog", "side_plank", "mountain_climber", "bicycle_crunch",
            "lying_leg_raise", "v_up", "hollow_body_hold", "burpee",
        )
        val keys = exercises.map { it.builtinKey }
        assertEquals("Builtin exercise keys must be unique", keys.size, keys.toSet().size)
        val keySet = keys.toSet()
        for (expectedKey in expectedBodyweightKeys) {
            assertTrue("Missing expected bodyweight exercise key: $expectedKey", expectedKey in keySet)
        }

        val coveredPrimaryMuscles = mutableSetOf<MuscleGroup>()
        val coveredRegions = mutableSetOf<BodyRegion>()

        for (exercise in exercises) {
            assertTrue("builtinKey must not be blank", exercise.builtinKey.isNotBlank())
            assertTrue("nameZh must not be blank for ${exercise.builtinKey}", exercise.nameZh.isNotBlank())
            assertTrue("nameEn must not be blank for ${exercise.builtinKey}", exercise.nameEn.isNotBlank())
            assertTrue("primaryMuscleMask must be non-zero for ${exercise.builtinKey}", exercise.primaryMuscleMask != 0)
            assertEquals(
                "primary and secondary masks must not overlap for ${exercise.builtinKey}",
                0,
                exercise.primaryMuscleMask and exercise.secondaryMuscleMask,
            )
            assertEquals(
                "primaryMuscleMask out of bounds for ${exercise.builtinKey}",
                exercise.primaryMuscleMask,
                exercise.primaryMuscleMask and ALL_MUSCLE_MASK,
            )
            assertEquals(
                "secondaryMuscleMask out of bounds for ${exercise.builtinKey}",
                exercise.secondaryMuscleMask,
                exercise.secondaryMuscleMask and ALL_MUSCLE_MASK,
            )
            assertTrue("defaultSets must be positive for ${exercise.builtinKey}", exercise.defaultSets > 0)
            assertTrue("defaultReps must be positive for ${exercise.builtinKey}", exercise.defaultReps > 0)

            val primaryMuscles = musclesFromMask(exercise.primaryMuscleMask)
            val secondaryMuscles = musclesFromMask(exercise.secondaryMuscleMask)
            assertTrue(
                "primary muscle count must be 1..2 for ${exercise.builtinKey}, got ${primaryMuscles.size}",
                primaryMuscles.size in 1..2,
            )
            assertTrue(
                "secondary muscle count must be 0..2 for ${exercise.builtinKey}, got ${secondaryMuscles.size}",
                secondaryMuscles.size in 0..2,
            )

            coveredPrimaryMuscles += primaryMuscles
            coveredRegions += exercise.region
        }

        assertEquals(MuscleGroup.entries.toSet(), coveredPrimaryMuscles)
        assertEquals(BodyRegion.entries.toSet(), coveredRegions)

        val expectedTimedKeys = setOf("plank", "side_plank", "wall_sit", "dead_hang", "hollow_body_hold")
        assertEquals(expectedTimedKeys, exercises.filter { it.isTimed }.map { it.builtinKey }.toSet())

        val plans = BuiltinStrengthCatalog.plans
        assertEquals(listOf("push", "pull", "legs"), plans.map { it.builtinKey })
        val exerciseByKey = exercises.associateBy { it.builtinKey }
        for (plan in plans) {
            assertEquals("Plan ${plan.builtinKey} should have 5 exercises", 5, plan.items.size)
            for (item in plan.items) {
                assertTrue(
                    "Plan ${plan.builtinKey} references unknown exerciseKey '${item.exerciseKey}'",
                    item.exerciseKey in exerciseByKey,
                )
                assertTrue(item.defaultSets > 0)
                assertTrue(item.defaultReps > 0)
            }
        }
    }

    @Test
    fun highlightPrimaryOverridesSecondary() {
        assertEquals(emptyMap<MuscleGroup, MuscleActivation>(), computeDayMuscleHighlights(emptyList()))

        val benchPress = muscleMaskOf(MuscleGroup.PECTORALS) to muscleMaskOf(MuscleGroup.FRONT_SIDE_DELTS, MuscleGroup.TRICEPS)
        val ohp = muscleMaskOf(MuscleGroup.FRONT_SIDE_DELTS) to muscleMaskOf(MuscleGroup.TRICEPS)

        val highlights = computeDayMuscleHighlights(listOf(benchPress, ohp))

        assertEquals(
            mapOf(
                MuscleGroup.PECTORALS to MuscleActivation.PRIMARY,
                MuscleGroup.FRONT_SIDE_DELTS to MuscleActivation.PRIMARY,
                MuscleGroup.TRICEPS to MuscleActivation.SECONDARY,
            ),
            highlights,
        )
        assertFalse(MuscleGroup.BICEPS in highlights)
    }

    @Test
    fun recommendNextPlanWrapsAroundAndHandlesDeleted() {
        val ordered = listOf(1L, 2L, 3L)
        assertEquals(2L, recommendNextPlanId(ordered, 1L))
        assertEquals(3L, recommendNextPlanId(ordered, 2L))
        assertEquals(1L, recommendNextPlanId(ordered, 3L))
        assertEquals(1L, recommendNextPlanId(ordered, 99L))
        assertEquals(1L, recommendNextPlanId(ordered, null))
        assertNull(recommendNextPlanId(emptyList(), 1L))
        assertNull(recommendNextPlanId(emptyList(), null))
        assertEquals(42L, recommendNextPlanId(listOf(42L), 42L))
    }

    @Test
    fun parseAndFormatWeightKgHandlesEdgeCases() {
        val invalidInputs = listOf("", "   ", "0", "-5", "abc", "500.1", "0.4", "NaN", "Infinity")
        for (raw in invalidInputs) {
            assertNull("Expected null for '$raw'", parseWeightKg(raw))
        }

        assertEquals(0.5f, parseWeightKg("0.5"))
        assertEquals(500f, parseWeightKg("500"))
        assertEquals(60f, parseWeightKg("60"))
        assertEquals(60f, parseWeightKg(" 60.0 "))
        assertEquals(22.5f, parseWeightKg("22.5"))
        assertEquals(22.6f, parseWeightKg("22.56"))

        assertEquals("60", formatWeightKg(60f))
        assertEquals("22.5", formatWeightKg(22.5f))
        assertEquals("22.6", formatWeightKg(22.6f))
        assertEquals("0.5", formatWeightKg(0.5f))
    }

    @Test
    fun muscleMaskRoundTripAndMetadata() {
        assertEquals(14, MuscleGroup.entries.size)
        assertEquals((1 shl MuscleGroup.entries.size) - 1, ALL_MUSCLE_MASK)
        assertEquals(
            setOf(MuscleGroup.PECTORALS, MuscleGroup.FOREARMS, MuscleGroup.CALVES),
            musclesFromMask(muscleMaskOf(MuscleGroup.PECTORALS, MuscleGroup.FOREARMS, MuscleGroup.CALVES)),
        )
        assertEquals(emptySet<MuscleGroup>(), musclesFromMask(0))
        assertEquals(MuscleGroup.entries.toSet(), musclesFromMask(ALL_MUSCLE_MASK))
    }
}
