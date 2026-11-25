package com.actiary.ui.common

import com.actiary.data.db.StrengthExerciseEntity
import com.actiary.domain.BodyRegion
import com.actiary.domain.MuscleGroup
import com.actiary.domain.muscleMaskOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnatomyBodyMapTest {

    @Test
    fun all14MuscleGroupsHavePolygonsOnExpectedSides() {
        val frontMuscles = ANATOMY_POLYGONS
            .filter { it.side == BodySide.FRONT }
            .map { it.muscle }
            .toSet()
        val backMuscles = ANATOMY_POLYGONS
            .filter { it.side == BodySide.BACK }
            .map { it.muscle }
            .toSet()

        for (muscle in MuscleGroup.entries) {
            assertEquals(
                "FRONT polygon presence mismatch for $muscle",
                muscle.onFront,
                muscle in frontMuscles,
            )
            assertEquals(
                "BACK polygon presence mismatch for $muscle",
                muscle.onBack,
                muscle in backMuscles,
            )
        }

        for (polygon in ANATOMY_POLYGONS) {
            assertTrue(
                "Polygon for ${polygon.muscle} (${polygon.side}) must have at least 3 vertices",
                polygon.points.size >= 3,
            )
            for (point in polygon.points) {
                assertTrue(
                    "x out of bounds [0,1] for ${polygon.muscle} (${polygon.side}): ${point.x}",
                    point.x in 0f..1f,
                )
                assertTrue(
                    "y out of bounds [0,1] for ${polygon.muscle} (${polygon.side}): ${point.y}",
                    point.y in 0f..1f,
                )
            }
        }
    }

    @Test
    fun hitTestFrontAndBackRepresentativeMuscles() {
        // Front representative muscles (both left & mirrored right where applicable)
        assertEquals(MuscleGroup.PECTORALS, hitTestMuscle(BodySide.FRONT, 0.40f, 0.28f))
        assertEquals(MuscleGroup.PECTORALS, hitTestMuscle(BodySide.FRONT, 0.60f, 0.28f))
        assertEquals(MuscleGroup.FRONT_SIDE_DELTS, hitTestMuscle(BodySide.FRONT, 0.26f, 0.255f))
        assertEquals(MuscleGroup.BICEPS, hitTestMuscle(BodySide.FRONT, 0.23f, 0.35f))
        assertEquals(MuscleGroup.FOREARMS, hitTestMuscle(BodySide.FRONT, 0.17f, 0.45f))
        assertEquals(MuscleGroup.ABS, hitTestMuscle(BodySide.FRONT, 0.50f, 0.42f))
        assertEquals(MuscleGroup.QUADS, hitTestMuscle(BodySide.FRONT, 0.385f, 0.63f))
        assertEquals(MuscleGroup.CALVES, hitTestMuscle(BodySide.FRONT, 0.375f, 0.83f))

        // Back representative muscles
        assertEquals(MuscleGroup.UPPER_BACK, hitTestMuscle(BodySide.BACK, 0.50f, 0.255f))
        assertEquals(MuscleGroup.REAR_DELTS, hitTestMuscle(BodySide.BACK, 0.27f, 0.255f))
        assertEquals(MuscleGroup.LATS, hitTestMuscle(BodySide.BACK, 0.39f, 0.36f))
        assertEquals(MuscleGroup.LATS, hitTestMuscle(BodySide.BACK, 0.61f, 0.36f))
        assertEquals(MuscleGroup.LOWER_BACK, hitTestMuscle(BodySide.BACK, 0.50f, 0.455f))
        assertEquals(MuscleGroup.TRICEPS, hitTestMuscle(BodySide.BACK, 0.23f, 0.35f))
        assertEquals(MuscleGroup.FOREARMS, hitTestMuscle(BodySide.BACK, 0.17f, 0.45f))
        assertEquals(MuscleGroup.GLUTES, hitTestMuscle(BodySide.BACK, 0.40f, 0.545f))
        assertEquals(MuscleGroup.HAMSTRINGS, hitTestMuscle(BodySide.BACK, 0.385f, 0.665f))
        assertEquals(MuscleGroup.CALVES, hitTestMuscle(BodySide.BACK, 0.375f, 0.825f))

        // Empty corners and out-of-bounds return null
        assertNull(hitTestMuscle(BodySide.FRONT, 0.02f, 0.02f))
        assertNull(hitTestMuscle(BodySide.FRONT, 0.98f, 0.98f))
        assertNull(hitTestMuscle(BodySide.BACK, 0.02f, 0.02f))
        assertNull(hitTestMuscle(BodySide.BACK, 0.98f, 0.98f))
        assertNull(hitTestMuscle(BodySide.FRONT, -0.1f, 0.5f))
        assertNull(hitTestMuscle(BodySide.BACK, 1.1f, 0.5f))
    }

    @Test
    fun heatmapLevelBuckets() {
        assertEquals(0, heatmapLevel(sets = 0, maxSets = 10))
        assertEquals(0, heatmapLevel(sets = 10, maxSets = 0))
        assertEquals(0, heatmapLevel(sets = -2, maxSets = 10))
        assertEquals(1, heatmapLevel(sets = 3, maxSets = 10))
        assertEquals(2, heatmapLevel(sets = 4, maxSets = 10))
        assertEquals(2, heatmapLevel(sets = 6, maxSets = 10))
        assertEquals(3, heatmapLevel(sets = 7, maxSets = 10))
        assertEquals(3, heatmapLevel(sets = 10, maxSets = 10))
    }

    @Test
    fun summarizeStrengthMusclesOrdersByFrequencyAndCaps() {
        val masks = listOf(
            muscleMaskOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
            muscleMaskOf(MuscleGroup.PECTORALS),
            muscleMaskOf(MuscleGroup.PECTORALS, MuscleGroup.TRICEPS),
            muscleMaskOf(MuscleGroup.PECTORALS),
            muscleMaskOf(MuscleGroup.GLUTES),
            muscleMaskOf(MuscleGroup.ABS),
        )
        // Counts: PECTORALS=3, GLUTES=2, TRICEPS=1 (ord 7), ABS=1 (ord 9), QUADS=1 (ord 10)
        assertEquals(
            listOf(MuscleGroup.PECTORALS, MuscleGroup.GLUTES, MuscleGroup.TRICEPS),
            summarizeStrengthMuscles(masks, maxMuscles = 3),
        )
        assertEquals(
            listOf(MuscleGroup.PECTORALS, MuscleGroup.GLUTES),
            summarizeStrengthMuscles(masks, maxMuscles = 2),
        )
        assertEquals(emptyList<MuscleGroup>(), summarizeStrengthMuscles(emptyList()))
    }

    @Test
    fun strengthUiFormattingAndDisplayNameHelpers() {
        val exercise = StrengthExerciseEntity(
            id = 1L,
            builtinKey = "barbell_bench_press",
            nameZh = "杠铃平板卧推",
            nameEn = "Barbell Bench Press",
            bodyRegion = BodyRegion.CHEST.key,
            primaryMuscleMask = muscleMaskOf(MuscleGroup.PECTORALS),
            secondaryMuscleMask = 0,
            defaultSets = 4,
            defaultReps = 10,
        )
        assertEquals("杠铃平板卧推", exerciseDisplayName(exercise, isChinese = true))
        assertEquals("Barbell Bench Press", exerciseDisplayName(exercise, isChinese = false))

        val noWeight = formatStrengthItemMetrics(
            sets = 3,
            reps = 15,
            weightKg = null,
            noWeightFmt = { s, r -> "$s × $r" },
            weightFmt = { s, r, w -> "$s × $r · $w kg" },
        )
        assertEquals("3 × 15", noWeight)

        val withWeight = formatStrengthItemMetrics(
            sets = 4,
            reps = 10,
            weightKg = 62.5f,
            noWeightFmt = { s, r -> "$s × $r" },
            weightFmt = { s, r, w -> "$s × $r · $w kg" },
        )
        assertEquals("4 × 10 · 62.5 kg", withWeight)
    }
}
