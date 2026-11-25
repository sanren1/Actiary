package com.actiary.data

import com.actiary.data.db.ExerciseLatestDayRow
import com.actiary.data.db.ExercisePerformanceRow
import com.actiary.data.db.StrengthDao
import com.actiary.data.db.StrengthExerciseEntity
import com.actiary.data.db.StrengthLogItemEntity
import com.actiary.data.db.WorkoutPlanEntity
import com.actiary.data.db.WorkoutPlanItemEntity
import com.actiary.data.db.WorkoutPlanWithItems
import com.actiary.domain.ALL_MUSCLE_MASK
import com.actiary.domain.BodyRegion
import com.actiary.domain.MuscleGroup
import com.actiary.domain.muscleMaskOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate

private class FakeStrengthDao : StrengthDao {
    val exercises = MutableStateFlow<List<StrengthExerciseEntity>>(emptyList())
    val plans = MutableStateFlow<List<WorkoutPlanEntity>>(emptyList())
    val planItems = MutableStateFlow<List<WorkoutPlanItemEntity>>(emptyList())
    val strengthLogs = MutableStateFlow<List<StrengthLogItemEntity>>(emptyList())
    var latestPlanIdResult: Long? = null
    var lastUpToEpochDay: Long? = null
    var performancesBeforeResult: List<ExercisePerformanceRow> = emptyList()
    var lastBeforeEpochDay: Long? = null
    var latestEpochDayRowsResult: List<ExerciseLatestDayRow> = emptyList()
    var lastLatestUpToEpochDay: Long? = null
    var historyRowsResult: List<ExercisePerformanceRow> = emptyList()
    var lastHistoryQuery: Pair<Long, Int>? = null

    private var nextExerciseId = 1L
    private var nextPlanId = 1L
    private var nextPlanItemId = 1L

    override fun observeActiveExercises(): Flow<List<StrengthExerciseEntity>> =
        exercises.map { list -> list.filter { !it.archived }.sortedWith(compareBy({ it.bodyRegion }, { it.id })) }

    override fun observeAllExercises(): Flow<List<StrengthExerciseEntity>> =
        exercises.map { list -> list.sortedWith(compareBy({ it.bodyRegion }, { it.id })) }

    override suspend fun getAllExercises(): List<StrengthExerciseEntity> = observeAllExercises().first()

    override fun observePlansWithItems(): Flow<List<WorkoutPlanWithItems>> =
        plans.map { list ->
            list.sortedWith(compareBy({ it.sortOrder }, { it.id })).map { plan ->
                WorkoutPlanWithItems(
                    plan = plan,
                    items = planItems.value.filter { it.planId == plan.id }.sortedWith(compareBy({ it.sortOrder }, { it.id })),
                )
            }
        }

    override suspend fun getAllPlansWithItems(): List<WorkoutPlanWithItems> = observePlansWithItems().first()

    override suspend fun getAllPlans(): List<WorkoutPlanEntity> =
        plans.value.sortedWith(compareBy({ it.sortOrder }, { it.id }))

    override suspend fun getAllPlanItems(): List<WorkoutPlanItemEntity> =
        planItems.value.sortedWith(compareBy({ it.planId }, { it.sortOrder }, { it.id }))

    override suspend fun getAllStrengthLogs(): List<StrengthLogItemEntity> =
        strengthLogs.value.sortedWith(compareBy({ it.entryId }, { it.sortOrder }, { it.id }))

    override suspend fun insertExercise(exercise: StrengthExerciseEntity): Long {
        val id = if (exercise.id != 0L) exercise.id else nextExerciseId++
        exercises.value = exercises.value + exercise.copy(id = id)
        return id
    }

    override suspend fun insertExercises(exercises: List<StrengthExerciseEntity>): List<Long> =
        exercises.map { insertExercise(it) }

    override suspend fun updateCustomExercise(
        id: Long,
        nameZh: String,
        nameEn: String,
        bodyRegion: String,
        primaryMuscleMask: Int,
        secondaryMuscleMask: Int,
        defaultSets: Int,
        defaultReps: Int,
        isTimed: Boolean,
    ): Int {
        var updated = 0
        exercises.value = exercises.value.map {
            if (it.id == id && it.builtinKey == null && !it.archived) {
                updated++
                it.copy(
                    nameZh = nameZh,
                    nameEn = nameEn,
                    bodyRegion = bodyRegion,
                    primaryMuscleMask = primaryMuscleMask,
                    secondaryMuscleMask = secondaryMuscleMask,
                    defaultSets = defaultSets,
                    defaultReps = defaultReps,
                    isTimed = isTimed,
                )
            } else it
        }
        return updated
    }

    override suspend fun isCustomExercise(id: Long): Boolean =
        exercises.value.any { it.id == id && it.builtinKey == null }

