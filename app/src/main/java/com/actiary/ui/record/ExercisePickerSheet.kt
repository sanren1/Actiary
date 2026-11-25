package com.actiary.ui.record

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.actiary.R
import com.actiary.data.db.StrengthExerciseEntity
import com.actiary.domain.BodyRegion
import com.actiary.domain.MuscleGroup
import com.actiary.domain.muscleMaskOf
import com.actiary.domain.musclesFromMask
import com.actiary.glass.GlassIconButton
import com.actiary.glass.GlassOverlayPanel
import com.actiary.glass.GlassSheet
import com.actiary.glass.overlayPresent
import com.actiary.glass.rememberOverlayVisibility
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor
import com.actiary.ui.common.exerciseDisplayName
import com.actiary.ui.common.isChineseLocale
import com.actiary.ui.common.labelRes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun ExercisePickerSheet(
    visible: Boolean,
    exercises: List<StrengthExerciseEntity>,
    initialMuscleFilter: MuscleGroup? = null,
    onCreateCustomExercise: suspend (
        name: String,
        region: BodyRegion,
        primaryMask: Int,
        secondaryMask: Int,
        defaultSets: Int,
        defaultReps: Int,
        isTimed: Boolean,
    ) -> Long,
    onConfirm: (List<StrengthExerciseEntity>) -> Unit,
    onDismiss: () -> Unit,
    onExited: () -> Unit = {},
) {
    val t = LocalThemeTokens.current
    val n = t.neutrals
    val a = t.accent
    val focus = LocalFocusManager.current
    val isChinese = isChineseLocale()
    val title = stringResource(R.string.strength_picker_title)

    var query by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf<BodyRegion?>(null) }
    var activeMuscleFilter by remember { mutableStateOf<MuscleGroup?>(null) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showCustomDialog by remember { mutableStateOf(false) }
    val customDialogVisibility = rememberOverlayVisibility(showCustomDialog)

    LaunchedEffect(visible, initialMuscleFilter) {
        if (visible) {
            query = ""
            selectedRegion = null
            activeMuscleFilter = initialMuscleFilter
            selectedIds = emptySet()
            showCustomDialog = false
        }
    }

    val filtered = remember(exercises, query, selectedRegion, activeMuscleFilter) {
        val trimmed = query.trim()
        exercises.filter { ex ->
            val matchesRegion = selectedRegion == null || ex.bodyRegion == selectedRegion?.key
            val matchesMuscle = activeMuscleFilter == null ||
                (ex.primaryMuscleMask and (1 shl activeMuscleFilter!!.bit)) != 0
            val matchesQuery = trimmed.isEmpty() ||
                ex.nameZh.contains(trimmed, ignoreCase = true) ||
                ex.nameEn.contains(trimmed, ignoreCase = true)
            matchesRegion && matchesMuscle && matchesQuery
        }
    }

    fun requestClose(): Boolean {
        focus.clearFocus()
        onDismiss()
        return true
    }

    GlassSheet(
        visible = visible,
        onRequestClose = ::requestClose,
        onExited = onExited,
        title = title,
        covered = customDialogVisibility.overlayPresent,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    title,
                    Modifier.weight(1f).semantics { heading() },
                    style = TextStyle(n.textPrimary.asColor(), 20.sp, FontWeight.SemiBold),
                )
                GlassIconButton(
                    icon = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.overlay_close),
                    onClick = { requestClose() },
                    glass = false,
                )
            }

            // Search input (solid surfaceMuted)
            val searchPlaceholder = stringResource(R.string.strength_picker_search)
            val fieldShape = RoundedCornerShape(12.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clip(fieldShape)
                    .background(n.surfaceMuted.asColor())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Outlined.Search, null, Modifier.size(20.dp), tint = n.textSecondary.asColor())
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        BasicText(
                            searchPlaceholder,
                            style = TextStyle(n.textSecondary.asColor(), 15.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(n.textPrimary.asColor(), 15.sp),
                        cursorBrush = SolidColor(a.accentAction.asColor()),
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = searchPlaceholder },
                    )
                }
                if (query.isNotEmpty()) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(role = Role.Button) { query = "" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            stringResource(R.string.overlay_close),
                            Modifier.size(16.dp),
                            tint = n.textSecondary.asColor(),
                        )
                    }
                }
            }

            // Horizontal 6-region filter chips
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PickerFilterChip(
                    label = stringResource(R.string.region_all),
                    selected = selectedRegion == null,
                    onClick = { selectedRegion = null },
                )
                BodyRegion.entries.forEach { region ->
                    PickerFilterChip(
                        label = stringResource(region.labelRes()),
                        selected = selectedRegion == region,
                        onClick = { selectedRegion = if (selectedRegion == region) null else region },
                    )
                }
            }

            // Active muscle filter dismissible chip
            activeMuscleFilter?.let { muscle ->
                val chipShape = RoundedCornerShape(50)
                val clearLabel = stringResource(R.string.strength_picker_clear_muscle)
                Row(
                    Modifier
                        .clip(chipShape)
                        .background(a.accentSubtle.asColor())
                        .border(1.dp, a.accentStroke.asColor(), chipShape)
                        .clickable(role = Role.Button, onClickLabel = clearLabel) { activeMuscleFilter = null }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    BasicText(
                        stringResource(muscle.labelRes()),
                        style = TextStyle(a.accentAction.asColor(), 13.sp, FontWeight.Medium),
                    )
                    Icon(
                        Icons.Outlined.Close,
                        clearLabel,
                        Modifier.size(16.dp),
                        tint = a.accentAction.asColor(),
                    )
                }
            }

            // Multi-select exercise list
            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(min = 160.dp, max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp),
            ) {
                items(filtered, key = { it.id }) { exercise ->
                    val isSelected = exercise.id in selectedIds
                    val primaryMuscles = musclesFromMask(exercise.primaryMuscleMask)
                    val muscleText = primaryMuscles.map { stringResource(it.labelRes()) }.joinToString(" · ")
                    val rowShape = RoundedCornerShape(12.dp)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(rowShape)
                            .background(if (isSelected) a.accentSubtle.asColor() else n.surface.asColor())
                            .then(
                                if (isSelected) Modifier.border(1.dp, a.accentStroke.asColor(), rowShape)
                                else Modifier
                            )
                            .clickable(role = Role.Checkbox) {
                                selectedIds = if (isSelected) selectedIds - exercise.id else selectedIds + exercise.id
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            BasicText(
                                exerciseDisplayName(exercise, isChinese),
                                style = TextStyle(n.textPrimary.asColor(), 15.sp, FontWeight.Medium),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (muscleText.isNotEmpty()) {
                                BasicText(
                                    muscleText,
                                    style = TextStyle(n.textSecondary.asColor(), 12.sp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isSelected) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = if (isSelected) a.accentAction.asColor() else n.textSecondary.asColor(),
                        )
                    }
                }
            }

            // Bottom action bar: [+ Custom exercise] + [Add (N)]
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val btnShape = RoundedCornerShape(12.dp)
                Row(
                    Modifier
                        .heightIn(min = 48.dp)
                        .clip(btnShape)
                        .background(n.surfaceMuted.asColor())
                        .clickable(role = Role.Button) {
                            focus.clearFocus()
                            showCustomDialog = true
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Outlined.Add, null, Modifier.size(18.dp), tint = n.textPrimary.asColor())
                    BasicText(
                        stringResource(R.string.strength_custom_exercise_new),
                        style = TextStyle(n.textPrimary.asColor(), 14.sp, FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                val canConfirm = selectedIds.isNotEmpty()
                Box(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(btnShape)
                        .background(if (canConfirm) a.accentAction.asColor() else n.surfaceMuted.asColor())
                        .clickable(enabled = canConfirm, role = Role.Button) {
                            focus.clearFocus()
                            val chosen = exercises.filter { it.id in selectedIds }
                            onConfirm(chosen)
                            onDismiss()
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        stringResource(R.string.strength_picker_confirm, selectedIds.size),
                        style = TextStyle(
                            color = if (canConfirm) a.onAccentAction.asColor() else n.textSecondary.asColor(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    CustomExerciseDialog(
        visible = showCustomDialog,
        editingExercise = null,
        visibility = customDialogVisibility,
        onSave = { name, region, primaryMask, secondaryMask, defaultSets, defaultReps, isTimed ->
            val newId = onCreateCustomExercise(
                name, region, primaryMask, secondaryMask, defaultSets, defaultReps, isTimed,
            )
            selectedIds = selectedIds + newId
            showCustomDialog = false
        },
        onDismiss = { showCustomDialog = false },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CustomExerciseDialog(
    visible: Boolean,
    editingExercise: StrengthExerciseEntity? = null,
    visibility: MutableTransitionState<Boolean> = rememberOverlayVisibility(visible),
    onSave: suspend (
        name: String,
        region: BodyRegion,
        primaryMask: Int,
        secondaryMask: Int,
        defaultSets: Int,
        defaultReps: Int,
        isTimed: Boolean,
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    val t = LocalThemeTokens.current
    val n = t.neutrals
    val a = t.accent
    val scope = rememberCoroutineScope()
    val isChinese = isChineseLocale()
    val focus = LocalFocusManager.current
    val title = stringResource(
        if (editingExercise == null) R.string.strength_custom_exercise_new
        else R.string.strength_custom_exercise_edit,
    )

    var name by remember { mutableStateOf("") }
    var region by remember { mutableStateOf(BodyRegion.CHEST) }
    var primaryMuscles by remember { mutableStateOf<Set<MuscleGroup>>(emptySet()) }
    var secondaryMuscles by remember { mutableStateOf<Set<MuscleGroup>>(emptySet()) }
    var isTimed by remember { mutableStateOf(false) }
    var setsText by remember { mutableStateOf("4") }
    var repsText by remember { mutableStateOf("10") }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(visible, editingExercise?.id) {
        if (visible) {
            saving = false
            if (editingExercise != null) {
                name = exerciseDisplayName(editingExercise, isChinese)
                region = BodyRegion.entries.firstOrNull { it.key == editingExercise.bodyRegion } ?: BodyRegion.CHEST
                primaryMuscles = musclesFromMask(editingExercise.primaryMuscleMask)
                secondaryMuscles = musclesFromMask(editingExercise.secondaryMuscleMask)
                isTimed = editingExercise.isTimed
                setsText = editingExercise.defaultSets.toString()
                repsText = editingExercise.defaultReps.toString()
            } else {
                name = ""
                region = BodyRegion.CHEST
                primaryMuscles = emptySet()
                secondaryMuscles = emptySet()
                isTimed = false
                setsText = "4"
                repsText = "10"
            }
        }
    }

    val canSave = !saving && name.trim().isNotEmpty() && primaryMuscles.isNotEmpty()

    GlassOverlayPanel(
        visible = visible,
        onDismiss = {
            if (!saving) {
                focus.clearFocus()
                onDismiss()
            }
        },
        contentPadding = PaddingValues(20.dp),
        title = title,
        visibility = visibility,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BasicText(
                title,
                Modifier.semantics { heading() },
                style = TextStyle(n.textPrimary.asColor(), 18.sp, FontWeight.SemiBold),
            )

            // Exercise name
            val nameLabel = stringResource(R.string.strength_custom_exercise_name)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BasicText(nameLabel, style = TextStyle(n.textSecondary.asColor(), 13.sp))
                val fieldShape = RoundedCornerShape(12.dp)
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    enabled = !saving,
                    singleLine = true,
                    textStyle = TextStyle(n.textPrimary.asColor(), 15.sp),
                    cursorBrush = SolidColor(a.accentAction.asColor()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .clip(fieldShape)
                        .background(n.surfaceMuted.asColor())
                        .semantics { contentDescription = nameLabel }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }

            // 6-region selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BasicText(
                    stringResource(R.string.strength_custom_exercise_region),
                    style = TextStyle(n.textSecondary.asColor(), 13.sp),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    BodyRegion.entries.forEach { item ->
                        PickerFilterChip(
                            label = stringResource(item.labelRes()),
                            selected = region == item,
                            onClick = { region = item },
                        )
                    }
                }
            }

            // Primary muscles (1-2)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BasicText(
                    stringResource(R.string.strength_custom_exercise_primary),
                    style = TextStyle(n.textSecondary.asColor(), 13.sp),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    MuscleGroup.entries.forEach { muscle ->
                        val isPrimary = muscle in primaryMuscles
                        PickerFilterChip(
                            label = stringResource(muscle.labelRes()),
                            selected = isPrimary,
                            onClick = {
                                if (isPrimary) {
                                    primaryMuscles = primaryMuscles - muscle
                                } else if (primaryMuscles.size < 2) {
                                    primaryMuscles = primaryMuscles + muscle
                                    secondaryMuscles = secondaryMuscles - muscle
                                }
                            },
                        )
                    }
                }
            }

            // Secondary muscles (0-2, excluding primary)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BasicText(
                    stringResource(R.string.strength_custom_exercise_secondary),
                    style = TextStyle(n.textSecondary.asColor(), 13.sp),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    MuscleGroup.entries.filter { it !in primaryMuscles }.forEach { muscle ->
                        val isSecondary = muscle in secondaryMuscles
                        PickerFilterChip(
                            label = stringResource(muscle.labelRes()),
                            selected = isSecondary,
                            onClick = {
                                if (isSecondary) {
                                    secondaryMuscles = secondaryMuscles - muscle
                                } else if (secondaryMuscles.size < 2) {
                                    secondaryMuscles = secondaryMuscles + muscle
                                }
                            },
                        )
                    }
                }
            }

            // Tracking unit (reps vs seconds)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BasicText(
                    stringResource(R.string.strength_unit_label),
                    style = TextStyle(n.textSecondary.asColor(), 13.sp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PickerFilterChip(
                        label = stringResource(R.string.strength_unit_reps),
                        selected = !isTimed,
                        onClick = {
                            if (isTimed) {
                                isTimed = false
                                if (repsText == "60") repsText = "10"
                            }
                        },
                    )
                    PickerFilterChip(
                        label = stringResource(R.string.strength_unit_secs),
                        selected = isTimed,
                        onClick = {
                            if (!isTimed) {
                                isTimed = true
                                if (repsText == "10") repsText = "60"
                            }
                        },
                    )
                }
            }

            // Default sets & reps/secs
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CompactNumberField(
                    label = stringResource(R.string.strength_sets_label),
                    value = setsText,
                    onValueChange = { raw -> setsText = raw.filter(Char::isDigit).take(2) },
                    modifier = Modifier.weight(1f),
                )
                CompactNumberField(
                    label = stringResource(if (isTimed) R.string.strength_secs_label else R.string.strength_reps_label),
                    value = repsText,
                    onValueChange = { raw -> repsText = raw.filter(Char::isDigit).take(3) },
                    modifier = Modifier.weight(1f),
                )
            }

            // Action row
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
                        if (saving || !canSave) return@TextButton
                        focus.clearFocus()
                        saving = true
                        val sets = (setsText.toIntOrNull() ?: 4).coerceIn(1, 99)
                        val defaultRepsValue = if (isTimed) 60 else 10
                        val reps = (repsText.toIntOrNull() ?: defaultRepsValue).coerceIn(1, 999)
                        val trimmedName = name.trim()
                        val primary = muscleMaskOf(*primaryMuscles.toTypedArray())
                        val secondary = muscleMaskOf(*secondaryMuscles.toTypedArray())
                        scope.launch {
                            try {
                                onSave(trimmedName, region, primary, secondary, sets, reps, isTimed)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                // Keep dialog open on failure
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

@Composable
internal fun PickerFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val t = LocalThemeTokens.current
    val n = t.neutrals
    val a = t.accent
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .clip(shape)
            .background(if (selected) a.accentSubtle.asColor() else n.surfaceMuted.asColor())
            .then(
                if (selected) Modifier.border(1.dp, a.accentStroke.asColor(), shape)
                else Modifier
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            label,
            style = TextStyle(
                color = if (selected) a.accentAction.asColor() else n.textSecondary.asColor(),
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            maxLines = 1,
        )
    }
}

@Composable
private fun CompactNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = LocalThemeTokens.current
    val n = t.neutrals
    val a = t.accent
    val fieldShape = RoundedCornerShape(10.dp)
    Row(
        modifier
            .heightIn(min = 42.dp)
            .clip(fieldShape)
            .background(n.surfaceMuted.asColor())
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(n.textPrimary.asColor(), 15.sp, FontWeight.Medium),
            cursorBrush = SolidColor(a.accentAction.asColor()),
            modifier = Modifier.weight(1f).semantics { contentDescription = label },
        )
        BasicText(label, style = TextStyle(n.textSecondary.asColor(), 13.sp))
    }
}
