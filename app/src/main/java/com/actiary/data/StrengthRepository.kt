package com.actiary.data

import com.actiary.data.db.ExercisePerformanceRow
import com.actiary.data.db.StrengthDao
import com.actiary.data.db.StrengthExerciseEntity
import com.actiary.data.db.WorkoutPlanEntity
import com.actiary.data.db.WorkoutPlanItemEntity
import com.actiary.data.db.WorkoutPlanWithItems
import com.actiary.domain.ALL_MUSCLE_MASK
import com.actiary.domain.BodyRegion
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class PlanItemInput(
    val exerciseId: Long,
    val defaultSets: Int,
    val defaultReps: Int,
    val defaultWeightKg: Float? = null,
)

class StrengthRepository(
    private val dao: StrengthDao,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val onChanged: () -> Unit = {},
) {
    val activeExercises: Flow<List<StrengthExerciseEntity>> get() = dao.observeActiveExercises()

    val allExercises: Flow<List<StrengthExerciseEntity>> get() = dao.observeAllExercises()

    val plans: Flow<List<WorkoutPlanWithItems>> get() = dao.observePlansWithItems()

    suspend fun latestFreeformPlanId(upToDate: LocalDate): Long? = withContext(io) {
        dao.latestFreeformPlanId(upToDate.toEpochDay())
    }

    suspend fun latestPerformanceByExercise(beforeDate: LocalDate): Map<Long, ExercisePerformanceRow> = withContext(io) {
        val result = LinkedHashMap<Long, ExercisePerformanceRow>()
        for (row in dao.getPerformancesBefore(beforeDate.toEpochDay())) {
            result.putIfAbsent(row.exerciseId, row)
        }
        result
    }

    suspend fun latestEpochDayByExercise(upToDate: LocalDate): Map<Long, Long> = withContext(io) {
        dao.getLatestEpochDayByExercise(upToDate.toEpochDay()).associate { it.exerciseId to it.maxEpochDay }
    }

    suspend fun getExerciseHistory(exerciseId: Long, limit: Int = 20): List<ExercisePerformanceRow> = withContext(io) {
        dao.getExerciseHistory(exerciseId, limit)
    }

    suspend fun createPlan(name: String, items: List<PlanItemInput>): Long = withContext(io) {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Plan name must not be blank" }
        val sortOrder = (dao.maxPlanSortOrder() ?: -1) + 1
        val planId = dao.insertPlan(WorkoutPlanEntity(builtinKey = null, name = trimmed, sortOrder = sortOrder))
        dao.replacePlanItems(planId, items.toEntities(planId))
        onChanged()
        planId
    }

    suspend fun updatePlan(planId: Long, name: String?, items: List<PlanItemInput>) = withContext(io) {
        val trimmed = name?.trim()
        if (trimmed != null) {
            require(trimmed.isNotEmpty()) { "Plan name must not be blank" }
        }
        check(dao.updatePlanName(planId, trimmed) == 1)
        dao.replacePlanItems(planId, items.toEntities(planId))
        onChanged()
    }

    suspend fun deletePlan(planId: Long) = withContext(io) {
        dao.deletePlan(planId)
        onChanged()
    }

    suspend fun movePlan(planId: Long, offset: Int) = withContext(io) {
        dao.movePlan(planId, offset)
        onChanged()
    }

    suspend fun addCustomExercise(
        name: String,
        region: BodyRegion,
        primaryMuscleMask: Int,
        secondaryMuscleMask: Int = 0,
        defaultSets: Int = 4,
        defaultReps: Int = 10,
        isTimed: Boolean = false,
    ): Long = withContext(io) {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Exercise name must not be blank" }
        val primary = primaryMuscleMask and ALL_MUSCLE_MASK
        require(primary != 0) { "Primary muscle mask must not be empty" }
        val secondary = secondaryMuscleMask and primary.inv() and ALL_MUSCLE_MASK
        val id = dao.insertExercise(
            StrengthExerciseEntity(
                builtinKey = null,
                nameZh = trimmed,
                nameEn = trimmed,
                bodyRegion = region.key,
                primaryMuscleMask = primary,
                secondaryMuscleMask = secondary,
                defaultSets = defaultSets.coerceIn(1, 99),
                defaultReps = defaultReps.coerceIn(1, 999),
                isTimed = isTimed,
            ),
        )
        onChanged()
        id
    }

    suspend fun updateCustomExercise(
        id: Long,
        name: String,
        region: BodyRegion,
        primaryMuscleMask: Int,
        secondaryMuscleMask: Int = 0,
        defaultSets: Int = 4,
        defaultReps: Int = 10,
        isTimed: Boolean = false,
    ) = withContext(io) {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Exercise name must not be blank" }
        val primary = primaryMuscleMask and ALL_MUSCLE_MASK
        require(primary != 0) { "Primary muscle mask must not be empty" }
        val secondary = secondaryMuscleMask and primary.inv() and ALL_MUSCLE_MASK
        check(
            dao.updateCustomExercise(
                id = id,
                nameZh = trimmed,
                nameEn = trimmed,
                bodyRegion = region.key,
                primaryMuscleMask = primary,
                secondaryMuscleMask = secondary,
                defaultSets = defaultSets.coerceIn(1, 99),
                defaultReps = defaultReps.coerceIn(1, 999),
                isTimed = isTimed,
            ) == 1,
        )
        onChanged()
    }

    suspend fun removeCustomExercise(id: Long) = withContext(io) {
        dao.removeExercise(id)
        onChanged()
    }

    suspend fun setBuiltinExerciseHidden(id: Long, hidden: Boolean) = withContext(io) {
        dao.setBuiltinHidden(id, hidden)
        onChanged()
    }

    private fun List<PlanItemInput>.toEntities(planId: Long): List<WorkoutPlanItemEntity> =
        mapIndexed { index, item ->
            WorkoutPlanItemEntity(
                id = 0,
                planId = planId,
                exerciseId = item.exerciseId,
                defaultSets = item.defaultSets.coerceIn(1, 99),
                defaultReps = item.defaultReps.coerceIn(1, 999),
                defaultWeightKg = item.defaultWeightKg,
                sortOrder = index,
            )
        }
}
