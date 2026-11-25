package com.actiary.ui.settings

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.actiary.R
import com.actiary.data.PlanItemInput
import com.actiary.data.StrengthRepository
import com.actiary.data.db.StrengthExerciseEntity
import com.actiary.data.db.WorkoutPlanWithItems
import com.actiary.domain.formatWeightKg
import com.actiary.domain.parseWeightKg
import com.actiary.glass.GlassDialog
import com.actiary.glass.GlassSheet
import com.actiary.glass.overlayPresent
import com.actiary.glass.rememberOverlayVisibility
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor
import com.actiary.ui.common.exerciseDisplayName
import com.actiary.ui.common.isChineseLocale
import com.actiary.ui.common.workoutPlanLabel
import com.actiary.ui.record.CustomExerciseDialog
import com.actiary.ui.record.ExerciseHistorySheet
import com.actiary.ui.record.ExercisePickerSheet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Stable
internal class WorkoutPlansOverlayState {
    var editingPlanOpen by mutableStateOf(false)
    var editingPlan by mutableStateOf<WorkoutPlanWithItems?>(null)
    var pendingPlanDelete by mutableStateOf<WorkoutPlanWithItems?>(null)
    var pickerOpen by mutableStateOf(false)
    var pickerPresent by mutableStateOf(false)
    var pickedBatch by mutableStateOf<Pair<Int, List<StrengthExerciseEntity>>?>(null)
    var customExerciseOpen by mutableStateOf(false)
    var editingExercise by mutableStateOf<StrengthExerciseEntity?>(null)
    var historyExercise by mutableStateOf<StrengthExerciseEntity?>(null)
    var historyOpen by mutableStateOf(false)
    var historyPresent by mutableStateOf(false)
    private var pickSeq = 0

    fun hasVisibleOverlay(
        deleteVisibility: MutableTransitionState<Boolean>,
        customExerciseVisibility: MutableTransitionState<Boolean>,
    ): Boolean =
        editingPlanOpen ||
            pendingPlanDelete != null ||
            pickerOpen ||
            pickerPresent ||
            customExerciseOpen ||
            historyOpen ||
            historyPresent ||
            deleteVisibility.overlayPresent ||
            customExerciseVisibility.overlayPresent

    fun openNewPlan() {
        editingPlan = null
        pickedBatch = null
        editingPlanOpen = true
    }

    fun openEditPlan(plan: WorkoutPlanWithItems) {
        editingPlan = plan
        pickedBatch = null
        editingPlanOpen = true
    }

    fun closePlanEditor() {
        editingPlanOpen = false
        editingPlan = null
        pickerOpen = false
        pickerPresent = false
        pickedBatch = null
    }

    fun requestDeletePlan(plan: WorkoutPlanWithItems) {
        pendingPlanDelete = plan
    }

    fun dismissDeletePlan() {
        pendingPlanDelete = null
    }

    fun openPicker() {
        pickerOpen = true
        pickerPresent = true
    }

    fun closePicker() {
        pickerOpen = false
    }

    fun onPickerExited() {
        if (!pickerOpen) pickerPresent = false
    }

    fun appendPickedExercises(exercises: List<StrengthExerciseEntity>) {
        pickedBatch = (++pickSeq) to exercises
        pickerOpen = false
    }

    fun consumePickedBatch() {
        pickedBatch = null
    }

    fun openNewExercise() {
        editingExercise = null
        customExerciseOpen = true
    }

    fun openEditExercise(exercise: StrengthExerciseEntity) {
        editingExercise = exercise
        customExerciseOpen = true
    }

    fun closeExerciseDialog() {
        customExerciseOpen = false
        editingExercise = null
    }

    fun openExerciseHistory(exercise: StrengthExerciseEntity) {
        historyExercise = exercise
        historyOpen = true
        historyPresent = true
    }

    fun closeExerciseHistory() {
        historyOpen = false
    }

    fun onExerciseHistoryExited() {
        if (!historyOpen) {
            historyPresent = false
            historyExercise = null
        }
    }
}

