package com.actiary.ui.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.actiary.R
import com.actiary.data.StrengthRepository
import com.actiary.data.db.StrengthExerciseEntity
import com.actiary.data.db.WorkoutPlanWithItems
import com.actiary.domain.BodyRegion
import com.actiary.domain.musclesFromMask
import com.actiary.glass.GlassSegmentedPicker
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor
import com.actiary.ui.common.exerciseDisplayName
import com.actiary.ui.common.formatStrengthItemMetrics
import com.actiary.ui.common.isChineseLocale
import com.actiary.ui.common.labelRes
import com.actiary.ui.common.summarizeStrengthMuscles
import com.actiary.ui.common.workoutPlanLabel
import com.actiary.ui.motion.rememberMotionReduced
import com.actiary.ui.record.PickerFilterChip
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun WorkoutPlansScreen(
    repository: StrengthRepository,
    onBack: () -> Unit,
    onRequestNewPlan: () -> Unit,
    onRequestEditPlan: (WorkoutPlanWithItems) -> Unit,
    onRequestDeletePlan: (WorkoutPlanWithItems) -> Unit,
    onRequestNewExercise: () -> Unit,
    onRequestEditExercise: (StrengthExerciseEntity) -> Unit,
    onRequestExerciseHistory: (StrengthExerciseEntity) -> Unit,
) {
    val plans by repository.plans.collectAsStateWithLifecycle(emptyList())
    val allExercises by repository.allExercises.collectAsStateWithLifecycle(emptyList())
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val scroll = rememberScrollState()

    SettingsPageLayout(
        title = stringResource(R.string.strength_plans_title),
        onBack = onBack,
        scrollState = scroll,
    ) {
        GlassSegmentedPicker(
            options = listOf(
                stringResource(R.string.strength_tab_plans),
                stringResource(R.string.strength_tab_exercises),
            ),
            selected = selectedTab,
            onSelect = { selectedTab = it },
        )

        if (selectedTab == 0) {
            PlansTabContent(
                plans = plans,
                allExercises = allExercises,
                repository = repository,
                scroll = scroll,
                onRequestNewPlan = onRequestNewPlan,
                onRequestEditPlan = onRequestEditPlan,
                onRequestDeletePlan = onRequestDeletePlan,
            )
        } else {
            ExercisesTabContent(
                allExercises = allExercises,
                repository = repository,
                onRequestNewExercise = onRequestNewExercise,
                onRequestEditExercise = onRequestEditExercise,
                onRequestExerciseHistory = onRequestExerciseHistory,
            )
        }
    }
}

