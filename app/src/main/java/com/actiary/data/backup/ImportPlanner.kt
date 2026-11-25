package com.actiary.data.backup

import com.actiary.data.db.ExerciseTypeEntity
import com.actiary.data.db.StrengthExerciseEntity
import com.actiary.data.db.WorkoutPlanEntity
import java.util.Locale

data class LocalSnapshot(
    val types: List<ExerciseTypeEntity>,
    val dayEpochs: Set<Long>,
    val strengthExercises: List<StrengthExerciseEntity> = emptyList(),
    val workoutPlans: List<WorkoutPlanEntity> = emptyList(),
)

data class ImportPlan(
    val typeMap: Map<Long, TypeTarget>,
    val daysToWrite: List<Long>,
    val skippedDays: Int,
    val strengthExerciseMap: Map<Long, StrengthExerciseTarget> = emptyMap(),
    val workoutPlanMap: Map<Long, WorkoutPlanTarget> = emptyMap(),
    val reseedStrengthDefaults: Boolean = false,
)

sealed interface TypeTarget {
    data class Existing(val id: Long) : TypeTarget
    data class Create(val spec: BackupType) : TypeTarget
}

sealed interface StrengthExerciseTarget {
    data class Existing(val id: Long) : StrengthExerciseTarget
    data class Create(val spec: BackupStrengthExercise) : StrengthExerciseTarget
}

sealed interface WorkoutPlanTarget {
    data class Existing(val id: Long) : WorkoutPlanTarget
    data class Create(val spec: BackupWorkoutPlan) : WorkoutPlanTarget
}

fun planMerge(local: LocalSnapshot, backup: BackupData): ImportPlan = planImport(local, backup, overwrite = false)

fun planOverwrite(local: LocalSnapshot, backup: BackupData): ImportPlan = planImport(local, backup, overwrite = true)

private fun planImport(local: LocalSnapshot, backup: BackupData, overwrite: Boolean): ImportPlan {
    val builtin = local.types.filter { it.builtinKey != null }.associateBy { it.builtinKey }
    val custom = mutableMapOf<String, TypeTarget>()
    if (!overwrite) {
        for (type in local.types.sortedBy { it.archived }) {
            if (type.builtinKey == null && type.name != null) {
                custom.putIfAbsent(type.name.trim(), TypeTarget.Existing(type.id))
            }
        }
    }
    val targets = backup.types.associate { type ->
        val target = when {
            type.builtinKey != null -> builtin[type.builtinKey]?.let { TypeTarget.Existing(it.id) } ?: TypeTarget.Create(type)
            overwrite -> TypeTarget.Create(type)
            else -> custom.getOrPut(requireNotNull(type.name).trim()) { TypeTarget.Create(type) }
        }
        type.id to target
    }
    val days = backup.records.map { it.epochDay }.filter { overwrite || it !in local.dayEpochs }

    val reseedStrengthDefaults = overwrite && (backup.format < 3 || backup.strengthExercises.isEmpty())
    val strengthExerciseMap: Map<Long, StrengthExerciseTarget>
    val workoutPlanMap: Map<Long, WorkoutPlanTarget>
    if (reseedStrengthDefaults) {
        strengthExerciseMap = emptyMap()
        workoutPlanMap = emptyMap()
    } else if (overwrite) {
        strengthExerciseMap = backup.strengthExercises.associate { it.id to StrengthExerciseTarget.Create(it) }
        workoutPlanMap = backup.workoutPlans.associate { it.id to WorkoutPlanTarget.Create(it) }
    } else {
        val builtinExercises = local.strengthExercises.filter { it.builtinKey != null }.associateBy { it.builtinKey }
        val customExercises = mutableMapOf<String, StrengthExerciseTarget>()
        for (exercise in local.strengthExercises.sortedBy { it.archived }) {
            if (exercise.builtinKey == null) {
                val key = normalizedExerciseKey(exercise.nameZh, exercise.nameEn)
                if (key.isNotEmpty()) {
                    customExercises.putIfAbsent(key, StrengthExerciseTarget.Existing(exercise.id))
                }
            }
        }
        strengthExerciseMap = backup.strengthExercises.associate { exercise ->
            val target = if (exercise.builtinKey != null) {
                builtinExercises[exercise.builtinKey]?.let { StrengthExerciseTarget.Existing(it.id) }
                    ?: StrengthExerciseTarget.Create(exercise)
            } else {
                customExercises.getOrPut(normalizedExerciseKey(exercise.nameZh, exercise.nameEn)) {
                    StrengthExerciseTarget.Create(exercise)
                }
            }
            exercise.id to target
        }

        val builtinPlans = local.workoutPlans.filter { it.builtinKey != null }.associateBy { it.builtinKey }
        val customPlans = mutableMapOf<String, WorkoutPlanTarget>()
        for (plan in local.workoutPlans) {
            if (plan.builtinKey == null) {
                val key = plan.name?.trim()?.lowercase(Locale.ROOT)
                if (!key.isNullOrEmpty()) {
                    customPlans.putIfAbsent(key, WorkoutPlanTarget.Existing(plan.id))
                }
            }
        }
        workoutPlanMap = backup.workoutPlans.associate { plan ->
            val target = if (plan.builtinKey != null) {
                builtinPlans[plan.builtinKey]?.let { WorkoutPlanTarget.Existing(it.id) }
                    ?: WorkoutPlanTarget.Create(plan)
            } else {
                customPlans.getOrPut(requireNotNull(plan.name).trim().lowercase(Locale.ROOT)) {
                    WorkoutPlanTarget.Create(plan)
                }
            }
            plan.id to target
        }
    }

    return ImportPlan(
        typeMap = targets,
        daysToWrite = days,
        skippedDays = backup.records.size - days.size,
        strengthExerciseMap = strengthExerciseMap,
        workoutPlanMap = workoutPlanMap,
        reseedStrengthDefaults = reseedStrengthDefaults,
    )
}

private fun normalizedExerciseKey(nameZh: String, nameEn: String): String =
    nameZh.trim().ifEmpty { nameEn.trim() }.lowercase(Locale.ROOT)