@Composable
internal fun WorkoutPlansOverlays(
    repository: StrengthRepository,
    state: WorkoutPlansOverlayState,
    deleteVisibility: MutableTransitionState<Boolean> = rememberOverlayVisibility(state.pendingPlanDelete != null),
    customExerciseVisibility: MutableTransitionState<Boolean> = rememberOverlayVisibility(state.customExerciseOpen),
) {
    val scope = rememberCoroutineScope()
    val activeExercises by repository.activeExercises.collectAsStateWithLifecycle(emptyList())
    val allExercises by repository.allExercises.collectAsStateWithLifecycle(emptyList())

    if (state.editingPlanOpen) {
        PlanEditorSheet(
            editingPlan = state.editingPlan,
            allExercises = allExercises,
            repository = repository,
            covered = state.pickerOpen || state.pickerPresent,
            pickedBatch = state.pickedBatch,
            onConsumePickedBatch = state::consumePickedBatch,
            onRequestPickExercises = state::openPicker,
            onDismiss = state::closePlanEditor,
        )
    }

    ExercisePickerSheet(
        visible = state.pickerOpen,
        exercises = activeExercises,
        onCreateCustomExercise = { name, region, primaryMask, secondaryMask, defaultSets, defaultReps, isTimed ->
            repository.addCustomExercise(
                name = name,
                region = region,
                primaryMuscleMask = primaryMask,
                secondaryMuscleMask = secondaryMask,
                defaultSets = defaultSets,
                defaultReps = defaultReps,
                isTimed = isTimed,
            )
        },
        onConfirm = state::appendPickedExercises,
        onDismiss = state::closePicker,
        onExited = state::onPickerExited,
    )

    CustomExerciseDialog(
        visible = state.customExerciseOpen,
        editingExercise = state.editingExercise,
        visibility = customExerciseVisibility,
        onSave = { name, region, primaryMask, secondaryMask, defaultSets, defaultReps, isTimed ->
            val target = state.editingExercise
            if (target == null) {
                repository.addCustomExercise(
                    name = name,
                    region = region,
                    primaryMuscleMask = primaryMask,
                    secondaryMuscleMask = secondaryMask,
                    defaultSets = defaultSets,
                    defaultReps = defaultReps,
                    isTimed = isTimed,
                )
            } else {
                repository.updateCustomExercise(
                    id = target.id,
                    name = name,
                    region = region,
                    primaryMuscleMask = primaryMask,
                    secondaryMuscleMask = secondaryMask,
                    defaultSets = defaultSets,
                    defaultReps = defaultReps,
                    isTimed = isTimed,
                )
            }
            state.closeExerciseDialog()
        },
        onDismiss = state::closeExerciseDialog,
    )

    val pendingPlan = state.pendingPlanDelete
    val pendingName = pendingPlan?.let { workoutPlanLabel(it.plan) }.orEmpty()
    GlassDialog(
        visible = pendingPlan != null,
        text = stringResource(R.string.strength_plan_delete_confirm, pendingName),
        confirm = stringResource(R.string.custom_type_delete),
        dismiss = stringResource(R.string.record_cancel),
        onConfirm = {
            val target = state.pendingPlanDelete
            state.dismissDeletePlan()
            if (target != null) {
                scope.launch { repository.deletePlan(target.plan.id) }
            }
        },
        onDismiss = state::dismissDeletePlan,
        visibility = deleteVisibility,
    )

    ExerciseHistorySheet(
        visible = state.historyOpen && state.historyExercise != null,
        exercise = state.historyExercise,
        strengthRepository = repository,
        onDismiss = state::closeExerciseHistory,
        onExited = state::onExerciseHistoryExited,
    )
}

private data class DraftPlanItem(
    val key: Long,
    val exerciseId: Long,
    val setsText: String,
    val repsText: String,
    val weightText: String,
)