@Composable
private fun PlansTabContent(
    plans: List<WorkoutPlanWithItems>,
    allExercises: List<StrengthExerciseEntity>,
    repository: StrengthRepository,
    scroll: ScrollState,
    onRequestNewPlan: () -> Unit,
    onRequestEditPlan: (WorkoutPlanWithItems) -> Unit,
    onRequestDeletePlan: (WorkoutPlanWithItems) -> Unit,
) {
    val n = LocalThemeTokens.current.neutrals
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val windowHeight = LocalWindowInfo.current.containerSize.height
    val spacing = with(density) { 12.dp.toPx() }
    val edge = with(density) { 80.dp.toPx() }
    val exerciseMap = remember(allExercises) { allExercises.associateBy { it.id } }

    var openId by remember { mutableStateOf<Long?>(null) }
    var dragId by remember { mutableStateOf<Long?>(null) }
    var dragDistance by remember { mutableFloatStateOf(0f) }
    var dragStartY by remember { mutableFloatStateOf(0f) }
    var scrollStart by remember { mutableIntStateOf(0) }
    var moving by remember { mutableStateOf(false) }
    var moveFailed by remember { mutableStateOf(false) }
    val heights = remember { mutableStateMapOf<Long, Int>() }

    val source = plans.indexOfFirst { it.plan.id == dragId }
    val displacement = if (source < 0) 0f else dragDistance + scroll.value - scrollStart
    val rowSizes = if (source < 0) emptyList() else plans.map { heights.getValue(it.plan.id) }
    val target = if (source < 0) source else typeDragDestination(source, rowSizes, spacing, displacement)
    var lastHapticTarget by remember { mutableIntStateOf(-1) }

    LaunchedEffect(dragId, target) {
        if (dragId == null) {
            lastHapticTarget = -1
        } else if (lastHapticTarget >= 0 && target != lastHapticTarget) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            lastHapticTarget = target
        } else {
            lastHapticTarget = target
        }
    }

    fun move(id: Long, offset: Int) {
        if (moving || offset == 0) return
        moving = true
        moveFailed = false
        scope.launch {
            try {
                repository.movePlan(id, offset)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                moveFailed = true
            } finally {
                moving = false
                dragId = null
            }
        }
    }

    LaunchedEffect(dragId, moving, windowHeight) {
        if (dragId == null || moving) return@LaunchedEffect
        var previous = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val pointerY = dragStartY + dragDistance
            val top = edge * 2f
            val bottom = (windowHeight - edge).coerceAtLeast(top)
            val pressure = when {
                pointerY < top -> (pointerY - top) / edge
                pointerY > bottom -> (pointerY - bottom) / edge
                else -> 0f
            }.coerceIn(-1f, 1f)
            if (pressure != 0f) scroll.scrollBy(pressure * edge * 7f * (now - previous) / 1_000_000_000f)
            previous = now
        }
    }

    SettingsRow(stringResource(R.string.strength_plan_new), Icons.Outlined.Add, onClick = onRequestNewPlan)

    if (moveFailed) {
        BasicText(
            stringResource(R.string.record_save_failed),
            Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = TextStyle(n.textPrimary.asColor(), 14.sp),
        )
    }

    if (plans.isEmpty()) {
        BasicText(
            stringResource(R.string.strength_plan_empty),
            Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            style = TextStyle(n.textSecondary.asColor(), 15.sp),
        )
    } else {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            plans.forEachIndexed { index, planWithItems ->
                val planId = planWithItems.plan.id
                key(planId) {
                    val shift = when {
                        index == source -> displacement
                        index > source && index <= target -> -(rowSizes[source] + spacing)
                        index < source && index >= target -> rowSizes[source] + spacing
                        else -> 0f
                    }
                    val totalSets = planWithItems.items.sumOf { it.defaultSets }
                    val primaryMasks = planWithItems.items.mapNotNull { exerciseMap[it.exerciseId]?.primaryMuscleMask }
                    val muscles = summarizeStrengthMuscles(primaryMasks, maxMuscles = 3)
                    val countsText = stringResource(R.string.strength_summary_counts, planWithItems.items.size, totalSets)
                    val muscleText = if (muscles.isNotEmpty()) {
                        stringResource(
                            R.string.strength_active_muscles,
                            muscles.map { stringResource(it.labelRes()) }.joinToString(" · "),
                        )
                    } else {
                        null
                    }
                    WorkoutPlanRow(
                        planWithItems = planWithItems,
                        countsText = countsText,
                        muscleText = muscleText,
                        open = openId == planId,
                        onOpen = { opened -> if (opened || openId == planId) openId = planId.takeIf { opened } },
                        onEdit = {
                            openId = null
                            onRequestEditPlan(planWithItems)
                        },
                        onDelete = {
                            openId = null
                            onRequestDeletePlan(planWithItems)
                        },
                        canGesture = dragId == null && !moving,
                        dragging = dragId == planId,
                        shift = shift,
                        onHeight = { heights[planId] = it },
                        onDragStart = { pointerY ->
                            if (dragId == null && !moving && plans.all { it.plan.id in heights }) {
                                openId = null
                                dragStartY = pointerY
                                dragDistance = 0f
                                scrollStart = scroll.value
                                dragId = planId
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        },
                        onDrag = { if (dragId == planId && !moving) dragDistance += it },
                        onDragEnd = { committed ->
                            if (dragId == planId) {
                                val distance = dragDistance + scroll.value - scrollStart
                                val destination = typeDragDestination(
                                    index,
                                    plans.map { heights.getValue(it.plan.id) },
                                    spacing,
                                    distance,
                                )
                                if (committed && destination != index) move(planId, destination - index)
                                else dragId = null
                            }
                        },
                        onMoveUp = if (index > 0 && dragId == null && !moving) ({ move(planId, -1) }) else null,
                        onMoveDown = if (index < plans.lastIndex && dragId == null && !moving) ({ move(planId, 1) }) else null,
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutPlanRow(
    planWithItems: WorkoutPlanWithItems,
    countsText: String,
    muscleText: String?,
    open: Boolean,
    onOpen: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    canGesture: Boolean,
    dragging: Boolean,
    shift: Float,
    onHeight: (Int) -> Unit,
    onDragStart: (Float) -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: (Boolean) -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
) {
    val t = LocalThemeTokens.current
    val n = t.neutrals
    val label = workoutPlanLabel(planWithItems.plan)
    val editLabel = stringResource(R.string.custom_type_edit_named, label)
    val deleteLabel = stringResource(R.string.custom_type_delete_named, label)
    val upLabel = stringResource(R.string.custom_type_move_up, label)
    val downLabel = stringResource(R.string.custom_type_move_down, label)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val direction = if (rtl) 1f else -1f
    val density = LocalDensity.current
    val width = with(density) { 110.dp.toPx() }
    val reduced = rememberMotionReduced()
    var swiping by remember { mutableStateOf(false) }
    var swipeDistance by remember { mutableFloatStateOf(0f) }
    var handleTop by remember { mutableFloatStateOf(0f) }

    val reveal by animateFloatAsState(
        if (swiping) swipeDistance else if (open) width else 0f,
        if (reduced || swiping) snap() else tween(180),
        label = "Plan actions",
    )
    val rowShift by animateFloatAsState(shift, if (dragging || reduced) snap() else tween(120), label = "Plan order")
    val dragScale by animateFloatAsState(
        if (dragging) 1.03f else 1f,
        if (reduced) snap() else tween(140),
        label = "Plan drag scale",
    )
    val beginDrag by rememberUpdatedState(onDragStart)
    val moveDrag by rememberUpdatedState(onDrag)
    val endDrag by rememberUpdatedState(onDragEnd)
    val shape = RoundedCornerShape(12.dp)
    val deleteBg = if (t.dark) Color(0xFF4D1F1D) else Color(0xFFFDE8E6)
    val deleteTint = if (t.dark) Color(0xFFFFB4AB) else Color(0xFFB3261E)
    val revealProgress = if (reduced) {
        if (reveal > 0f) 1f else 0f
    } else {
        (reveal / width).coerceIn(0f, 1f)
    }

    Box(
        Modifier
            .fillMaxWidth()
            .onSizeChanged { onHeight(it.height) }
            .zIndex(if (dragging) 1f else 0f)
            .offset { IntOffset(0, rowShift.roundToInt()) }
            .graphicsLayer {
                scaleX = dragScale
                scaleY = dragScale
                shadowElevation = if (dragging) 8.dp.toPx() else 0f
                this.shape = shape
                clip = true
            },
    ) {
        Row(
            Modifier
                .align(Alignment.CenterEnd)
                .padding(start = 6.dp)
                .then(if (open) Modifier else Modifier.clearAndSetSemantics { hideFromAccessibility() }),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlanRowAction(
                Icons.Outlined.Edit, editLabel,
                t.accent.accentSubtle.asColor(), t.accent.accentStroke.asColor(), t.accent.accentAction.asColor(),
                revealProgress, open, onEdit,
            )
            PlanRowAction(
                Icons.Outlined.DeleteOutline, deleteLabel,
                deleteBg, deleteTint.copy(alpha = 0.45f), deleteTint,
                revealProgress, open, onDelete,
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .padding(end = with(density) { reveal.toDp() })
                .background(n.surface.asColor(), shape)
                .clickable(enabled = canGesture, role = Role.Button, onClick = onEdit)
                .draggable(
                    rememberDraggableState { delta -> swipeDistance = (swipeDistance + delta * direction).coerceIn(0f, width) },
                    Orientation.Horizontal,
                    enabled = canGesture,
                    onDragStarted = { swipeDistance = reveal; swiping = true },
                    onDragStopped = { onOpen(swipeDistance >= width / 2); swiping = false },
                )
                .semantics(mergeDescendants = true) {
                    customActions = buildList {
                        add(CustomAccessibilityAction(editLabel) { onEdit(); true })
                        add(CustomAccessibilityAction(deleteLabel) { onDelete(); true })
                        if (onMoveUp != null) add(CustomAccessibilityAction(upLabel) { onMoveUp(); true })
                        if (onMoveDown != null) add(CustomAccessibilityAction(downLabel) { onMoveDown(); true })
                    }
                }
                .padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText(
                    label,
                    style = TextStyle(n.textPrimary.asColor(), 16.sp, FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                BasicText(
                    countsText,
                    style = TextStyle(n.textSecondary.asColor(), 13.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (muscleText != null) {
                    BasicText(
                        muscleText,
                        style = TextStyle(n.textSecondary.asColor(), 12.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Box(
                Modifier
                    .size(48.dp)
                    .onGloballyPositioned { handleTop = it.positionInWindow().y }
                    .pointerInput(planWithItems.plan.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { beginDrag(handleTop + it.y) },
                            onDragEnd = { endDrag(true) },
                            onDragCancel = { endDrag(false) },
                        ) { change, offset ->
                            moveDrag(offset.y)
                            change.consume()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.DragHandle, null, tint = n.textSecondary.asColor())
            }
        }
    }
}

@Composable
private fun PlanRowAction(
    icon: ImageVector,
    label: String,
    background: Color,
    border: Color,
    tint: Color,
    revealProgress: Float,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val actionShape = RoundedCornerShape(12.dp)
    val scale = 0.72f + 0.28f * revealProgress
    Box(
        Modifier
            .size(width = 48.dp, height = 56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = revealProgress
            }
            .clip(actionShape)
            .background(background, actionShape)
            .border(1.dp, border, actionShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = tint)
    }
}

@Composable
private fun ExercisesTabContent(
    allExercises: List<StrengthExerciseEntity>,
    repository: StrengthRepository,
    onRequestNewExercise: () -> Unit,
    onRequestEditExercise: (StrengthExerciseEntity) -> Unit,
    onRequestExerciseHistory: (StrengthExerciseEntity) -> Unit,
) {
    val t = LocalThemeTokens.current
    val n = t.neutrals
    val a = t.accent
    val scope = rememberCoroutineScope()
    val isChinese = isChineseLocale()

    var query by rememberSaveable { mutableStateOf("") }
    var selectedRegionKey by rememberSaveable { mutableStateOf<String?>(null) }
    var hiddenExpanded by rememberSaveable { mutableStateOf(true) }

    val selectedRegion = remember(selectedRegionKey) {
        BodyRegion.entries.firstOrNull { it.key == selectedRegionKey }
    }
    val activeExercises = remember(allExercises, query, selectedRegion) {
        val trimmed = query.trim()
        allExercises.filter { ex ->
            !ex.archived &&
                (selectedRegion == null || ex.bodyRegion == selectedRegion.key) &&
                (trimmed.isEmpty() || ex.nameZh.contains(trimmed, ignoreCase = true) || ex.nameEn.contains(trimmed, ignoreCase = true))
        }
    }
    val hiddenBuiltins = remember(allExercises) {
        allExercises.filter { it.builtinKey != null && it.archived }
    }

    SettingsRow(stringResource(R.string.strength_custom_exercise_new), Icons.Outlined.Add, onClick = onRequestNewExercise)

    val searchPlaceholder = stringResource(R.string.strength_picker_search)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(12.dp))
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
                Icon(Icons.Outlined.Close, stringResource(R.string.overlay_close), Modifier.size(16.dp), tint = n.textSecondary.asColor())
            }
        }
    }

    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PickerFilterChip(
            label = stringResource(R.string.region_all),
            selected = selectedRegion == null,
            onClick = { selectedRegionKey = null },
        )
        BodyRegion.entries.forEach { region ->
            PickerFilterChip(
                label = stringResource(region.labelRes()),
                selected = selectedRegion == region,
                onClick = { selectedRegionKey = if (selectedRegion == region) null else region.key },
            )
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        activeExercises.forEach { exercise ->
            key(exercise.id) {
                ExerciseLibraryRow(
                    exercise = exercise,
                    isChinese = isChinese,
                    onHistory = { onRequestExerciseHistory(exercise) },
                    onEdit = { onRequestEditExercise(exercise) },
                    onDeleteCustom = { scope.launch { repository.removeCustomExercise(exercise.id) } },
                    onHideBuiltin = { scope.launch { repository.setBuiltinExerciseHidden(exercise.id, true) } },
                )
            }
        }
    }

    if (hiddenBuiltins.isNotEmpty()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable(role = Role.Button) { hiddenExpanded = !hiddenExpanded }
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BasicText(
                stringResource(R.string.strength_exercise_hidden_section),
                Modifier.semantics { heading() },
                style = TextStyle(n.textSecondary.asColor(), 14.sp, FontWeight.SemiBold),
            )
            Icon(
                imageVector = if (hiddenExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = n.textSecondary.asColor(),
            )
        }
        if (hiddenExpanded) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                hiddenBuiltins.forEach { hidden ->
                    key("hidden_${hidden.id}") {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(n.surface.asColor().copy(alpha = 0.72f))
                                .padding(start = 12.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BasicText(
                                exerciseDisplayName(hidden, isChinese),
                                Modifier.weight(1f).padding(end = 8.dp),
                                style = TextStyle(n.textSecondary.asColor(), 15.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            TextButton(
                                onClick = { scope.launch { repository.setBuiltinExerciseHidden(hidden.id, false) } },
                                colors = ButtonDefaults.textButtonColors(contentColor = a.accentAction.asColor()),
                            ) {
                                Icon(Icons.Outlined.Visibility, null, Modifier.size(18.dp))
                                Text(
                                    stringResource(R.string.strength_exercise_restore),
                                    Modifier.padding(start = 6.dp),
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseLibraryRow(
    exercise: StrengthExerciseEntity,
    isChinese: Boolean,
    onHistory: () -> Unit,
    onEdit: () -> Unit,
    onDeleteCustom: () -> Unit,
    onHideBuiltin: () -> Unit,
) {
    val n = LocalThemeTokens.current.neutrals
    val name = exerciseDisplayName(exercise, isChinese)
    val muscles = musclesFromMask(exercise.primaryMuscleMask)
        .map { stringResource(it.labelRes()) }
        .joinToString(" · ")
    val defaultSpec = formatStrengthItemMetrics(
        sets = exercise.defaultSets,
        reps = exercise.defaultReps,
        weightKg = null,
        isTimed = exercise.isTimed,
    )
    val subtitle = if (muscles.isNotEmpty()) "$muscles · $defaultSpec" else defaultSpec
    val isCustom = exercise.builtinKey == null

    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(n.surface.asColor())
            .clickable(role = Role.Button, onClick = if (isCustom) onEdit else onHistory)
            .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(
                name,
                style = TextStyle(n.textPrimary.asColor(), 15.sp, FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                subtitle,
                style = TextStyle(n.textSecondary.asColor(), 12.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(role = Role.Button, onClick = onHistory),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.History,
                stringResource(R.string.strength_history_title),
                Modifier.size(18.dp),
                tint = n.textSecondary.asColor(),
            )
        }

        if (isCustom) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button, onClick = onEdit),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Edit,
                    stringResource(R.string.custom_type_edit_named, name),
                    Modifier.size(18.dp),
                    tint = n.textSecondary.asColor(),
                )
            }
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button, onClick = onDeleteCustom),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    stringResource(R.string.custom_type_delete_named, name),
                    Modifier.size(18.dp),
                    tint = n.textSecondary.asColor(),
                )
            }
        } else {
            TextButton(
                onClick = onHideBuiltin,
                colors = ButtonDefaults.textButtonColors(contentColor = n.textSecondary.asColor()),
            ) {
                Icon(Icons.Outlined.VisibilityOff, null, Modifier.size(16.dp))
                Text(stringResource(R.string.strength_exercise_hide), Modifier.padding(start = 4.dp))
            }
        }
    }
}
