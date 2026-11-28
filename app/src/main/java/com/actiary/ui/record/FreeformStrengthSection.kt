package com.actiary.ui.record

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.actiary.R
import com.actiary.data.appearance.UiStyle
import com.actiary.data.db.ExercisePerformanceRow
import com.actiary.data.db.StrengthExerciseEntity
import com.actiary.data.db.WorkoutPlanWithItems
import com.actiary.domain.DraftEntry
import com.actiary.domain.DraftStrengthItem
import com.actiary.domain.MuscleActivation
import com.actiary.domain.MuscleGroup
import com.actiary.domain.computeDayMuscleHighlights
import com.actiary.domain.formatWeightKg
import com.actiary.domain.musclesFromMask
import com.actiary.domain.parseWeightKg
import com.actiary.domain.recommendNextPlanId
import com.actiary.glass.GlassOverlayPanel
import com.actiary.glass.rememberOverlayVisibility
import com.actiary.p5.P5ShapeVariant
import com.actiary.p5.p5Shape
import com.actiary.p5.p5Surface
import com.actiary.theme.LocalP5Tokens
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.LocalUiStyle
import com.actiary.theme.asColor
import com.actiary.ui.common.AnatomyBodyMap
import com.actiary.ui.common.BodyMapStyle
import com.actiary.ui.common.exerciseDisplayName
import com.actiary.ui.common.formatStrengthItemMetrics
import com.actiary.ui.common.isChineseLocale
import com.actiary.ui.common.labelRes
import com.actiary.ui.common.workoutPlanLabel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun FreeformStrengthSection(
    entry: DraftEntry,
    plans: List<WorkoutPlanWithItems>,
    exercisesById: Map<Long, StrengthExerciseEntity>,
    latestPlanId: Long?,
    lastPerformanceByExercise: Map<Long, ExercisePerformanceRow> = emptyMap(),
    planResetToken: Int,
    enabled: Boolean,
    onTogglePlan: (WorkoutPlanWithItems) -> Unit,
    onManagePlans: () -> Unit,
    onOpenPicker: (MuscleGroup?) -> Unit,
    onSaveAsPlan: () -> Unit,
    onClearAll: () -> Unit,
    onUpdateItem: (DraftStrengthItem) -> Unit,
    onRemoveItem: (Long) -> Unit,
    onMoveItem: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onValidityChange: (Boolean) -> Unit,
) {
    val t = LocalThemeTokens.current
    val n = t.neutrals
    val a = t.accent
    val isP5 = LocalUiStyle.current == UiStyle.PERSONA_5
    val p5 = LocalP5Tokens.current
    val p5Stroke = if (t.dark) p5.strokeLight.asColor() else p5.strokeDark.asColor()
    val isChinese = isChineseLocale()
    val orderedPlans = remember(plans) { plans.sortedBy { it.plan.sortOrder } }
    val selectedPlanIds = remember(entry.planId, entry.strengthItems, orderedPlans) {
        buildSet {
            entry.strengthItems.forEach { item ->
                item.sourcePlanId?.let(::add)
            }
            val legacyPlanId = entry.planId
            if (legacyPlanId != null && entry.strengthItems.any { it.sourcePlanId == null }) {
                val planExerciseIds = orderedPlans
                    .firstOrNull { it.plan.id == legacyPlanId }
                    ?.items
                    ?.mapTo(HashSet()) { it.exerciseId }
                    .orEmpty()
                if (entry.strengthItems.any { it.sourcePlanId == null && it.exerciseId in planExerciseIds }) {
                    add(legacyPlanId)
                }
            }
        }
    }
    val recommendedPlanId = remember(orderedPlans, latestPlanId) {
        recommendNextPlanId(orderedPlans.map { it.plan.id }, latestPlanId)
    }
    val activations = remember(entry.strengthItems, exercisesById) {
        computeDayMuscleHighlights(
            entry.strengthItems.mapNotNull { item ->
                exercisesById[item.exerciseId]?.let { ex ->
                    ex.primaryMuscleMask to ex.secondaryMuscleMask
                }
            },
        )
    }
    val primaryMuscles = remember(activations) {
        activations.filterValues { it == MuscleActivation.PRIMARY }.keys.toList()
    }

    var invalidItemIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    LaunchedEffect(entry.strengthItems) {
        val currentIds = entry.strengthItems.mapTo(HashSet()) { it.localId }
        val pruned = invalidItemIds.intersect(currentIds)
        if (pruned != invalidItemIds) invalidItemIds = pruned
    }
    LaunchedEffect(invalidItemIds) {
        onValidityChange(invalidItemIds.isEmpty())
    }

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PlanTemplateStrip(
            plans = orderedPlans,
            selectedPlanIds = selectedPlanIds,
            recommendedPlanId = if (entry.strengthItems.isEmpty()) recommendedPlanId else null,
            enabled = enabled,
            onTogglePlan = onTogglePlan,
            onManagePlans = onManagePlans,
        )

        val mapShape = if (isP5) RoundedCornerShape(2.dp) else RoundedCornerShape(12.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(mapShape)
                .background(if (isP5) n.surfaceMuted.asColor() else n.surface.asColor())
                .then(
                    if (isP5) {
                        Modifier.border(
                            1.5.dp,
                            p5Stroke.copy(alpha = if (t.dark) 0.55f else 0.85f),
                            mapShape,
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AnatomyBodyMap(
                style = BodyMapStyle.Highlight(activations),
                modifier = Modifier.fillMaxWidth().height(160.dp),
                showLabels = true,
                onMuscleClick = if (enabled) { muscle -> onOpenPicker(muscle) } else null,
            )
            if (primaryMuscles.isNotEmpty()) {
                val muscleText = primaryMuscles.map { stringResource(it.labelRes()) }.joinToString(" · ")
                BasicText(
                    stringResource(R.string.strength_active_muscles, muscleText),
                    style = TextStyle(n.textSecondary.asColor(), 12.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (entry.strengthItems.isNotEmpty()) {
            val showReorder = entry.strengthItems.size > 1
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                entry.strengthItems.forEachIndexed { index, item ->
                    key(planResetToken, item.localId) {
                        val exercise = exercisesById[item.exerciseId]
                        StrengthItemRow(
                            item = item,
                            exercise = exercise,
                            lastPerf = lastPerformanceByExercise[item.exerciseId],
                            isChinese = isChinese,
                            enabled = enabled,
                            showReorder = showReorder,
                            canMoveUp = index > 0,
                            canMoveDown = index < entry.strengthItems.lastIndex,
                            onMoveUp = { onMoveItem(index, index - 1) },
                            onMoveDown = { onMoveItem(index, index + 1) },
                            onUpdate = onUpdateItem,
                            onRemove = { onRemoveItem(item.localId) },
                            onValidityChange = { valid ->
                                invalidItemIds = if (valid) {
                                    invalidItemIds - item.localId
                                } else {
                                    invalidItemIds + item.localId
                                }
                            },
                        )
                    }
                }
            }
        }

        val btnShape = RoundedCornerShape(10.dp)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .heightIn(min = 42.dp)
                    .then(
                        if (isP5) {
                            Modifier.p5Surface(
                                variant = P5ShapeVariant.PILL,
                                fillColor = p5.brandFill.asColor(),
                                strokeColor = p5Stroke,
                                shadowColor = p5.strokeDark.asColor(),
                                strokeWidth = 2.dp,
                                shadowOffset = DpOffset(2.dp, 2.dp),
                            )
                        } else {
                            Modifier
                                .clip(btnShape)
                                .background(n.surfaceMuted.asColor())
                        },
                    )
                    .clickable(enabled = enabled, role = Role.Button) { onOpenPicker(null) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            ) {
                Icon(
                    Icons.Outlined.Add,
                    null,
                    Modifier.size(18.dp),
                    tint = if (isP5) p5.onBrandFill.asColor() else a.accentAction.asColor(),
                )
                BasicText(
                    stringResource(R.string.strength_add_exercise),
                    style = TextStyle(
                        color = if (isP5) p5.onBrandFill.asColor() else a.accentAction.asColor(),
                        fontSize = 14.sp,
                        fontWeight = if (isP5) FontWeight.Bold else FontWeight.Medium,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (entry.strengthItems.isNotEmpty()) {
                Box(
                    Modifier
                        .heightIn(min = 42.dp)
                        .then(
                            if (isP5) {
                                Modifier.p5Surface(
                                    variant = P5ShapeVariant.BADGE,
                                    fillColor = n.surface.asColor(),
                                    strokeColor = p5Stroke,
                                    shadowColor = Color.Transparent,
                                    strokeWidth = 1.5.dp,
                                    shadowOffset = DpOffset.Zero,
                                )
                            } else {
                                Modifier
                                    .clip(btnShape)
                                    .background(n.surfaceMuted.asColor())
                            },
                        )
                        .clickable(enabled = enabled, role = Role.Button, onClick = onSaveAsPlan)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        stringResource(R.string.strength_plan_save_current),
                        style = TextStyle(
                            color = n.textPrimary.asColor(),
                            fontSize = 14.sp,
                            fontWeight = if (isP5) FontWeight.Bold else FontWeight.Medium,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(
                    Modifier
                        .heightIn(min = 42.dp)
                        .then(
                            if (isP5) {
                                Modifier.p5Surface(
                                    variant = P5ShapeVariant.BADGE,
                                    fillColor = n.surface.asColor(),
                                    strokeColor = p5Stroke.copy(alpha = if (t.dark) 0.6f else 0.85f),
                                    shadowColor = Color.Transparent,
                                    strokeWidth = 1.5.dp,
                                    shadowOffset = DpOffset.Zero,
                                )
                            } else {
                                Modifier
                                    .clip(btnShape)
                                    .background(n.surfaceMuted.asColor())
                            },
                        )
                        .clickable(enabled = enabled, role = Role.Button, onClick = onClearAll)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        stringResource(R.string.strength_clear_all),
                        style = TextStyle(
                            color = if (isP5) n.textPrimary.asColor() else n.textSecondary.asColor(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanTemplateStrip(
    plans: List<WorkoutPlanWithItems>,
    selectedPlanIds: Set<Long>,
    recommendedPlanId: Long?,
    enabled: Boolean,
    onTogglePlan: (WorkoutPlanWithItems) -> Unit,
    onManagePlans: () -> Unit,
) {
    val t = LocalThemeTokens.current
    val n = t.neutrals
    val a = t.accent
    val isP5 = LocalUiStyle.current == UiStyle.PERSONA_5
    val p5 = LocalP5Tokens.current
    val p5Stroke = if (t.dark) p5.strokeLight.asColor() else p5.strokeDark.asColor()
    val pillShape = RoundedCornerShape(50)

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            plans.forEach { plan ->
                val isSelected = plan.plan.id in selectedPlanIds
                val isRecommended = recommendedPlanId == plan.plan.id
                Row(
                    Modifier
                        .heightIn(min = 34.dp)
                        .then(
                            if (isP5) {
                                Modifier.p5Surface(
                                    variant = P5ShapeVariant.BADGE,
                                    fillColor = if (isSelected) p5.brandFill.asColor() else n.surface.asColor(),
                                    strokeColor = if (isSelected) p5Stroke else p5Stroke.copy(alpha = if (t.dark) 0.6f else 0.85f),
                                    shadowColor = if (isSelected) p5.strokeDark.asColor() else Color.Transparent,
                                    strokeWidth = if (isSelected) 2.dp else 1.5.dp,
                                    shadowOffset = if (isSelected) DpOffset(2.dp, 2.dp) else DpOffset.Zero,
                                )
                            } else {
                                Modifier
                                    .clip(pillShape)
                                    .background(if (isSelected) a.accentSubtle.asColor() else n.surfaceMuted.asColor())
                                    .then(
                                        if (isSelected) Modifier.border(1.dp, a.accentStroke.asColor(), pillShape)
                                        else Modifier
                                    )
                            },
                        )
                        .clickable(enabled = enabled, role = Role.Button) { onTogglePlan(plan) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Outlined.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (isP5) p5.onBrandFill.asColor() else a.accentAction.asColor(),
                        )
                    }
                    BasicText(
                        workoutPlanLabel(plan.plan),
                        style = TextStyle(
                            color = when {
                                isP5 && isSelected -> p5.onBrandFill.asColor()
                                isP5 -> n.textPrimary.asColor()
                                isSelected -> a.accentAction.asColor()
                                else -> n.textPrimary.asColor()
                            },
                            fontSize = 13.sp,
                            fontWeight = when {
                                isP5 && isSelected -> FontWeight.Bold
                                isSelected -> FontWeight.SemiBold
                                else -> FontWeight.Medium
                            },
                        ),
                        maxLines = 1,
                    )
                    if (isRecommended) {
                        Box(
                            Modifier
                                .clip(if (isP5) p5Shape(P5ShapeVariant.BADGE) else pillShape)
                                .background(
                                    if (isP5) p5.highlightBadge.asColor()
                                    else a.accentAction.asColor().copy(alpha = 0.14f),
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            BasicText(
                                stringResource(R.string.strength_plan_next_badge),
                                style = TextStyle(
                                    color = if (isP5) p5.onHighlightBadge.asColor() else a.accentAction.asColor(),
                                    fontSize = 10.sp,
                                    fontWeight = if (isP5) FontWeight.Black else FontWeight.SemiBold,
                                ),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }

        Box(
            Modifier
                .heightIn(min = 34.dp)
                .then(
                    if (isP5) {
                        Modifier.p5Surface(
                            variant = P5ShapeVariant.BADGE,
                            fillColor = n.surface.asColor(),
                            strokeColor = p5Stroke,
                            shadowColor = Color.Transparent,
                            strokeWidth = 1.5.dp,
                            shadowOffset = DpOffset.Zero,
                        )
                    } else {
                        Modifier
                            .clip(pillShape)
                            .background(n.surfaceMuted.asColor())
                    },
                )
                .clickable(enabled = enabled, role = Role.Button, onClick = onManagePlans)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                stringResource(R.string.strength_plan_manage),
                style = TextStyle(
                    color = if (isP5) n.textPrimary.asColor() else n.textSecondary.asColor(),
                    fontSize = 12.sp,
                    fontWeight = if (isP5) FontWeight.Bold else FontWeight.Medium,
                ),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun StrengthItemRow(
    item: DraftStrengthItem,
    exercise: StrengthExerciseEntity?,
    lastPerf: ExercisePerformanceRow?,
    isChinese: Boolean,
    enabled: Boolean,
    showReorder: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onUpdate: (DraftStrengthItem) -> Unit,
    onRemove: () -> Unit,
    onValidityChange: (Boolean) -> Unit,
) {
    val t = LocalThemeTokens.current
    val n = t.neutrals
    val a = t.accent
    val isP5 = LocalUiStyle.current == UiStyle.PERSONA_5
    val p5 = LocalP5Tokens.current
    val p5Stroke = if (t.dark) p5.strokeLight.asColor() else p5.strokeDark.asColor()
    val haptics = LocalHapticFeedback.current
    val isTimed = exercise?.isTimed == true
    val name = exercise?.let { exerciseDisplayName(it, isChinese) }.orEmpty()
    val primaryMuscles = exercise?.let { ex ->
        musclesFromMask(ex.primaryMuscleMask).map { stringResource(it.labelRes()) }.joinToString(" · ")
    }.orEmpty()

    var setsText by remember { mutableStateOf(item.sets.toString()) }
    var repsText by remember { mutableStateOf(item.reps.toString()) }
    var weightText by remember { mutableStateOf(item.weightKg?.let(::formatWeightKg).orEmpty()) }

    LaunchedEffect(item.exerciseId, item.sets, item.reps, item.weightKg) {
        if (parseStrengthSets(setsText) != item.sets) setsText = item.sets.toString()
        if (parseStrengthReps(repsText) != item.reps) repsText = item.reps.toString()
        val parsedWeight = if (weightText.isEmpty()) null else parseWeightKg(weightText)
        if (weightText.isEmpty() && item.weightKg != null) {
            weightText = formatWeightKg(item.weightKg)
        } else if (weightText.isNotEmpty() && parsedWeight != null && parsedWeight != item.weightKg) {
            weightText = item.weightKg?.let(::formatWeightKg).orEmpty()
        }
    }

    val setsValid = parseStrengthSets(setsText) != null
    val repsValid = parseStrengthReps(repsText) != null
    val weightValid = weightText.isEmpty() || parseWeightKg(weightText) != null

    LaunchedEffect(setsValid, repsValid, weightValid) {
        onValidityChange(setsValid && repsValid && weightValid)
    }

    val currentSets = parseStrengthSets(setsText) ?: item.sets
    val currentReps = parseStrengthReps(repsText) ?: item.reps
    val minReps = if (isTimed) 5 else 1
    val cardShape = if (isP5) RoundedCornerShape(2.dp) else RoundedCornerShape(12.dp)

    Column(
        Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(n.surface.asColor())
            .then(
                if (isP5) {
                    Modifier.border(
                        1.5.dp,
                        p5Stroke.copy(alpha = if (t.dark) 0.65f else 0.9f),
                        cardShape,
                    )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BasicText(
                    name,
                    style = TextStyle(n.textPrimary.asColor(), 14.sp, if (isP5) FontWeight.Bold else FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (primaryMuscles.isNotEmpty()) {
                    BasicText(
                        primaryMuscles,
                        style = TextStyle(n.textSecondary.asColor(), 11.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (lastPerf != null) {
                    val formattedLast = formatStrengthItemMetrics(
                        sets = lastPerf.sets,
                        reps = lastPerf.reps,
                        weightKg = lastPerf.weightKg,
                        isTimed = isTimed,
                    )
                    val applyLastCd = stringResource(R.string.strength_apply_last_perf_cd)
                    val pillShape = RoundedCornerShape(50)
                    Box(
                        Modifier
                            .then(
                                if (isP5) {
                                    Modifier.p5Surface(
                                        variant = P5ShapeVariant.BADGE,
                                        fillColor = p5.highlightBadge.asColor(),
                                        strokeColor = p5.strokeDark.asColor(),
                                        shadowColor = Color.Transparent,
                                        strokeWidth = 1.5.dp,
                                        shadowOffset = DpOffset.Zero,
                                    )
                                } else {
                                    Modifier
                                        .clip(pillShape)
                                        .background(a.accentSubtle.asColor())
                                        .border(1.dp, a.accentStroke.asColor().copy(alpha = 0.4f), pillShape)
                                },
                            )
                            .clickable(enabled = enabled, role = Role.Button) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val nextSets = lastPerf.sets.coerceIn(1, 99)
                                val nextReps = lastPerf.reps.coerceIn(1, 999)
                                val nextWeight = lastPerf.weightKg
                                setsText = nextSets.toString()
                                repsText = nextReps.toString()
                                weightText = nextWeight?.let(::formatWeightKg).orEmpty()
                                onUpdate(item.copy(sets = nextSets, reps = nextReps, weightKg = nextWeight))
                            }
                            .semantics { contentDescription = applyLastCd }
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        BasicText(
                            stringResource(R.string.strength_last_perf, formattedLast),
                            style = TextStyle(
                                color = if (isP5) p5.onHighlightBadge.asColor() else a.accentAction.asColor(),
                                fontSize = 11.sp,
                                fontWeight = if (isP5) FontWeight.Bold else FontWeight.Medium,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            StrengthRowActionButtons(
                showReorder = showReorder,
                canMoveUp = canMoveUp,
                canMoveDown = canMoveDown,
                enabled = enabled,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
                onRemove = onRemove,
                removeContentDescription = stringResource(R.string.record_remove_entry),
            )
        }

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CompactStrengthStepperField(
                value = setsText,
                suffix = stringResource(R.string.strength_sets_label),
                placeholder = "4",
                enabled = enabled,
                canDecrease = currentSets > 1,
                canIncrease = currentSets < 99,
                invalid = !setsValid,
                onValueChange = { raw ->
                    val next = sanitizeStrengthSets(raw)
                    setsText = next
                    parseStrengthSets(next)?.let { parsed ->
                        onUpdate(item.copy(sets = parsed))
                    }
                },
                onDecrease = {
                    val next = stepStrengthSets(currentSets, -1)
                    setsText = next.toString()
                    onUpdate(item.copy(sets = next))
                },
                onIncrease = {
                    val next = stepStrengthSets(currentSets, 1)
                    setsText = next.toString()
                    onUpdate(item.copy(sets = next))
                },
                modifier = Modifier.weight(1f),
            )
            BasicText(
                "×",
                style = TextStyle(n.textSecondary.asColor(), 14.sp, FontWeight.Medium),
            )
            CompactStrengthStepperField(
                value = repsText,
                suffix = stringResource(if (isTimed) R.string.strength_secs_label else R.string.strength_reps_label),
                placeholder = if (isTimed) "60" else "10",
                enabled = enabled,
                canDecrease = currentReps > minReps,
                canIncrease = currentReps < 999,
                invalid = !repsValid,
                onValueChange = { raw ->
                    val next = sanitizeStrengthReps(raw)
                    repsText = next
                    parseStrengthReps(next)?.let { parsed ->
                        onUpdate(item.copy(reps = parsed))
                    }
                },
                onDecrease = {
                    val next = stepStrengthReps(currentReps, -1, isTimed)
                    repsText = next.toString()
                    onUpdate(item.copy(reps = next))
                },
                onIncrease = {
                    val next = stepStrengthReps(currentReps, 1, isTimed)
                    repsText = next.toString()
                    onUpdate(item.copy(reps = next))
                },
                modifier = Modifier.weight(1f),
            )
        }

        val weightUnit = stringResource(R.string.strength_weight_unit)
        CompactStrengthField(
            value = weightText,
            suffix = if (weightText.isEmpty()) "" else weightUnit,
            placeholder = stringResource(R.string.strength_weight_optional),
            keyboardType = KeyboardType.Decimal,
            enabled = enabled,
            invalid = !weightValid,
            onValueChange = { raw ->
                val next = sanitizeStrengthWeight(raw)
                weightText = next
                if (next.isEmpty()) {
                    onUpdate(item.copy(weightKg = null))
                } else {
                    parseWeightKg(next)?.let { parsed ->
                        onUpdate(item.copy(weightKg = parsed))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            contentDescription = weightUnit,
        )
    }
}

@Composable
internal fun StrengthRowActionButtons(
    showReorder: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    enabled: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    removeContentDescription: String,
) {
    val haptics = LocalHapticFeedback.current
    val t = LocalThemeTokens.current
    val n = t.neutrals
    val isP5 = LocalUiStyle.current == UiStyle.PERSONA_5
    val p5 = LocalP5Tokens.current
    val p5Stroke = if (t.dark) p5.strokeLight.asColor() else p5.strokeDark.asColor()
    val btnShape = if (isP5) RoundedCornerShape(2.dp) else RoundedCornerShape(8.dp)
    val btnBorder = if (isP5) Modifier.border(1.dp, p5Stroke.copy(alpha = if (t.dark) 0.45f else 0.7f), btnShape) else Modifier
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (isP5) 4.dp else 2.dp),
    ) {
        if (showReorder) {
            val upEnabled = enabled && canMoveUp
            Box(
                Modifier
                    .size(28.dp)
                    .clip(btnShape)
                    .then(btnBorder)
                    .alpha(if (upEnabled) 1f else 0.3f)
                    .clickable(enabled = upEnabled, role = Role.Button) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onMoveUp()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.KeyboardArrowUp,
                    stringResource(R.string.strength_move_up),
                    Modifier.size(18.dp),
                    tint = n.textSecondary.asColor(),
                )
            }
            val downEnabled = enabled && canMoveDown
            Box(
                Modifier
                    .size(28.dp)
                    .clip(btnShape)
                    .then(btnBorder)
                    .alpha(if (downEnabled) 1f else 0.3f)
                    .clickable(enabled = downEnabled, role = Role.Button) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onMoveDown()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.KeyboardArrowDown,
                    stringResource(R.string.strength_move_down),
                    Modifier.size(18.dp),
                    tint = n.textSecondary.asColor(),
                )
            }
        }
        Box(
            Modifier
                .size(28.dp)
                .clip(btnShape)
                .then(btnBorder)
                .clickable(enabled = enabled, role = Role.Button, onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Close,
                removeContentDescription,
                Modifier.size(16.dp),
                tint = n.textSecondary.asColor(),
            )
        }
    }
}

@Composable
internal fun CompactStrengthStepperField(
    value: String,
    suffix: String,
    placeholder: String,
    enabled: Boolean,
    canDecrease: Boolean,
    canIncrease: Boolean,
    invalid: Boolean = false,
    onValueChange: (String) -> Unit,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = suffix,
) {
    val t = LocalThemeTokens.current
    val n = t.neutrals
    val a = t.accent
    val isP5 = LocalUiStyle.current == UiStyle.PERSONA_5
    val p5 = LocalP5Tokens.current
    val p5Stroke = if (t.dark) p5.strokeLight.asColor() else p5.strokeDark.asColor()
    val haptics = LocalHapticFeedback.current
    val shape = if (isP5) RoundedCornerShape(2.dp) else RoundedCornerShape(10.dp)
    val stepBtnShape = if (isP5) RoundedCornerShape(2.dp) else RoundedCornerShape(8.dp)
    val stepBtnMod = if (isP5) {
        Modifier
            .background(n.surface.asColor())
            .border(1.dp, p5Stroke.copy(alpha = if (t.dark) 0.4f else 0.65f), stepBtnShape)
    } else {
        Modifier
    }
    Row(
        modifier
            .heightIn(min = 38.dp)
            .clip(shape)
            .background(n.surfaceMuted.asColor())
            .then(
                when {
                    isP5 -> Modifier.border(
                        width = if (invalid) 2.dp else 1.5.dp,
                        color = if (invalid) p5.brandFill.asColor() else p5Stroke.copy(alpha = if (t.dark) 0.6f else 0.85f),
                        shape = shape,
                    )
                    invalid -> Modifier.border(1.dp, a.accentStroke.asColor(), shape)
                    else -> Modifier
                },
            )
            .padding(horizontal = 2.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val decEnabled = enabled && canDecrease
        Box(
            Modifier
                .size(width = 28.dp, height = 34.dp)
                .clip(stepBtnShape)
                .then(stepBtnMod)
                .alpha(if (decEnabled) 1f else 0.32f)
                .clickable(enabled = decEnabled, role = Role.Button) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDecrease()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Remove,
                stringResource(R.string.strength_decrease),
                Modifier.size(15.dp),
                tint = n.textPrimary.asColor(),
            )
        }
        Row(
            Modifier
                .weight(1f)
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
        ) {
            Box(Modifier.weight(1f, fill = false), contentAlignment = Alignment.Center) {
                if (value.isEmpty()) {
                    BasicText(
                        placeholder,
                        style = TextStyle(n.textSecondary.asColor().copy(alpha = 0.6f), 12.sp, textAlign = TextAlign.Center),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(n.textPrimary.asColor(), 14.sp, FontWeight.Medium, textAlign = TextAlign.Center),
                    cursorBrush = SolidColor(a.accentAction.asColor()),
                    modifier = Modifier.widthIn(min = 24.dp).semantics { this.contentDescription = contentDescription },
                )
            }
            if (suffix.isNotEmpty()) {
                BasicText(suffix, style = TextStyle(n.textSecondary.asColor(), 11.sp), maxLines = 1)
            }
        }
        val incEnabled = enabled && canIncrease
        Box(
            Modifier
                .size(width = 28.dp, height = 34.dp)
                .clip(stepBtnShape)
                .then(stepBtnMod)
                .alpha(if (incEnabled) 1f else 0.32f)
                .clickable(enabled = incEnabled, role = Role.Button) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onIncrease()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Add,
                stringResource(R.string.strength_increase),
                Modifier.size(15.dp),
                tint = n.textPrimary.asColor(),
            )
        }
    }
}

@Composable
internal fun CompactStrengthField(
    value: String,
    suffix: String,
    placeholder: String,
    keyboardType: KeyboardType,
    enabled: Boolean,
    invalid: Boolean = false,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = suffix,
) {
    val t = LocalThemeTokens.current
    val n = t.neutrals
    val a = t.accent
    val isP5 = LocalUiStyle.current == UiStyle.PERSONA_5
    val p5 = LocalP5Tokens.current
    val p5Stroke = if (t.dark) p5.strokeLight.asColor() else p5.strokeDark.asColor()
    val shape = if (isP5) RoundedCornerShape(2.dp) else RoundedCornerShape(10.dp)
    Row(
        modifier
            .heightIn(min = 38.dp)
            .clip(shape)
            .background(n.surfaceMuted.asColor())
            .then(
                when {
                    isP5 -> Modifier.border(
                        width = if (invalid) 2.dp else 1.5.dp,
                        color = if (invalid) p5.brandFill.asColor() else p5Stroke.copy(alpha = if (t.dark) 0.6f else 0.85f),
                        shape = shape,
                    )
                    invalid -> Modifier.border(1.dp, a.accentStroke.asColor(), shape)
                    else -> Modifier
                },
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                BasicText(
                    placeholder,
                    style = TextStyle(n.textSecondary.asColor().copy(alpha = 0.6f), 12.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                textStyle = TextStyle(n.textPrimary.asColor(), 14.sp, FontWeight.Medium),
                cursorBrush = SolidColor(a.accentAction.asColor()),
                modifier = Modifier.fillMaxWidth().semantics { this.contentDescription = contentDescription },
            )
        }
        if (suffix.isNotEmpty()) {
            BasicText(suffix, style = TextStyle(n.textSecondary.asColor(), 11.sp), maxLines = 1)
        }
    }
}

@Composable
internal fun SaveAsPlanDialog(
    visible: Boolean,
    visibility: MutableTransitionState<Boolean> = rememberOverlayVisibility(visible),
    onSave: suspend (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val t = LocalThemeTokens.current
    val n = t.neutrals
    val a = t.accent
    val isP5 = LocalUiStyle.current == UiStyle.PERSONA_5
    val p5 = LocalP5Tokens.current
    val p5Stroke = if (t.dark) p5.strokeLight.asColor() else p5.strokeDark.asColor()
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val title = stringResource(R.string.strength_plan_save_current)
    val hint = stringResource(R.string.strength_plan_name_hint)
    var name by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            name = ""
            saving = false
            failed = false
        }
    }

    val canSave = !saving && name.trim().isNotEmpty()
    GlassOverlayPanel(
        visible = visible,
        onDismiss = {
            if (!saving) {
                focus.clearFocus()
                onDismiss()
            }
        },
        contentPadding = PaddingValues(start = 24.dp, top = 22.dp, end = 20.dp, bottom = 12.dp),
        title = title,
        visibility = visibility,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BasicText(
                title,
                Modifier.semantics { heading() },
                style = TextStyle(n.textPrimary.asColor(), 17.sp, if (isP5) FontWeight.Black else FontWeight.SemiBold),
            )
            val inputShape = if (isP5) RoundedCornerShape(2.dp) else RoundedCornerShape(12.dp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clip(inputShape)
                    .background(n.surfaceMuted.asColor())
                    .then(if (isP5) Modifier.border(2.dp, p5Stroke, inputShape) else Modifier)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (name.isEmpty()) {
                    BasicText(hint, style = TextStyle(n.textSecondary.asColor(), 15.sp))
                }
                BasicTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        failed = false
                    },
                    enabled = !saving,
                    singleLine = true,
                    textStyle = TextStyle(n.textPrimary.asColor(), 15.sp),
                    cursorBrush = SolidColor(a.accentAction.asColor()),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = hint },
                )
            }
            if (failed) {
                BasicText(
                    stringResource(R.string.record_save_failed),
                    Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    style = TextStyle(a.accentStroke.asColor(), 13.sp),
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = {
                        if (!saving) {
                            focus.clearFocus()
                            onDismiss()
                        }
                    },
                    enabled = !saving,
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = n.textSecondary.asColor()),
                ) {
                    Text(stringResource(R.string.record_cancel), fontWeight = if (isP5) FontWeight.Bold else FontWeight.Normal)
                }
                val performSave = {
                    if (canSave) {
                        focus.clearFocus()
                        saving = true
                        failed = false
                        val trimmed = name.trim()
                        scope.launch {
                            try {
                                onSave(trimmed)
                                onDismiss()
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                failed = true
                            } finally {
                                saving = false
                            }
                        }
                    }
                }
                if (isP5) {
                    Box(
                        Modifier
                            .heightIn(min = 48.dp)
                            .alpha(if (canSave) 1f else 0.45f)
                            .p5Surface(
                                variant = P5ShapeVariant.BADGE,
                                fillColor = p5.brandFill.asColor(),
                                strokeColor = p5Stroke,
                                shadowColor = p5.strokeDark.asColor(),
                                strokeWidth = 1.5.dp,
                                shadowOffset = DpOffset(2.dp, 2.dp),
                            )
                            .clickable(enabled = canSave, role = Role.Button, onClick = performSave)
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(R.string.record_save),
                            color = p5.onBrandFill.asColor(),
                            fontWeight = FontWeight.Black,
                        )
                    }
                } else {
                    TextButton(
                        onClick = performSave,
                        enabled = canSave,
                        modifier = Modifier.heightIn(min = 48.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = a.accentAction.asColor()),
                    ) {
                        Text(stringResource(R.string.record_save))
                    }
                }
            }
        }
    }
}