@Composable
private fun PlanEditorSheet(
    editingPlan: WorkoutPlanWithItems?,
    allExercises: List<StrengthExerciseEntity>,
    repository: StrengthRepository,
    covered: Boolean,
    pickedBatch: Pair<Int, List<StrengthExerciseEntity>>?,
    onConsumePickedBatch: () -> Unit,
    onRequestPickExercises: () -> Unit,
    onDismiss: () -> Unit,
) {
    val t = LocalThemeTokens.current
    val n = t.neutrals
    val a = t.accent
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val isChinese = isChineseLocale()
    val title = stringResource(
        if (editingPlan == null) R.string.strength_plan_new else R.string.strength_plan_edit,
    )
    val initialName = editingPlan?.let { workoutPlanLabel(it.plan) }.orEmpty()
    val builtinDefaultName = editingPlan?.plan?.takeIf { it.builtinKey != null }?.let {
        workoutPlanLabel(it.copy(name = null))
    }
    val exerciseMap = remember(allExercises) { allExercises.associateBy { it.id } }

    var visible by remember { mutableStateOf(true) }
    var planName by remember(editingPlan?.plan?.id) { mutableStateOf(initialName) }
    var nextKey by remember(editingPlan?.plan?.id) { mutableLongStateOf(0L) }
    val draftItems = remember(editingPlan?.plan?.id) {
        mutableStateListOf<DraftPlanItem>().apply {
            val initial = editingPlan?.items?.sortedBy { it.sortOrder }.orEmpty()
            initial.forEachIndexed { index, item ->
                add(
                    DraftPlanItem(
                        key = index.toLong(),
                        exerciseId = item.exerciseId,
                        setsText = item.defaultSets.toString(),
                        repsText = item.defaultReps.toString(),
                        weightText = item.defaultWeightKg?.let(::formatWeightKg).orEmpty(),
                    ),
                )
            }
            nextKey = initial.size.toLong()
        }
    }
    var saving by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }

    LaunchedEffect(pickedBatch?.first) {
        val batch = pickedBatch ?: return@LaunchedEffect
        batch.second.forEach { ex ->
            draftItems.add(
                DraftPlanItem(
                    key = nextKey++,
                    exerciseId = ex.id,
                    setsText = ex.defaultSets.toString(),
                    repsText = ex.defaultReps.toString(),
                    weightText = "",
                ),
            )
        }
        onConsumePickedBatch()
    }

    fun close(): Boolean {
        if (saving) return false
        focus.clearFocus()
        visible = false
        return true
    }

    val savedName = planName.trim().takeUnless { builtinDefaultName != null && (it.isEmpty() || it == builtinDefaultName) }
    val canSave = !saving && (planName.trim().isNotEmpty() || builtinDefaultName != null)

    GlassSheet(
        visible = visible,
        onRequestClose = ::close,
        onExited = onDismiss,
        title = title,
        covered = covered,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    title,
                    Modifier.weight(1f).semantics { heading() },
                    style = TextStyle(n.textPrimary.asColor(), 20.sp, FontWeight.SemiBold),
                )
                TextButton(
                    onClick = { close() },
                    enabled = !saving,
                    colors = ButtonDefaults.textButtonColors(contentColor = n.textSecondary.asColor()),
                ) {
                    Text(stringResource(R.string.record_cancel))
                }
            }

            val nameHint = stringResource(R.string.strength_plan_name_hint)
            val fieldShape = RoundedCornerShape(12.dp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(fieldShape)
                    .background(n.surfaceMuted.asColor())
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (planName.isEmpty()) {
                    BasicText(nameHint, style = TextStyle(n.textSecondary.asColor(), 16.sp))
                }
                BasicTextField(
                    value = planName,
                    onValueChange = {
                        planName = it
                        saveFailed = false
                    },
                    enabled = !saving,
                    singleLine = true,
                    textStyle = TextStyle(n.textPrimary.asColor(), 16.sp),
                    cursorBrush = SolidColor(a.accentAction.asColor()),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = nameHint },
                )
            }

            draftItems.forEachIndexed { index, item ->
                key(item.key) {
                    val exercise = exerciseMap[item.exerciseId]
                    val name = exercise?.let { exerciseDisplayName(it, isChinese) }.orEmpty()
                    DraftPlanItemRow(
                        name = name,
                        isTimed = exercise?.isTimed == true,
                        item = item,
                        canMoveUp = index > 0 && !saving,
                        canMoveDown = index < draftItems.lastIndex && !saving,
                        enabled = !saving,
                        onUpdate = { updated -> draftItems[index] = updated },
                        onMoveUp = {
                            val removed = draftItems.removeAt(index)
                            draftItems.add(index - 1, removed)
                        },
                        onMoveDown = {
                            val removed = draftItems.removeAt(index)
                            draftItems.add(index + 1, removed)
                        },
                        onRemove = { draftItems.removeAt(index) },
                    )
                }
            }

            val btnShape = RoundedCornerShape(12.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(btnShape)
                    .background(n.surfaceMuted.asColor())
                    .clickable(enabled = !saving, role = Role.Button) {
                        focus.clearFocus()
                        onRequestPickExercises()
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                Icon(Icons.Outlined.Add, null, Modifier.size(18.dp), tint = a.accentAction.asColor())
                BasicText(
                    stringResource(R.string.strength_add_exercise),
                    style = TextStyle(a.accentAction.asColor(), 15.sp, FontWeight.Medium),
                )
            }

            if (saveFailed) {
                BasicText(
                    stringResource(R.string.record_save_failed),
                    Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    style = TextStyle(n.textPrimary.asColor(), 14.sp),
                )
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(btnShape)
                    .background(if (canSave) a.accentAction.asColor() else n.surfaceMuted.asColor())
                    .clickable(enabled = canSave, role = Role.Button) {
                        focus.clearFocus()
                        saving = true
                        saveFailed = false
                        val inputs = draftItems.map { item ->
                            val fallbackReps = if (exerciseMap[item.exerciseId]?.isTimed == true) 60 else 10
                            PlanItemInput(
                                exerciseId = item.exerciseId,
                                defaultSets = (item.setsText.toIntOrNull() ?: 4).coerceIn(1, 99),
                                defaultReps = (item.repsText.toIntOrNull() ?: fallbackReps).coerceIn(1, 999),
                                defaultWeightKg = parseWeightKg(item.weightText),
                            )
                        }
                        scope.launch {
                            try {
                                if (editingPlan == null) {
                                    repository.createPlan(planName, inputs)
                                } else {
                                    repository.updatePlan(editingPlan.plan.id, savedName, inputs)
                                }
                                saving = false
                                visible = false
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                saving = false
                                saveFailed = true
                            }
                        }
                    }
                    .padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    stringResource(R.string.record_save),
                    style = TextStyle(
                        color = if (canSave) a.onAccentAction.asColor() else n.textSecondary.asColor(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
    }
}

@Composable
private fun DraftPlanItemRow(
    name: String,
    isTimed: Boolean,
    item: DraftPlanItem,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    enabled: Boolean,
    onUpdate: (DraftPlanItem) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    val t = LocalThemeTokens.current
    val n = t.neutrals
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(n.surface.asColor())
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            BasicText(
                name,
                Modifier.weight(1f),
                style = TextStyle(n.textPrimary.asColor(), 15.sp, FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (canMoveUp) {
                SmallRowIconButton(
                    icon = Icons.Outlined.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.custom_type_move_up, name),
                    enabled = enabled,
                    onClick = onMoveUp,
                )
            }
            if (canMoveDown) {
                SmallRowIconButton(
                    icon = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.custom_type_move_down, name),
                    enabled = enabled,
                    onClick = onMoveDown,
                )
            }
            SmallRowIconButton(
                icon = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.custom_type_delete_named, name),
                enabled = enabled,
                onClick = onRemove,
            )
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PlanMetricField(
                value = item.setsText,
                suffix = stringResource(R.string.strength_sets_label),
                placeholder = "4",
                keyboardType = KeyboardType.Number,
                enabled = enabled,
                onValueChange = { raw -> onUpdate(item.copy(setsText = raw.filter(Char::isDigit).take(2))) },
                modifier = Modifier.weight(1f),
            )
            PlanMetricField(
                value = item.repsText,
                suffix = stringResource(if (isTimed) R.string.strength_secs_label else R.string.strength_reps_label),
                placeholder = if (isTimed) "60" else "10",
                keyboardType = KeyboardType.Number,
                enabled = enabled,
                onValueChange = { raw -> onUpdate(item.copy(repsText = raw.filter(Char::isDigit).take(3))) },
                modifier = Modifier.weight(1f),
            )
            val weightUnit = stringResource(R.string.strength_weight_unit)
            PlanMetricField(
                value = item.weightText,
                suffix = if (item.weightText.isEmpty()) "" else weightUnit,
                placeholder = stringResource(R.string.strength_weight_optional),
                keyboardType = KeyboardType.Decimal,
                enabled = enabled,
                onValueChange = { raw -> onUpdate(item.copy(weightText = filterDecimalWeight(raw))) },
                modifier = Modifier.weight(1.3f),
                contentDescription = weightUnit,
            )
        }
    }
}

@Composable
private fun SmallRowIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val n = LocalThemeTokens.current.neutrals
    Box(
        Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, Modifier.size(18.dp), tint = n.textSecondary.asColor())
    }
}

@Composable
private fun PlanMetricField(
    value: String,
    suffix: String,
    placeholder: String,
    keyboardType: KeyboardType,
    enabled: Boolean,
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
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                BasicText(
                    placeholder,
                    style = TextStyle(n.textSecondary.asColor().copy(alpha = 0.6f), 13.sp),
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
            BasicText(suffix, style = TextStyle(n.textSecondary.asColor(), 12.sp))
        }
    }
}

private fun filterDecimalWeight(raw: String): String {
    val sb = StringBuilder()
    var hasDot = false
    var decimals = 0
    for (ch in raw) {
        if (ch.isDigit()) {
            if (hasDot) {
                if (decimals >= 1) continue
                decimals++
            }
            sb.append(ch)
        } else if (ch == '.' && !hasDot && sb.isNotEmpty()) {
            hasDot = true
            sb.append(ch)
        }
        if (sb.length >= 6) break
    }
    return sb.toString()
}
