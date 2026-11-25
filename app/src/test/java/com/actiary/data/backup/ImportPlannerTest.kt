package com.actiary.data.backup

import com.actiary.data.db.DayStatus
import com.actiary.data.db.ExerciseTypeEntity
import com.actiary.data.db.StrengthExerciseEntity
import com.actiary.data.db.WorkoutPlanEntity
import com.actiary.domain.MuscleGroup
import com.actiary.domain.muscleMaskOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportPlannerTest {
    private val backup = BackupData(
        1, "0.13.0", 0, emptyMap(),
        listOf(
            BackupType(10, null, " 瑜伽 ", 0, false, false, null),
            BackupType(11, "running", null, 1, false, true, null),
            BackupType(12, "yoga", null, 2, true, false, null),
        ),
        listOf(BackupRecord(20, DayStatus.REST, "old", 0), BackupRecord(21, DayStatus.REST, "new", 0)),
        emptyList(), emptyList(),
    )
    private val local = LocalSnapshot(
        listOf(
            ExerciseTypeEntity(1, null, "瑜伽", 0),
            ExerciseTypeEntity(2, "running", "本机名称", 1),
            ExerciseTypeEntity(3, null, "yoga", 2),
        ),
        setOf(20L),
    )

    @Test fun mergeKeepsExistingDayAndAddsMissingDay() {
        val plan = planMerge(local, backup)
        assertEquals(listOf(21L), plan.daysToWrite)
        assertEquals(1, plan.skippedDays)
    }

    @Test fun customTypeMatchedByTrimmedName() {
        assertEquals(TypeTarget.Existing(1), planMerge(local, backup).typeMap[10L])
    }

    @Test fun customTypeNameCaseSensitive() {
        val distinct = backup.types.first().copy(name = "Yoga")
        assertEquals(TypeTarget.Create(distinct), planMerge(local, backup.copy(types = listOf(distinct))).typeMap[10L])
    }

    @Test fun builtinMatchedByKeyAndMissingArchivedTypeIsCreated() {
        val plan = planMerge(local, backup)
        assertEquals(TypeTarget.Existing(2), plan.typeMap[11L])
        assertEquals(TypeTarget.Create(backup.types.last()), plan.typeMap[12L])
    }

    @Test fun overwriteWritesAllDaysAndCreatesCustomType() {
        val plan = planOverwrite(local, backup)
        assertEquals(listOf(20L, 21L), plan.daysToWrite)
        assertEquals(0, plan.skippedDays)
        assertEquals(TypeTarget.Create(backup.types.first()), plan.typeMap[10L])
        assertEquals(TypeTarget.Existing(2), plan.typeMap[11L])
        assertTrue(plan.reseedStrengthDefaults)
    }

    @Test fun mergeTwiceWithSameV3BackupProducesZeroDuplicatesAndLocalWins() {
        val localExercises = listOf(
            StrengthExerciseEntity(
                id = 1, builtinKey = "barbell_bench_press", nameZh = "杠铃平板卧推", nameEn = "Barbell Bench Press",
                bodyRegion = "chest", primaryMuscleMask = muscleMaskOf(MuscleGroup.PECTORALS),
                secondaryMuscleMask = muscleMaskOf(MuscleGroup.TRICEPS), defaultSets = 4, defaultReps = 10, archived = true,
            ),
            StrengthExerciseEntity(
                id = 2, builtinKey = null, nameZh = "Cable Fly", nameEn = "Cable Fly",
                bodyRegion = "chest", primaryMuscleMask = muscleMaskOf(MuscleGroup.PECTORALS),
                secondaryMuscleMask = 0, defaultSets = 3, defaultReps = 12, archived = false,
            ),
        )
        val localPlans = listOf(
            WorkoutPlanEntity(id = 10, builtinKey = "push", name = "我的推日", sortOrder = 0),
            WorkoutPlanEntity(id = 11, builtinKey = "pull", name = null, sortOrder = 1),
            WorkoutPlanEntity(id = 12, builtinKey = "legs", name = null, sortOrder = 2),
            WorkoutPlanEntity(id = 13, builtinKey = null, name = "Upper Body", sortOrder = 3),
        )
        val localSnapshot = LocalSnapshot(
            types = listOf(ExerciseTypeEntity(5, "freeform", null, 0)),
            dayEpochs = setOf(20L),
            strengthExercises = localExercises,
            workoutPlans = localPlans,
        )

        val incomingBench = BackupStrengthExercise(
            id = 501, builtinKey = "barbell_bench_press", nameZh = "杠铃平板卧推", nameEn = "Barbell Bench Press",
            bodyRegion = "chest", primaryMuscleMask = muscleMaskOf(MuscleGroup.PECTORALS),
            secondaryMuscleMask = muscleMaskOf(MuscleGroup.TRICEPS), defaultSets = 4, defaultReps = 10, archived = false,
        )
        val incomingCollidingCustom = BackupStrengthExercise(
            id = 502, builtinKey = null, nameZh = "  cable fly  ", nameEn = "Cable Fly",
            bodyRegion = "chest", primaryMuscleMask = muscleMaskOf(MuscleGroup.PECTORALS),
            secondaryMuscleMask = 0, defaultSets = 5, defaultReps = 8, archived = false,
        )
        val incomingNewCustom = BackupStrengthExercise(
            id = 503, builtinKey = null, nameZh = "地雷管推举", nameEn = "Landmine Press",
            bodyRegion = "shoulders", primaryMuscleMask = muscleMaskOf(MuscleGroup.FRONT_SIDE_DELTS),
            secondaryMuscleMask = 0, defaultSets = 3, defaultReps = 10, archived = false,
        )

        val incomingPush = BackupWorkoutPlan(
            id = 601, builtinKey = "push", name = null, sortOrder = 0,
            items = listOf(BackupWorkoutPlanItem(501, 4, 10, 80f, 0)),
        )
        val incomingPull = BackupWorkoutPlan(
            id = 602, builtinKey = "pull", name = null, sortOrder = 1, items = emptyList(),
        )
        val incomingLegs = BackupWorkoutPlan(
            id = 603, builtinKey = "legs", name = null, sortOrder = 2, items = emptyList(),
        )
        val incomingCollidingPlan = BackupWorkoutPlan(
            id = 604, builtinKey = null, name = " upper body ", sortOrder = 3,
            items = listOf(BackupWorkoutPlanItem(502, 3, 12, null, 0)),
        )
        val incomingNewPlan = BackupWorkoutPlan(
            id = 605, builtinKey = null, name = "核心强化", sortOrder = 4,
            items = listOf(BackupWorkoutPlanItem(503, 4, 12, 20f, 0)),
        )

        val v3Backup = BackupData(
            format = BACKUP_FORMAT,
            appVersion = "0.14.0",
            exportedAt = 1_700_000_000_000L,
            appearance = emptyMap(),
            types = listOf(BackupType(40, "freeform", null, 0, false, false, null)),
            records = listOf(
                BackupRecord(20, DayStatus.EXERCISED, "skip", 0),
                BackupRecord(21, DayStatus.EXERCISED, "import", 0),
            ),
            entries = listOf(
                BackupEntry(701, 21, 40, 50, null, null, null, 0, planId = 601),
                BackupEntry(702, 21, 40, 20, null, null, null, 1, planId = 605),
            ),
            photos = emptyList(),
            strengthExercises = listOf(incomingBench, incomingCollidingCustom, incomingNewCustom),
            workoutPlans = listOf(incomingPush, incomingPull, incomingLegs, incomingCollidingPlan, incomingNewPlan),
            strengthLogs = listOf(
                BackupStrengthLog(701, 501, 4, 10, 60f, 0),
                BackupStrengthLog(701, 502, 3, 12, null, 1),
                BackupStrengthLog(702, 503, 3, 10, 22.5f, 0),
            ),
        )

        // First MERGE: builtin & colliding custom win locally; only genuinely new custom exercise/plan are created.
        val firstPlan = planMerge(localSnapshot, v3Backup)
        assertFalse(firstPlan.reseedStrengthDefaults)
        assertEquals(listOf(21L), firstPlan.daysToWrite)
        assertEquals(1, firstPlan.skippedDays)
        assertEquals(StrengthExerciseTarget.Existing(1L), firstPlan.strengthExerciseMap[501L])
        assertEquals(StrengthExerciseTarget.Existing(2L), firstPlan.strengthExerciseMap[502L])
        assertEquals(StrengthExerciseTarget.Create(incomingNewCustom), firstPlan.strengthExerciseMap[503L])
        assertEquals(WorkoutPlanTarget.Existing(10L), firstPlan.workoutPlanMap[601L])
        assertEquals(WorkoutPlanTarget.Existing(11L), firstPlan.workoutPlanMap[602L])
        assertEquals(WorkoutPlanTarget.Existing(12L), firstPlan.workoutPlanMap[603L])
        assertEquals(WorkoutPlanTarget.Existing(13L), firstPlan.workoutPlanMap[604L])
        assertEquals(WorkoutPlanTarget.Create(incomingNewPlan), firstPlan.workoutPlanMap[605L])

        // Simulate applying the first MERGE to local DB, then MERGE the exact same backup a second time.
        val afterFirstMerge = localSnapshot.copy(
            dayEpochs = localSnapshot.dayEpochs + firstPlan.daysToWrite,
            strengthExercises = localSnapshot.strengthExercises + StrengthExerciseEntity(
                id = 3, builtinKey = null, nameZh = incomingNewCustom.nameZh, nameEn = incomingNewCustom.nameEn,
                bodyRegion = incomingNewCustom.bodyRegion, primaryMuscleMask = incomingNewCustom.primaryMuscleMask,
                secondaryMuscleMask = incomingNewCustom.secondaryMuscleMask,
                defaultSets = incomingNewCustom.defaultSets, defaultReps = incomingNewCustom.defaultReps,
            ),
            workoutPlans = localSnapshot.workoutPlans + WorkoutPlanEntity(
                id = 14, builtinKey = null, name = incomingNewPlan.name, sortOrder = 4,
            ),
        )

        val secondPlan = planMerge(afterFirstMerge, v3Backup)
        assertFalse(secondPlan.reseedStrengthDefaults)
        assertTrue(secondPlan.daysToWrite.isEmpty())
        assertEquals(2, secondPlan.skippedDays)
        assertTrue(secondPlan.strengthExerciseMap.values.all { it is StrengthExerciseTarget.Existing })
        assertTrue(secondPlan.workoutPlanMap.values.all { it is WorkoutPlanTarget.Existing })
        assertEquals(WorkoutPlanTarget.Existing(10L), secondPlan.workoutPlanMap[601L])
        assertEquals(WorkoutPlanTarget.Existing(14L), secondPlan.workoutPlanMap[605L])
        assertEquals(StrengthExerciseTarget.Existing(3L), secondPlan.strengthExerciseMap[503L])
    }

    @Test fun overwriteWithV2BackupSignalsReseedingStrengthDefaults() {
        val v2Backup = backup.copy(format = 2)
        val plan = planOverwrite(local, v2Backup)
        assertTrue(plan.reseedStrengthDefaults)
        assertTrue(plan.strengthExerciseMap.isEmpty())
        assertTrue(plan.workoutPlanMap.isEmpty())
        assertEquals(listOf(20L, 21L), plan.daysToWrite)
    }

    @Test fun overwriteWithV3BackupReplacesPlansExercisesAndLogs() {
        val ex = BackupStrengthExercise(
            id = 101, builtinKey = "barbell_bench_press", nameZh = "杠铃平板卧推", nameEn = "Barbell Bench Press",
            bodyRegion = "chest", primaryMuscleMask = muscleMaskOf(MuscleGroup.PECTORALS),
            secondaryMuscleMask = 0, defaultSets = 4, defaultReps = 10, archived = false,
        )
        val customPlan = BackupWorkoutPlan(
            id = 201, builtinKey = "push", name = null, sortOrder = 0,
            items = listOf(BackupWorkoutPlanItem(101, 4, 10, 60f, 0)),
        )
        val v3Backup = backup.copy(
            format = BACKUP_FORMAT,
            strengthExercises = listOf(ex),
            workoutPlans = listOf(customPlan),
            strengthLogs = emptyList(),
        )
        val localWithStrength = local.copy(
            strengthExercises = listOf(
                StrengthExerciseEntity(
                    id = 1, builtinKey = "barbell_bench_press", nameZh = "杠铃平板卧推", nameEn = "Barbell Bench Press",
                    bodyRegion = "chest", primaryMuscleMask = muscleMaskOf(MuscleGroup.PECTORALS),
                    secondaryMuscleMask = 0, defaultSets = 4, defaultReps = 10, archived = true,
                ),
            ),
            workoutPlans = listOf(WorkoutPlanEntity(id = 10, builtinKey = "push", name = "旧推日", sortOrder = 0)),
        )

        val plan = planOverwrite(localWithStrength, v3Backup)
        assertFalse(plan.reseedStrengthDefaults)
        assertEquals(StrengthExerciseTarget.Create(ex), plan.strengthExerciseMap[101L])
        assertEquals(WorkoutPlanTarget.Create(customPlan), plan.workoutPlanMap[201L])
        assertEquals(listOf(20L, 21L), plan.daysToWrite)
    }
}