    override suspend fun deletePlanItemsByExercise(exerciseId: Long) {
        planItems.value = planItems.value.filterNot { it.exerciseId == exerciseId }
    }

    override suspend fun deleteUnusedCustomExercise(id: Long): Int {
        val used = strengthLogs.value.any { it.exerciseId == id }
        if (used) return 0
        val before = exercises.value.size
        exercises.value = exercises.value.filterNot { it.id == id && it.builtinKey == null }
        return before - exercises.value.size
    }

    override suspend fun archiveExercise(id: Long) {
        exercises.value = exercises.value.map {
            if (it.id == id && it.builtinKey == null) it.copy(archived = true) else it
        }
    }

    override suspend fun setBuiltinExerciseArchived(id: Long, archived: Boolean): Int {
        var updated = 0
        exercises.value = exercises.value.map {
            if (it.id == id && it.builtinKey != null) {
                updated++
                it.copy(archived = archived)
            } else it
        }
        return updated
    }

    override suspend fun insertPlan(plan: WorkoutPlanEntity): Long {
        val id = if (plan.id != 0L) plan.id else nextPlanId++
        plans.value = plans.value + plan.copy(id = id)
        return id
    }

    override suspend fun updatePlanName(id: Long, name: String?): Int {
        var updated = 0
        plans.value = plans.value.map {
            if (it.id == id) {
                updated++
                it.copy(name = name)
            } else it
        }
        return updated
    }

    override suspend fun setPlanSortOrder(id: Long, sortOrder: Int) {
        plans.value = plans.value.map { if (it.id == id) it.copy(sortOrder = sortOrder) else it }
    }

    override suspend fun maxPlanSortOrder(): Int? = plans.value.maxOfOrNull { it.sortOrder }

    override suspend fun deletePlan(id: Long) {
        plans.value = plans.value.filterNot { it.id == id }
        planItems.value = planItems.value.filterNot { it.planId == id }
    }

    override suspend fun deletePlanItems(planId: Long) {
        planItems.value = planItems.value.filterNot { it.planId == planId }
    }

    override suspend fun insertPlanItems(items: List<WorkoutPlanItemEntity>) {
        val inserted = items.map { item ->
            val id = if (item.id != 0L) item.id else nextPlanItemId++
            item.copy(id = id)
        }
        planItems.value = planItems.value + inserted
    }

    override suspend fun latestFreeformPlanId(upToEpochDay: Long): Long? {
        lastUpToEpochDay = upToEpochDay
        return latestPlanIdResult
    }

    override suspend fun getPerformancesBefore(beforeEpochDay: Long): List<ExercisePerformanceRow> {
        lastBeforeEpochDay = beforeEpochDay
        return performancesBeforeResult
    }

    override suspend fun getLatestEpochDayByExercise(upToEpochDay: Long): List<ExerciseLatestDayRow> {
        lastLatestUpToEpochDay = upToEpochDay
        return latestEpochDayRowsResult
    }

    override suspend fun getExerciseHistory(exerciseId: Long, limit: Int): List<ExercisePerformanceRow> {
        lastHistoryQuery = exerciseId to limit
        return historyRowsResult.take(limit)
    }

    override suspend fun deleteAllStrengthLogs() { strengthLogs.value = emptyList() }
    override suspend fun deleteAllPlanItems() { planItems.value = emptyList() }
    override suspend fun deleteAllPlans() { plans.value = emptyList() }
    override suspend fun deleteAllExercises() { exercises.value = emptyList() }
}

class StrengthRepositoryTest {
    private val dao = FakeStrengthDao()
    private var changedCount = 0
    private val repo = StrengthRepository(dao, onChanged = { changedCount++ })

    @Test fun removeUnusedCustomExerciseRemovesPlanItemsAndHardDeletes() = runBlocking {
        val exId = repo.addCustomExercise(
            name = "Cable Fly",
            region = BodyRegion.CHEST,
            primaryMuscleMask = muscleMaskOf(MuscleGroup.PECTORALS),
        )
        val planId = repo.createPlan("Push", listOf(PlanItemInput(exId, 4, 12, 15f)))

        repo.removeCustomExercise(exId)

        assertTrue(repo.allExercises.first().none { it.id == exId })
        assertTrue(dao.getAllPlanItems().none { it.planId == planId && it.exerciseId == exId })
    }

