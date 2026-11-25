package com.actiary.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class ExercisePerformanceRow(
    val exerciseId: Long,
    val epochDay: Long,
    val sets: Int,
    val reps: Int,
    val weightKg: Float?,
)

data class ExerciseLatestDayRow(
    val exerciseId: Long,
    val maxEpochDay: Long,
)

@Dao
interface StrengthDao {
    @Query("SELECT * FROM strength_exercise WHERE archived = 0 ORDER BY bodyRegion, id")
    fun observeActiveExercises(): Flow<List<StrengthExerciseEntity>>

    @Query("SELECT * FROM strength_exercise ORDER BY bodyRegion, id")
    fun observeAllExercises(): Flow<List<StrengthExerciseEntity>>

    @Query("SELECT * FROM strength_exercise ORDER BY bodyRegion, id")
    suspend fun getAllExercises(): List<StrengthExerciseEntity>

    @Transaction
    @Query("SELECT * FROM workout_plan ORDER BY sortOrder, id")
    fun observePlansWithItems(): Flow<List<WorkoutPlanWithItems>>

    @Transaction
    @Query("SELECT * FROM workout_plan ORDER BY sortOrder, id")
    suspend fun getAllPlansWithItems(): List<WorkoutPlanWithItems>

    @Query("SELECT * FROM workout_plan ORDER BY sortOrder, id")
    suspend fun getAllPlans(): List<WorkoutPlanEntity>

    @Query("SELECT * FROM workout_plan_item ORDER BY planId, sortOrder, id")
    suspend fun getAllPlanItems(): List<WorkoutPlanItemEntity>

    @Query("SELECT * FROM strength_log_item ORDER BY entryId, sortOrder, id")
    suspend fun getAllStrengthLogs(): List<StrengthLogItemEntity>

    @Insert
    suspend fun insertExercise(exercise: StrengthExerciseEntity): Long

    @Insert
    suspend fun insertExercises(exercises: List<StrengthExerciseEntity>): List<Long>

    @Query("""
        UPDATE strength_exercise
        SET nameZh = :nameZh,
            nameEn = :nameEn,
            bodyRegion = :bodyRegion,
            primaryMuscleMask = :primaryMuscleMask,
            secondaryMuscleMask = :secondaryMuscleMask,
            defaultSets = :defaultSets,
            defaultReps = :defaultReps,
            isTimed = :isTimed
        WHERE id = :id AND builtinKey IS NULL AND archived = 0
    """)
    suspend fun updateCustomExercise(
        id: Long,
        nameZh: String,
        nameEn: String,
        bodyRegion: String,
        primaryMuscleMask: Int,
        secondaryMuscleMask: Int,
        defaultSets: Int,
        defaultReps: Int,
        isTimed: Boolean,
    ): Int

    @Query("SELECT EXISTS(SELECT 1 FROM strength_exercise WHERE id = :id AND builtinKey IS NULL)")
    suspend fun isCustomExercise(id: Long): Boolean

    @Query("DELETE FROM workout_plan_item WHERE exerciseId = :exerciseId")
    suspend fun deletePlanItemsByExercise(exerciseId: Long)

    @Query("""
        DELETE FROM strength_exercise
        WHERE id = :id
          AND builtinKey IS NULL
          AND NOT EXISTS (SELECT 1 FROM strength_log_item WHERE exerciseId = :id)
    """)
    suspend fun deleteUnusedCustomExercise(id: Long): Int

    @Query("UPDATE strength_exercise SET archived = 1 WHERE id = :id AND builtinKey IS NULL")
    suspend fun archiveExercise(id: Long)

    @Query("UPDATE strength_exercise SET archived = :archived WHERE id = :id AND builtinKey IS NOT NULL")
    suspend fun setBuiltinExerciseArchived(id: Long, archived: Boolean): Int

    @Transaction
    suspend fun removeExercise(id: Long) {
        if (!isCustomExercise(id)) return
        deletePlanItemsByExercise(id)
        if (deleteUnusedCustomExercise(id) == 0) {
            archiveExercise(id)
        }
    }

    @Transaction
    suspend fun setBuiltinHidden(id: Long, hidden: Boolean) {
        if (setBuiltinExerciseArchived(id, hidden) > 0 && hidden) {
            deletePlanItemsByExercise(id)
        }
    }

    @Insert
    suspend fun insertPlan(plan: WorkoutPlanEntity): Long

