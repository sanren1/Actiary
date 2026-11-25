package com.actiary.ui.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.actiary.R
import com.actiary.data.db.StrengthExerciseEntity
import com.actiary.data.db.WorkoutPlanEntity
import com.actiary.domain.BodyRegion
import com.actiary.domain.MuscleGroup
import com.actiary.domain.formatWeightKg

@StringRes
fun MuscleGroup.labelRes(): Int = when (this) {
    MuscleGroup.PECTORALS -> R.string.muscle_pectorals
    MuscleGroup.UPPER_BACK -> R.string.muscle_upper_back
    MuscleGroup.LATS -> R.string.muscle_lats
    MuscleGroup.LOWER_BACK -> R.string.muscle_lower_back
    MuscleGroup.FRONT_SIDE_DELTS -> R.string.muscle_front_side_delts
    MuscleGroup.REAR_DELTS -> R.string.muscle_rear_delts
    MuscleGroup.BICEPS -> R.string.muscle_biceps
    MuscleGroup.TRICEPS -> R.string.muscle_triceps
    MuscleGroup.FOREARMS -> R.string.muscle_forearms
    MuscleGroup.ABS -> R.string.muscle_abs
    MuscleGroup.QUADS -> R.string.muscle_quads
    MuscleGroup.HAMSTRINGS -> R.string.muscle_hamstrings
    MuscleGroup.GLUTES -> R.string.muscle_glutes
    MuscleGroup.CALVES -> R.string.muscle_calves
}

@StringRes
fun BodyRegion.labelRes(): Int = when (this) {
    BodyRegion.CHEST -> R.string.region_chest
    BodyRegion.BACK -> R.string.region_back
    BodyRegion.SHOULDERS -> R.string.region_shoulders
    BodyRegion.ARMS -> R.string.region_arms
    BodyRegion.LEGS -> R.string.region_legs
    BodyRegion.ABS -> R.string.region_abs
}

@StringRes
internal fun builtinPlanLabelRes(builtinKey: String?): Int? = when (builtinKey) {
    "push" -> R.string.strength_plan_push
    "pull" -> R.string.strength_plan_pull
    "legs" -> R.string.strength_plan_legs
    else -> null
}

@Composable
fun workoutPlanLabel(plan: WorkoutPlanEntity): String {
    val customName = plan.name
    if (!customName.isNullOrBlank()) return customName
    val resId = builtinPlanLabelRes(plan.builtinKey) ?: return ""
    return stringResource(resId)
}

fun exerciseDisplayName(exercise: StrengthExerciseEntity, isChinese: Boolean): String =
    if (isChinese && exercise.nameZh.isNotBlank()) {
        exercise.nameZh
    } else {
        exercise.nameEn.ifBlank { exercise.nameZh }
    }

@Composable
fun isChineseLocale(): Boolean {
    val locales = LocalConfiguration.current.locales
    return !locales.isEmpty && locales[0]?.language == "zh"
}

fun formatStrengthItemMetrics(
    sets: Int,
    reps: Int,
    weightKg: Float?,
    noWeightFmt: (Int, Int) -> String,
    weightFmt: (Int, Int, String) -> String,
): String = if (weightKg != null && weightKg > 0f) {
    weightFmt(sets, reps, formatWeightKg(weightKg))
} else {
    noWeightFmt(sets, reps)
}

@Composable
fun formatStrengthItemMetrics(
    sets: Int,
    reps: Int,
    weightKg: Float?,
    isTimed: Boolean,
): String = if (weightKg != null && weightKg > 0f) {
    val resId = if (isTimed) R.string.strength_item_format_timed_weight else R.string.strength_item_format_weight
    stringResource(resId, sets, reps, formatWeightKg(weightKg))
} else {
    val resId = if (isTimed) R.string.strength_item_format_timed_no_weight else R.string.strength_item_format_no_weight
    stringResource(resId, sets, reps)
}

@Composable
fun formatDaysAgoLabel(daysAgo: Int): String = when {
    daysAgo <= 0 -> stringResource(R.string.records_muscle_today)
    daysAgo == 1 -> stringResource(R.string.records_muscle_yesterday)
    else -> stringResource(R.string.records_muscle_days_ago, daysAgo)
}

fun summarizeStrengthMuscles(
    primaryMasks: List<Int>,
    maxMuscles: Int = 3,
): List<MuscleGroup> {
    if (maxMuscles <= 0 || primaryMasks.isEmpty()) return emptyList()
    val counts = IntArray(MuscleGroup.entries.size)
    for (mask in primaryMasks) {
        if (mask == 0) continue
        for (muscle in MuscleGroup.entries) {
            if ((mask and (1 shl muscle.bit)) != 0) {
                counts[muscle.ordinal]++
            }
        }
    }
    return MuscleGroup.entries
        .filter { counts[it.ordinal] > 0 }
        .sortedWith(compareByDescending<MuscleGroup> { counts[it.ordinal] }.thenBy { it.ordinal })
        .take(maxMuscles)
}
