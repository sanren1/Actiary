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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.actiary.R
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
import com.actiary.theme.LocalThemeTokens
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
    onValidityChange: (Boolean) -> Unit,
) {
    val t = LocalThemeTokens.current
    val n = t.neutrals
    val a = t.accent
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

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(n.surface.asColor())
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
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                entry.strengthItems.forEach { item ->
                    key(planResetToken, item.localId) {
                        val exercise = exercisesById[item.exerciseId]
                        StrengthItemRow(
                            item = item,
                            exercise = exercise,
                            lastPerf = lastPerformanceByExercise[item.exerciseId],
                            isChinese = isChinese,
                            enabled = enabled,
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
                    .clip(btnShape)
                    .background(n.surfaceMuted.asColor())
                    .clickable(enabled = enabled, role = Role.Button) { onOpenPicker(null) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            ) {
                Icon(Icons.Outlined.Add, null, Modifier.size(18.dp), tint = a.accentAction.asColor())
                BasicText(
                    stringResource(R.string.strength_add_exercise),
                    style = TextStyle(a.accentAction.asColor(), 14.sp, FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (entry.strengthItems.isNotEmpty()) {
                Box(
                    Modifier
                        .heightIn(min = 42.dp)
                        .clip(btnShape)
                        .background(n.surfaceMuted.asColor())
                        .clickable(enabled = enabled, role = Role.Button, onClick = onSaveAsPlan)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        stringResource(R.string.strength_plan_save_current),
                        style = TextStyle(n.textPrimary.asColor(), 14.sp, FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(
                    Modifier
                        .heightIn(min = 42.dp)
                        .clip(btnShape)
                        .background(n.surfaceMuted.asColor())
                        .clickable(enabled = enabled, role = Role.Button, onClick = onClearAll)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        stringResource(R.string.strength_clear_all),
                        style = TextStyle(n.textSecondary.asColor(), 14.sp, FontWeight.Medium),
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
                        .clip(pillShape)
                        .background(if (isSelected) a.accentSubtle.asColor() else n.surfaceMuted.asColor())
                        .then(
                            if (isSelected) Modifier.border(1.dp, a.accentStroke.asColor(), pillShape)
                            else Modifier
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
                            tint = a.accentAction.asColor(),
                        )
                    }
                    BasicText(
                        workoutPlanLabel(plan.plan),
                        style = TextStyle(
                            color = if (isSelected) a.accentAction.asColor() else n.textPrimary.asColor(),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        ),
                        maxLines = 1,
                    )
                    if (isRecommended) {
                        Box(
                            Modifier
                                .clip(pillShape)
                                .background(a.accentAction.asColor().copy(alpha = 0.14f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            BasicText(
                                stringResource(R.string.strength_plan_next_badge),
                                style = TextStyle(a.accentAction.asColor(), 10.sp, FontWeight.SemiBold),
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
                .clip(pillShape)
                .background(n.surfaceMuted.asColor())
                .clickable(enabled = enabled, role = Role.Button, onClick = onManagePlans)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                stringResource(R.string.strength_plan_manage),
                style = TextStyle(n.textSecondary.asColor(), 12.sp, FontWeight.Medium),
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
    onUpdate: (DraftStrengthItem) -> Unit,
    onRemove: () -> Unit,
    onValidityChange: (Boolean) -> Unit,
) {
    val t = LocalThemeTokens.current
    val n = t.neutrals
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

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(n.surface.asColor())
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText(
                    name,
                    style = TextStyle(n.textPrimary.asColor(), 14.sp, FontWeight.Medium),
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
                    BasicText(
                        stringResource(R.string.strength_last_perf, formattedLast),
                        style = TextStyle(n.textSecondary.asColor(), 11.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = enabled, role = Role.Button, onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Close,
                    stringResource(R.string.record_remove_entry),
                    Modifier.size(16.dp),
                    tint = n.textSecondary.asColor(),
                )
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CompactStrengthField(
                value = setsText,
                suffix = stringResource(R.string.strength_sets_label),
                placeholder = "4",
                keyboardType = KeyboardType.Number,
                enabled = enabled,
                invalid = !setsValid,
                onValueChange = { raw ->
                    val next = sanitizeStrengthSets(raw)
                    setsText = next
                    parseStrengthSets(next)?.let { parsed ->
                        onUpdate(item.copy(sets = parsed))
                    }
                },
                modifier = Modifier.weight(0.85f),
            )
            BasicText(
                "×",
                style = TextStyle(n.textSecondary.asColor(), 14.sp, FontWeight.Medium),
            )
            CompactStrengthField(
                value = repsText,
                suffix = stringResource(if (isTimed) R.string.strength_secs_label else R.string.strength_reps_label),
                placeholder = if (isTimed) "60" else "10",
                keyboardType = KeyboardType.Number,
                enabled = enabled,
                invalid = !repsValid,
                onValueChange = { raw ->
                    val next = sanitizeStrengthReps(raw)
                    repsText = next
                    parseStrengthReps(next)?.let { parsed ->
                        onUpdate(item.copy(reps = parsed))
                    }
                },
                modifier = Modifier.weight(0.85f),
            )
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
                modifier = Modifier.weight(1.3f),
                contentDescription = weightUnit,
            )
        }
    }
}

@Composable
private fun CompactStrengthField(
    value: String,
    suffix: String,
    placeholder: String,
    keyboardType: KeyboardType,
    enabled: Boolean,
    invalid: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = suffix,
) {
    val t = LocalThemeTokens.current
    val n = t.neutrals
    val a = t.accent
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier
            .heightIn(min = 38.dp)
            .clip(shape)
            .background(n.surfaceMuted.asColor())
            .then(if (invalid) Modifier.border(1.dp, a.accentStroke.asColor(), shape) else Modifier)
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
                style = TextStyle(n.textPrimary.asColor(), 17.sp, FontWeight.SemiBold),
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(n.surfaceMuted.asColor())
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
                horizontalArrangement = Arrangement.End,
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
                    Text(stringResource(R.string.record_cancel))
                }
                TextButton(
                    onClick = {
                        if (!canSave) return@TextButton
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
                    },
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