    @Test fun removeReferencedCustomExerciseRemovesPlanItemsAndArchives() = runBlocking {
        val exId = repo.addCustomExercise(
            name = "Landmine Press",
            region = BodyRegion.SHOULDERS,
            primaryMuscleMask = muscleMaskOf(MuscleGroup.FRONT_SIDE_DELTS),
        )
        val planId = repo.createPlan("Upper", listOf(PlanItemInput(exId, 4, 10, 40f)))
        dao.strengthLogs.value = listOf(
            StrengthLogItemEntity(id = 1, entryId = 10, exerciseId = exId, sets = 4, reps = 10, weightKg = 40f, sortOrder = 0),
        )

        repo.removeCustomExercise(exId)

        assertTrue(repo.activeExercises.first().none { it.id == exId })
        val archived = repo.allExercises.first().single { it.id == exId }
        assertTrue(archived.archived)
        assertTrue(dao.getAllPlanItems().none { it.planId == planId && it.exerciseId == exId })
    }

    @Test fun removeCustomExerciseDoesNotStripBuiltinFromPlans() = runBlocking {
        val builtinId = dao.insertExercise(
            StrengthExerciseEntity(
                builtinKey = "barbell_bench_press",
                nameZh = "杠铃平板卧推",
                nameEn = "Barbell Bench Press",
                bodyRegion = BodyRegion.CHEST.key,
                primaryMuscleMask = muscleMaskOf(MuscleGroup.PECTORALS),
                secondaryMuscleMask = muscleMaskOf(MuscleGroup.TRICEPS),
                defaultSets = 4,
                defaultReps = 10,
            ),
        )
        val planId = repo.createPlan("Push", listOf(PlanItemInput(builtinId, 4, 10, 60f)))

        repo.removeCustomExercise(builtinId)

        assertFalse(repo.allExercises.first().single { it.id == builtinId }.archived)
        assertEquals(1, dao.getAllPlanItems().count { it.planId == planId && it.exerciseId == builtinId })
    }

    @Test fun hidingBuiltinExerciseArchivesAndRemovesFromPlans() = runBlocking {
        val builtinId = dao.insertExercise(
            StrengthExerciseEntity(
                builtinKey = "pull_up",
                nameZh = "引体向上",
                nameEn = "Pull-Up",
                bodyRegion = BodyRegion.BACK.key,
                primaryMuscleMask = muscleMaskOf(MuscleGroup.LATS),
                secondaryMuscleMask = muscleMaskOf(MuscleGroup.BICEPS),
                defaultSets = 4,
                defaultReps = 8,
            ),
        )
        val planId = repo.createPlan("Pull", listOf(PlanItemInput(builtinId, 4, 8, null)))

        repo.setBuiltinExerciseHidden(builtinId, true)

        assertTrue(repo.activeExercises.first().none { it.id == builtinId })
        assertTrue(repo.allExercises.first().single { it.id == builtinId }.archived)
        assertTrue(dao.getAllPlanItems().none { it.planId == planId && it.exerciseId == builtinId })

        repo.setBuiltinExerciseHidden(builtinId, false)
        assertFalse(repo.allExercises.first().single { it.id == builtinId }.archived)
    }

    @Test fun addCustomExerciseValidatesStripsOverlapAndClamps() = runBlocking {
        try {
            repo.addCustomExercise("   ", BodyRegion.CHEST, muscleMaskOf(MuscleGroup.PECTORALS))
            fail("Expected blank name to be rejected")
        } catch (_: IllegalArgumentException) {
        }

        try {
            repo.addCustomExercise("Press", BodyRegion.CHEST, primaryMuscleMask = 0)
            fail("Expected empty primary mask to be rejected")
        } catch (_: IllegalArgumentException) {
        }

        try {
            repo.addCustomExercise("Press", BodyRegion.CHEST, primaryMuscleMask = (1 shl 20))
            fail("Expected out-of-range primary mask to be rejected")
        } catch (_: IllegalArgumentException) {
        }

        val pecs = muscleMaskOf(MuscleGroup.PECTORALS)
        val triceps = muscleMaskOf(MuscleGroup.TRICEPS)
        val id = repo.addCustomExercise(
            name = "  Weighted Dip  ",
            region = BodyRegion.CHEST,
            primaryMuscleMask = pecs or (1 shl 20),
            secondaryMuscleMask = pecs or triceps or (1 shl 21),
            defaultSets = 150,
            defaultReps = 0,
        )
        val created = repo.allExercises.first().single { it.id == id }
        assertNull(created.builtinKey)
        assertEquals("Weighted Dip", created.nameZh)
        assertEquals("Weighted Dip", created.nameEn)
        assertEquals(BodyRegion.CHEST.key, created.bodyRegion)
        assertEquals(pecs and ALL_MUSCLE_MASK, created.primaryMuscleMask)
        assertEquals(triceps, created.secondaryMuscleMask)
        assertEquals(99, created.defaultSets)
        assertEquals(1, created.defaultReps)
    }