    @Query("UPDATE workout_plan SET name = :name WHERE id = :id")
    suspend fun updatePlanName(id: Long, name: String?): Int

    @Query("UPDATE workout_plan SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun setPlanSortOrder(id: Long, sortOrder: Int)

    @Query("SELECT MAX(sortOrder) FROM workout_plan")
    suspend fun maxPlanSortOrder(): Int?

    @Query("DELETE FROM workout_plan WHERE id = :id")
    suspend fun deletePlan(id: Long)

    @Query("DELETE FROM workout_plan_item WHERE planId = :planId")
    suspend fun deletePlanItems(planId: Long)

    @Insert
    suspend fun insertPlanItems(items: List<WorkoutPlanItemEntity>)

    @Transaction
    suspend fun replacePlanItems(planId: Long, items: List<WorkoutPlanItemEntity>) {
        deletePlanItems(planId)
        if (items.isNotEmpty()) {
            insertPlanItems(
                items.mapIndexed { index, item ->
                    item.copy(id = 0, planId = planId, sortOrder = index)
                },
            )
        }
    }

    @Transaction
    suspend fun movePlan(id: Long, offset: Int) {
        val plans = getAllPlans().toMutableList()
        val index = plans.indexOfFirst { it.id == id }
        val destination = index + offset
        if (index < 0 || destination !in plans.indices) return
        plans.add(destination, plans.removeAt(index))
        plans.forEachIndexed { order, plan ->
            if (plan.sortOrder != order) setPlanSortOrder(plan.id, order)
        }
    }

    @Query("""
        SELECT e.planId
        FROM exercise_entry e
        INNER JOIN daily_record d ON d.epochDay = e.epochDay
        INNER JOIN workout_plan p ON p.id = e.planId
        WHERE e.epochDay <= :upToEpochDay
          AND d.status = 'EXERCISED'
          AND e.planId IS NOT NULL
        ORDER BY e.epochDay DESC, e.sortOrder DESC, e.id DESC
        LIMIT 1
    """)
    suspend fun latestFreeformPlanId(upToEpochDay: Long): Long?

    @Query("""
        SELECT sli.exerciseId, ee.epochDay, sli.sets, sli.reps, sli.weightKg
        FROM strength_log_item sli
        INNER JOIN exercise_entry ee ON ee.id = sli.entryId
        INNER JOIN daily_record dr ON dr.epochDay = ee.epochDay
        WHERE dr.status = 'EXERCISED' AND ee.epochDay < :beforeEpochDay
        ORDER BY ee.epochDay DESC, ee.sortOrder DESC, sli.sortOrder DESC, sli.id DESC
    """)
    suspend fun getPerformancesBefore(beforeEpochDay: Long): List<ExercisePerformanceRow>

    @Query("""
        SELECT sli.exerciseId, MAX(ee.epochDay) AS maxEpochDay
        FROM strength_log_item sli
        INNER JOIN exercise_entry ee ON ee.id = sli.entryId
        INNER JOIN daily_record dr ON dr.epochDay = ee.epochDay
        WHERE dr.status = 'EXERCISED' AND sli.sets > 0 AND ee.epochDay <= :upToEpochDay
        GROUP BY sli.exerciseId
    """)
    suspend fun getLatestEpochDayByExercise(upToEpochDay: Long): List<ExerciseLatestDayRow>

    @Query("""
        SELECT sli.exerciseId, ee.epochDay, sli.sets, sli.reps, sli.weightKg
        FROM strength_log_item sli
        INNER JOIN exercise_entry ee ON ee.id = sli.entryId
        INNER JOIN daily_record dr ON dr.epochDay = ee.epochDay
        WHERE dr.status = 'EXERCISED' AND sli.exerciseId = :exerciseId
        ORDER BY ee.epochDay DESC, ee.sortOrder DESC, sli.sortOrder DESC, sli.id DESC
        LIMIT :limit
    """)
    suspend fun getExerciseHistory(exerciseId: Long, limit: Int = 20): List<ExercisePerformanceRow>

    @Query("DELETE FROM strength_log_item")
    suspend fun deleteAllStrengthLogs()

    @Query("DELETE FROM workout_plan_item")
    suspend fun deleteAllPlanItems()

    @Query("DELETE FROM workout_plan")
    suspend fun deleteAllPlans()

    @Query("DELETE FROM strength_exercise")
    suspend fun deleteAllExercises()
}