    @Test fun planCrudAndLatestFreeformPlanId() = runBlocking {
        val ex1 = repo.addCustomExercise("Ex1", BodyRegion.CHEST, muscleMaskOf(MuscleGroup.PECTORALS))
        val ex2 = repo.addCustomExercise("Ex2", BodyRegion.BACK, muscleMaskOf(MuscleGroup.LATS))

        val p1 = repo.createPlan("  Day A  ", listOf(PlanItemInput(ex1, 4, 10, 50f)))
        val p2 = repo.createPlan("Day B", listOf(PlanItemInput(ex2, 3, 12, null)))

        assertEquals(listOf("Day A", "Day B"), repo.plans.first().map { it.plan.name })
        assertEquals(listOf(0, 1), repo.plans.first().map { it.plan.sortOrder })

        repo.movePlan(p2, -1)
        assertEquals(listOf(p2, p1), repo.plans.first().map { it.plan.id })

        repo.updatePlan(p1, "  Day A2  ", listOf(PlanItemInput(ex2, 5, 5, 80f)))
        val updated = repo.plans.first().single { it.plan.id == p1 }
        assertEquals("Day A2", updated.plan.name)
        assertEquals(listOf(ex2), updated.items.map { it.exerciseId })

        dao.latestPlanIdResult = p1
        val date = LocalDate.of(2026, 11, 24)
        assertEquals(p1, repo.latestFreeformPlanId(date))
        assertEquals(date.toEpochDay(), dao.lastUpToEpochDay)

        repo.deletePlan(p2)
        assertEquals(listOf(p1), repo.plans.first().map { it.plan.id })
        assertTrue(changedCount > 0)
    }

    @Test fun updatePlanAllowsNullNameForBuiltinLocalization() = runBlocking {
        val exId = repo.addCustomExercise("Push-Up", BodyRegion.CHEST, muscleMaskOf(MuscleGroup.PECTORALS))
        val builtinPlanId = dao.insertPlan(WorkoutPlanEntity(builtinKey = "push", name = null, sortOrder = 0))

        repo.updatePlan(builtinPlanId, null, listOf(PlanItemInput(exId, 3, 15, null)))
        val updated = repo.plans.first().single { it.plan.id == builtinPlanId }
        assertNull(updated.plan.name)
        assertEquals(1, updated.items.size)
    }

    @Test fun addAndUpdateCustomExercisePersistsIsTimed() = runBlocking {
        val id = repo.addCustomExercise(
            name = "L-Sit Hold",
            region = BodyRegion.ABS,
            primaryMuscleMask = muscleMaskOf(MuscleGroup.ABS),
            defaultSets = 3,
            defaultReps = 30,
            isTimed = true,
        )
        val created = repo.allExercises.first().single { it.id == id }
        assertTrue(created.isTimed)

        repo.updateCustomExercise(
            id = id,
            name = "L-Sit Reps",
            region = BodyRegion.ABS,
            primaryMuscleMask = muscleMaskOf(MuscleGroup.ABS),
            defaultSets = 4,
            defaultReps = 12,
            isTimed = false,
        )
        val updated = repo.allExercises.first().single { it.id == id }
        assertFalse(updated.isTimed)
        assertEquals("L-Sit Reps", updated.nameZh)
        assertEquals(4, updated.defaultSets)
        assertEquals(12, updated.defaultReps)
    }

    @Test fun latestPerformanceLatestEpochDayAndHistoryDelegateAndDeduplicate() = runBlocking {
        val date = LocalDate.of(2026, 11, 25)
        val row1Latest = ExercisePerformanceRow(exerciseId = 1L, epochDay = 200L, sets = 4, reps = 10, weightKg = 60f)
        val row1Older = ExercisePerformanceRow(exerciseId = 1L, epochDay = 190L, sets = 3, reps = 10, weightKg = 55f)
        val row2Latest = ExercisePerformanceRow(exerciseId = 2L, epochDay = 195L, sets = 3, reps = 30, weightKg = null)
        dao.performancesBeforeResult = listOf(row1Latest, row1Older, row2Latest)

        val perfMap = repo.latestPerformanceByExercise(date)
        assertEquals(date.toEpochDay(), dao.lastBeforeEpochDay)
        assertEquals(mapOf(1L to row1Latest, 2L to row2Latest), perfMap)

        dao.latestEpochDayRowsResult = listOf(
            ExerciseLatestDayRow(exerciseId = 1L, maxEpochDay = 200L),
            ExerciseLatestDayRow(exerciseId = 2L, maxEpochDay = 195L),
        )
        val dayMap = repo.latestEpochDayByExercise(date)
        assertEquals(date.toEpochDay(), dao.lastLatestUpToEpochDay)
        assertEquals(mapOf(1L to 200L, 2L to 195L), dayMap)

        dao.historyRowsResult = listOf(row1Latest, row1Older)
        val history = repo.getExerciseHistory(exerciseId = 1L, limit = 1)
        assertEquals(1L to 1, dao.lastHistoryQuery)
        assertEquals(listOf(row1Latest), history)
    }
}
