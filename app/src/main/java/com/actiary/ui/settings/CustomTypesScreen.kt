package com.actiary.ui.settings

import android.app.Application
import android.os.Bundle
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Edit
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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.os.bundleOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.actiary.ActiaryApp
import com.actiary.R
import com.actiary.data.RecordRepository
import com.actiary.data.appearance.UiStyle
import com.actiary.data.db.ActiaryDatabase
import com.actiary.data.db.ExerciseTypeEntity
import com.actiary.domain.WorkoutMetric
import com.actiary.domain.availableWorkoutMetrics
import com.actiary.domain.effectiveWorkoutMetricMask
import com.actiary.glass.GlassOverlayPanel
import com.actiary.glass.GlassSheet
import com.actiary.glass.overlayInteraction
import com.actiary.glass.rememberOverlayVisibility
import com.actiary.glass.GlassToggle
import com.actiary.ui.motion.rememberMotionReduced
import com.actiary.theme.LocalP5Tokens
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.LocalUiStyle
import com.actiary.theme.asColor
import com.actiary.ui.common.CUSTOM_TYPE_ICONS
import com.actiary.ui.common.exerciseTypeIcon
import com.actiary.ui.common.exerciseTypeLabel
import com.actiary.ui.common.labelId
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlin.math.abs
import kotlin.math.roundToInt

internal class AddTypeActions(application: Application) : AndroidViewModel(application) {
    enum class State { Idle, Saving, Saved, Failed }

    var state by mutableStateOf(State.Idle)
        private set

    fun save(typeId: Long?, name: String?, hasDistance: Boolean, iconKey: String?, extraMetricMask: Int?) {
        if (state == State.Saving || state == State.Saved) return
        state = State.Saving
        viewModelScope.launch {
            try {
                val repository = (getApplication<Application>() as ActiaryApp).container.recordRepository
                if (typeId == null) repository.addType(requireNotNull(name), hasDistance, iconKey, extraMetricMask)
                else repository.updateType(typeId, name, hasDistance, iconKey, extraMetricMask)
                state = State.Saved
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state = State.Failed
            }
        }
    }

    fun reset() {
        state = State.Idle
    }
}

internal val EditingTypeSaver = Saver<ExerciseTypeEntity?, Bundle>(
    save = { type ->
        type?.let {
            bundleOf(
                "id" to it.id, "builtin" to it.builtinKey, "name" to it.name,
                "distance" to it.hasDistance, "icon" to it.iconKey,
            ).apply {
                it.extraMetricMask?.let { mask -> putInt("extraMetrics", mask) }
            }
        }
    },
    restore = {
        ExerciseTypeEntity(
            id = it.getLong("id"), builtinKey = it.getString("builtin"), name = it.getString("name"),
            sortOrder = 0, hasDistance = it.getBoolean("distance"), iconKey = it.getString("icon"),
            extraMetricMask = if (it.containsKey("extraMetrics")) it.getInt("extraMetrics") else null,
        )
    },
)

@Composable
fun CustomTypesScreen(
    repository: RecordRepository,
    onBack: () -> Unit,
    onRequestAdd: () -> Unit,
    onRequestEdit: (ExerciseTypeEntity) -> Unit,
    onRequestRemoval: (Long) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val types by repository.types.collectAsStateWithLifecycle(emptyList())
    val allTypes by repository.allTypes.collectAsStateWithLifecycle(emptyList())
    val hiddenBuiltins = remember(allTypes) {
        allTypes.filter { it.builtinKey in ActiaryDatabase.BUILTIN_TYPE_KEYS && it.archived }
    }
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val windowHeight = LocalWindowInfo.current.containerSize.height
    val spacing = with(density) { 16.dp.toPx() }
    val edge = with(density) { 80.dp.toPx() }
    var openId by remember { mutableStateOf<Long?>(null) }
    var dragId by remember { mutableStateOf<Long?>(null) }
    var dragDistance by remember { mutableFloatStateOf(0f) }
    var dragStartY by remember { mutableFloatStateOf(0f) }
    var scrollStart by remember { mutableIntStateOf(0) }
    var moving by remember { mutableStateOf(false) }
    var moveFailed by remember { mutableStateOf(false) }
    val heights = remember { mutableStateMapOf<Long, Int>() }
    val source = types.indexOfFirst { it.id == dragId }
    val displacement = if (source < 0) 0f else dragDistance + scroll.value - scrollStart
    val rowSizes = if (source < 0) emptyList() else types.map { heights.getValue(it.id) }
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
                repository.moveType(id, offset)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
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

    SettingsPageLayout(stringResource(R.string.settings_custom_types), onBack, scrollState = scroll) {
        SettingsRow(stringResource(R.string.custom_type_add_title), Icons.Outlined.Add, onClick = onRequestAdd)
        if (moveFailed) BasicText(stringResource(R.string.record_save_failed), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        types.forEachIndexed { index, type ->
            key(type.id) {
                val shift = when {
                    index == source -> displacement
                    index > source && index <= target -> -(rowSizes[source] + spacing)
                    index < source && index >= target -> rowSizes[source] + spacing
                    else -> 0f
                }
                WorkoutTypeRow(
                    type = type,
                    open = openId == type.id,
                    onOpen = { opened -> if (opened || openId == type.id) openId = type.id.takeIf { opened } },
                    onEdit = { openId = null; onRequestEdit(type) },
                    onRemove = if (type.builtinKey != null) null else ({ openId = null; onRequestRemoval(type.id) }),
                    onHide = if (type.builtinKey != null && types.size > 1) {
                        { openId = null; scope.launch { repository.setBuiltinTypeHidden(type.id, true) } }
                    } else null,
                    canGesture = dragId == null && !moving,
                    dragging = dragId == type.id,
                    shift = shift,
                    onHeight = { heights[type.id] = it },
                    onDragStart = { pointerY ->
                        if (dragId == null && !moving && types.all { it.id in heights }) {
                            openId = null
                            dragStartY = pointerY
                            dragDistance = 0f
                            scrollStart = scroll.value
                            dragId = type.id
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    onDrag = { if (dragId == type.id && !moving) dragDistance += it },
                    onDragEnd = { committed ->
                        if (dragId == type.id) {
                            val distance = dragDistance + scroll.value - scrollStart
                            val destination = typeDragDestination(index, types.map { heights.getValue(it.id) }, spacing, distance)
                            if (committed && destination != index) move(type.id, destination - index) else dragId = null
                        }
                    },
                    onMoveUp = if (index > 0 && dragId == null && !moving) ({ move(type.id, -1) }) else null,
                    onMoveDown = if (index < types.lastIndex && dragId == null && !moving) ({ move(type.id, 1) }) else null,
                )
            }
        }
        if (hiddenBuiltins.isNotEmpty()) {
            val t = LocalThemeTokens.current
            val n = t.neutrals
            val isP5 = LocalUiStyle.current == UiStyle.PERSONA_5
            val p5 = LocalP5Tokens.current
            val accent = t.accent.accentAction.asColor()
            BasicText(
                stringResource(R.string.custom_type_hidden_section),
                Modifier.padding(top = 8.dp, start = 4.dp).semantics { heading() },
                style = TextStyle(n.textSecondary.asColor(), 14.sp, if (isP5) FontWeight.Black else FontWeight.SemiBold),
            )
            hiddenBuiltins.forEach { hidden ->
                key("hidden_${hidden.id}") {
                    val label = exerciseTypeLabel(hidden)
                    val hiddenShape = if (isP5) RoundedCornerShape(2.dp) else RoundedCornerShape(12.dp)
                    val hiddenBg = if (isP5) {
                        if (t.dark) p5.darkCard.asColor() else p5.lightCard.asColor()
                    } else {
                        n.surface.asColor().copy(alpha = 0.72f)
                    }
                    val hiddenBorder = if (isP5) {
                        Modifier.border(
                            1.5.dp,
                            if (t.dark) p5.strokeLight.asColor().copy(alpha = 0.45f) else p5.strokeDark.asColor(),
                            hiddenShape,
                        )
                    } else {
                        Modifier
                    }
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 52.dp)
                            .clip(hiddenShape)
                            .background(hiddenBg, hiddenShape)
                            .then(hiddenBorder)
                            .padding(start = 12.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(exerciseTypeIcon(hidden), null, Modifier.size(22.dp), tint = n.textSecondary.asColor())
                        BasicText(
                            label,
                            Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp),
                            style = TextStyle(n.textSecondary.asColor(), 15.sp, if (isP5) FontWeight.Bold else FontWeight.Normal),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        TextButton(
                            onClick = { scope.launch { repository.setBuiltinTypeHidden(hidden.id, false) } },
                            colors = ButtonDefaults.textButtonColors(contentColor = accent),
                        ) {
                            Icon(Icons.Outlined.Visibility, null, Modifier.size(18.dp))
                            Text(
                                stringResource(R.string.custom_type_restore),
                                Modifier.padding(start = 6.dp),
                                fontWeight = if (isP5) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun typeDragDestination(source: Int, rowSizes: List<Int>, gap: Float, distance: Float): Int {
    val direction = if (distance >= 0f) 1 else -1
    var index = source
    var remaining = abs(distance)
    while (index + direction in rowSizes.indices) {
        val next = index + direction
        remaining -= (rowSizes[index] + rowSizes[next]) / 2f + gap
        if (remaining < 0f) break
        index = next
    }
    return index
}

@Composable
private fun WorkoutTypeRow(
    type: ExerciseTypeEntity,
    open: Boolean,
    onOpen: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onRemove: (() -> Unit)?,
    onHide: (() -> Unit)?,
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
    val label = exerciseTypeLabel(type)
    val edit = stringResource(R.string.custom_type_edit_named, label)
    val remove = stringResource(R.string.custom_type_delete_named, label)
    val hide = stringResource(R.string.custom_type_hide_named, label)
    val up = stringResource(R.string.custom_type_move_up, label)
    val down = stringResource(R.string.custom_type_move_down, label)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val direction = if (rtl) 1f else -1f
    val density = LocalDensity.current
    val hasSecondAction = onRemove != null || onHide != null
    val width = with(density) { (if (hasSecondAction) 110.dp else 56.dp).toPx() }
    val reduced = rememberMotionReduced()
    var swiping by remember { mutableStateOf(false) }
    var swipeDistance by remember { mutableFloatStateOf(0f) }
    var handleTop by remember { mutableFloatStateOf(0f) }
    val reveal by animateFloatAsState(
        if (swiping) swipeDistance else if (open) width else 0f,
        if (reduced || swiping) snap() else tween(180), label = "Type actions",
    )
    val rowShift by animateFloatAsState(shift, if (dragging || reduced) snap() else tween(120), label = "Type order")
    val dragScale by animateFloatAsState(
        if (dragging) 1.03f else 1f,
        if (reduced) snap() else tween(140),
        label = "Type drag scale",
    )
    val beginDrag by rememberUpdatedState(onDragStart)
    val moveDrag by rememberUpdatedState(onDrag)
    val endDrag by rememberUpdatedState(onDragEnd)
    val isP5 = LocalUiStyle.current == UiStyle.PERSONA_5
    val p5 = LocalP5Tokens.current
    val shape = if (isP5) RoundedCornerShape(2.dp) else RoundedCornerShape(12.dp)
    val rowBg = if (isP5) {
        if (t.dark) p5.darkCard.asColor() else p5.lightCard.asColor()
    } else {
        n.surface.asColor()
    }
    val rowBorder = if (isP5) {
        Modifier.border(2.dp, if (t.dark) p5.strokeLight.asColor() else p5.strokeDark.asColor(), shape)
    } else {
        Modifier
    }
    val deleteBg = if (isP5) Color(0xFFD91E26) else if (t.dark) Color(0xFF4D1F1D) else Color(0xFFFDE8E6)
    val deleteTint = if (isP5) Color.White else if (t.dark) Color(0xFFFFB4AB) else Color(0xFFB3261E)
    val revealProgress = if (reduced) {
        if (reveal > 0f) 1f else 0f
    } else {
        (reveal / width).coerceIn(0f, 1f)
    }
    Box(
        Modifier.fillMaxWidth().onSizeChanged { onHeight(it.height) }.zIndex(if (dragging) 1f else 0f)
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
            Modifier.align(Alignment.CenterEnd)
                .padding(start = 6.dp)
                .then(if (open) Modifier else Modifier.clearAndSetSemantics { hideFromAccessibility() }),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TypeAction(
                Icons.Outlined.Edit, edit,
                if (isP5) p5.brandFill.asColor() else t.accent.accentSubtle.asColor(),
                if (isP5) (if (t.dark) p5.strokeLight.asColor() else p5.strokeDark.asColor()) else t.accent.accentStroke.asColor(),
                if (isP5) p5.onBrandFill.asColor() else t.accent.accentAction.asColor(),
                revealProgress, open, onEdit,
            )
            if (onRemove != null) {
                TypeAction(
                    Icons.Outlined.DeleteOutline, remove,
                    deleteBg,
                    if (isP5) (if (t.dark) p5.strokeLight.asColor() else p5.strokeDark.asColor()) else deleteTint.copy(alpha = 0.45f),
                    deleteTint,
                    revealProgress, open, onRemove,
                )
            } else if (onHide != null) {
                TypeAction(
                    Icons.Outlined.VisibilityOff, hide,
                    if (isP5) p5.darkCard.asColor() else n.surfaceMuted.asColor(),
                    if (isP5) (if (t.dark) p5.strokeLight.asColor() else p5.strokeDark.asColor()) else n.divider.asColor(),
                    if (isP5) p5.strokeLight.asColor() else n.textSecondary.asColor(),
                    revealProgress, open, onHide,
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .padding(end = with(density) { reveal.toDp() })
                .background(rowBg, shape)
                .then(rowBorder)
                .draggable(
                    rememberDraggableState { delta -> swipeDistance = (swipeDistance + delta * direction).coerceIn(0f, width) },
                    Orientation.Horizontal, enabled = canGesture,
                    onDragStarted = { swipeDistance = reveal; swiping = true },
                    onDragStopped = { onOpen(swipeDistance >= width / 2); swiping = false },
                )
                .semantics(mergeDescendants = true) {
                    customActions = buildList {
                        add(CustomAccessibilityAction(edit) { onEdit(); true })
                        if (onRemove != null) add(CustomAccessibilityAction(remove) { onRemove(); true })
                        if (onHide != null) add(CustomAccessibilityAction(hide) { onHide(); true })
                        if (onMoveUp != null) add(CustomAccessibilityAction(up) { onMoveUp(); true })
                        if (onMoveDown != null) add(CustomAccessibilityAction(down) { onMoveDown(); true })
                    }
                }
                .padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(exerciseTypeIcon(type), null, Modifier.size(24.dp), tint = n.textPrimary.asColor())
            BasicText(
                label,
                Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp),
                style = TextStyle(n.textPrimary.asColor(), 16.sp, if (isP5) FontWeight.Bold else FontWeight.Normal),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box(
                Modifier.size(48.dp)
                    .onGloballyPositioned { handleTop = it.positionInWindow().y }
                    .pointerInput(type.id) {
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
private fun TypeAction(
    icon: ImageVector,
    label: String,
    background: androidx.compose.ui.graphics.Color,
    border: androidx.compose.ui.graphics.Color,
    tint: androidx.compose.ui.graphics.Color,
    revealProgress: Float,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val isP5 = LocalUiStyle.current == UiStyle.PERSONA_5
    val actionShape = if (isP5) RoundedCornerShape(2.dp) else RoundedCornerShape(12.dp)
    val scale = 0.72f + 0.28f * revealProgress
    Box(
        Modifier.size(width = 48.dp, height = 52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = revealProgress
            }
            .clip(actionShape)
            .background(background, actionShape)
            .border(if (isP5) 2.dp else 1.dp, border, actionShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = tint)
    }
}

internal const val MAX_TYPE_NAME_LENGTH = 20

internal enum class TypeNameError { BLANK, TOO_LONG, DUPLICATE }

/**
 * Validates a workout type name against active (non-archived) types.
 * [existingLabels] pairs each active type's id with its display or built-in label.
 * When editing a built-in type ([allowBlank] = true), a blank input restores its default name.
 */
internal fun validateTypeName(
    name: String,
    existingLabels: List<Pair<Long, String>>,
    currentId: Long?,
    allowBlank: Boolean = false,
): TypeNameError? {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return if (allowBlank) null else TypeNameError.BLANK
    if (trimmed.length > MAX_TYPE_NAME_LENGTH) return TypeNameError.TOO_LONG
    val duplicate = existingLabels.any { (id, label) ->
        id != currentId && label.trim().equals(trimmed, ignoreCase = true)
    }
    if (duplicate) return TypeNameError.DUPLICATE
    return null
}

@Composable
internal fun AddTypeSheet(
    actions: AddTypeActions,
    editingType: ExerciseTypeEntity?,
    existingTypes: List<ExerciseTypeEntity>,
    iconKey: String?,
    covered: Boolean,
    onRequestIconPicker: () -> Unit,
    onDismiss: () -> Unit,
) {
    val t = LocalThemeTokens.current
    val n = t.neutrals
    val focus = LocalFocusManager.current
    val title = stringResource(if (editingType == null) R.string.custom_type_add_title else R.string.custom_type_edit_title)
    val initialName = if (editingType == null) "" else exerciseTypeLabel(editingType)
    val builtinName = editingType?.takeIf { it.builtinKey != null }?.let { exerciseTypeLabel(it.copy(name = null)) }
    var visible by rememberSaveable { mutableStateOf(true) }
    var name by rememberSaveable { mutableStateOf(initialName) }
    var hasDistance by rememberSaveable { mutableStateOf(editingType?.hasDistance ?: false) }
    val initialMetricMask = effectiveWorkoutMetricMask(editingType?.builtinKey, editingType?.extraMetricMask)
    var metricMask by rememberSaveable { mutableIntStateOf(initialMetricMask) }
    val saving = actions.state == AddTypeActions.State.Saving || actions.state == AddTypeActions.State.Saved
    val selectedIcon = CUSTOM_TYPE_ICONS.firstOrNull { it.key == iconKey }
    val existingLabels = existingTypes.flatMap { type ->
        buildList {
            add(type.id to exerciseTypeLabel(type))
            if (type.builtinKey != null) {
                add(type.id to exerciseTypeLabel(type.copy(name = null)))
            }
        }
    }
    val nameError = validateTypeName(name, existingLabels, editingType?.id, allowBlank = builtinName != null)
    val visibleNameError = when (nameError) {
        TypeNameError.TOO_LONG -> stringResource(R.string.custom_type_name_too_long, MAX_TYPE_NAME_LENGTH)
        TypeNameError.DUPLICATE -> stringResource(R.string.custom_type_name_duplicate)
        else -> null
    }
    val savedName = name.trim().takeUnless { builtinName != null && (it.isEmpty() || it == builtinName) }
    val canSave = !saving && nameError == null

    fun close(): Boolean {
        if (saving) return false
        focus.clearFocus()
        visible = false
        return true
    }

    LaunchedEffect(actions.state) {
        if (actions.state == AddTypeActions.State.Saved) {
            focus.clearFocus()
            visible = false
        }
    }
    val isP5 = LocalUiStyle.current == UiStyle.PERSONA_5
    val p5 = LocalP5Tokens.current
    val p5Stroke = if (t.dark) p5.strokeLight.asColor() else p5.strokeDark.asColor()
    val sheetPrimary = n.textPrimary.asColor()
    val sheetSecondary = n.textSecondary.asColor()
    val sheetAccent = t.accent.accentAction.asColor()
    GlassSheet(visible, ::close, onDismiss, title, covered = covered) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    title, Modifier.weight(1f).semantics { heading() },
                    style = TextStyle(sheetPrimary, 20.sp, if (isP5) FontWeight.Black else FontWeight.SemiBold),
                )
                TextButton(
                    onClick = { close() },
                    enabled = !saving,
                    colors = ButtonDefaults.textButtonColors(contentColor = sheetSecondary),
                ) { Text(stringResource(R.string.record_cancel), fontWeight = if (isP5) FontWeight.Bold else FontWeight.Normal) }
            }

            val nameLabel = stringResource(R.string.custom_type_name)
            BasicText(nameLabel, style = TextStyle(sheetSecondary, 14.sp, if (isP5) FontWeight.Bold else FontWeight.Normal))
            val fieldShape = if (isP5) RoundedCornerShape(2.dp) else RoundedCornerShape(12.dp)
            val fieldBg = if (isP5 && t.dark) Color(0xFF1F1F24) else n.surfaceMuted.asColor()
            val fieldBorder = when {
                visibleNameError != null -> Modifier.border(2.dp, if (isP5) p5.brandFill.asColor() else t.accent.accentStroke.asColor(), fieldShape)
                isP5 -> Modifier.border(2.dp, p5Stroke, fieldShape)
                else -> Modifier
            }
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                enabled = !saving,
                singleLine = true,
                textStyle = TextStyle(sheetPrimary, 16.sp),
                cursorBrush = SolidColor(sheetAccent),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .clip(fieldShape)
                    .background(fieldBg, fieldShape)
                    .then(fieldBorder)
                    .semantics {
                        contentDescription = nameLabel
                        if (visibleNameError != null) error(visibleNameError)
                    }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            )
            if (visibleNameError != null) {
                BasicText(
                    visibleNameError,
                    Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    style = TextStyle(sheetAccent, 13.sp),
                )
            }

            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .clickable(enabled = !saving, role = Role.Button, onClickLabel = stringResource(R.string.custom_type_choose_icon)) {
                        focus.clearFocus()
                        onRequestIconPicker()
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                BasicText(stringResource(R.string.custom_type_icon), style = TextStyle(sheetPrimary, 16.sp, if (isP5) FontWeight.Bold else FontWeight.Normal))
                val iconBoxShape = if (isP5) RoundedCornerShape(2.dp) else RoundedCornerShape(12.dp)
                val iconBoxBorder = if (isP5) Modifier.border(2.dp, p5Stroke, iconBoxShape) else Modifier
                Box(
                    Modifier.size(48.dp).background(fieldBg, iconBoxShape).then(iconBoxBorder),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selectedIcon != null) {
                        Icon(selectedIcon.vector, stringResource(selectedIcon.label), tint = sheetPrimary)
                    } else if (editingType != null) {
                        Icon(
                            exerciseTypeIcon(editingType.copy(iconKey = null)),
                            stringResource(R.string.custom_type_default_icon),
                            tint = sheetPrimary,
                        )
                    }
                }
            }

            Column(Modifier.fillMaxWidth()) {
                BasicText(
                    stringResource(R.string.workout_metric_options),
                    Modifier.semantics { heading() },
                    style = TextStyle(sheetSecondary, 14.sp, if (isP5) FontWeight.Bold else FontWeight.Normal),
                )
                val distanceLabel = stringResource(R.string.custom_type_has_distance)
                GlassToggle(
                    checked = hasDistance,
                    onCheckedChange = { if (!saving) hasDistance = it },
                    modifier = Modifier.fillMaxWidth().overlayInteraction(!saving),
                    label = distanceLabel,
                ) {
                    BasicText(distanceLabel, style = TextStyle(sheetPrimary, 16.sp))
                }
                val offeredMetrics = availableWorkoutMetrics(editingType?.builtinKey)
                val shownMetrics = WorkoutMetric.entries.filter { it in offeredMetrics || (metricMask and it.bit) != 0 }
                shownMetrics.forEach { metric ->
                    val label = stringResource(metric.labelId)
                    GlassToggle(
                        checked = (metricMask and metric.bit) != 0,
                        onCheckedChange = { checked ->
                            if (!saving) {
                                metricMask = if (checked) metricMask or metric.bit else metricMask and metric.bit.inv()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().overlayInteraction(!saving),
                        label = label,
                    ) {
                        BasicText(label, style = TextStyle(sheetPrimary, 16.sp))
                    }
                }
            }
            if (actions.state == AddTypeActions.State.Failed) {
                BasicText(
                    stringResource(R.string.record_save_failed),
                    Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    style = TextStyle(sheetPrimary, 14.sp),
                )
            }
            val saveBtnShape = if (isP5) RoundedCornerShape(2.dp) else RoundedCornerShape(12.dp)
            val saveBtnBg = if (isP5) {
                if (canSave) p5.brandFill.asColor() else if (t.dark) Color(0xFF26262C) else n.surfaceMuted.asColor()
            } else {
                if (canSave) t.accent.accentAction.asColor() else n.surfaceMuted.asColor()
            }
            val saveBtnBorder = if (isP5) {
                Modifier.border(
                    2.dp,
                    if (canSave) p5Stroke else p5Stroke.copy(alpha = 0.35f),
                    saveBtnShape,
                )
            } else {
                Modifier
            }
            val saveBtnText = if (isP5) {
                if (canSave) p5.onBrandFill.asColor() else n.textSecondary.asColor()
            } else {
                if (canSave) t.accent.onAccentAction.asColor() else n.textSecondary.asColor()
            }
            Box(
                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .clip(saveBtnShape)
                    .background(saveBtnBg, saveBtnShape)
                    .then(saveBtnBorder)
                    .clickable(enabled = canSave, role = Role.Button) {
                        focus.clearFocus()
                        val savedMetricMask = if (metricMask == initialMetricMask) editingType?.extraMetricMask else metricMask
                        actions.save(editingType?.id, savedName, hasDistance, iconKey, savedMetricMask)
                    }
                    .padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    stringResource(R.string.record_save),
                    style = TextStyle(saveBtnText, 16.sp, if (isP5) FontWeight.Black else FontWeight.Normal),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomTypeIconPicker(
    visible: Boolean,
    iconKey: String?,
    defaultType: ExerciseTypeEntity? = null,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
    visibility: MutableTransitionState<Boolean> = rememberOverlayVisibility(visible),
) {
    val t = LocalThemeTokens.current
    val n = t.neutrals
    val isP5 = LocalUiStyle.current == UiStyle.PERSONA_5
    val titleColor = n.textPrimary.asColor()
    val cancelColor = n.textSecondary.asColor()
    val title = stringResource(R.string.custom_type_choose_icon)
    GlassOverlayPanel(visible, onDismiss, PaddingValues(16.dp), title, visibility) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                title,
                modifier = Modifier.semantics { heading() },
                color = titleColor,
                fontWeight = if (isP5) FontWeight.Black else FontWeight.Medium,
            )
            FlowRow(
                Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (defaultType != null) {
                    TypeIconChoice(
                        exerciseTypeIcon(defaultType.copy(iconKey = null)),
                        stringResource(R.string.custom_type_default_icon),
                        iconKey == null,
                    ) { onSelect(null) }
                }
                for (icon in CUSTOM_TYPE_ICONS) {
                    TypeIconChoice(icon.vector, stringResource(icon.label), iconKey == icon.key) { onSelect(icon.key) }
                }
            }
            TextButton(
                onDismiss,
                Modifier.align(Alignment.End).heightIn(min = 48.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = cancelColor),
            ) { Text(stringResource(R.string.record_cancel), fontWeight = if (isP5) FontWeight.Bold else FontWeight.Normal) }
        }
    }
}

@Composable
private fun TypeIconChoice(icon: ImageVector, label: String, selected: Boolean, onSelect: () -> Unit) {
    val t = LocalThemeTokens.current
    val n = t.neutrals
    val a = t.accent
    val isP5 = LocalUiStyle.current == UiStyle.PERSONA_5
    val p5 = LocalP5Tokens.current
    val p5Stroke = if (t.dark) p5.strokeLight.asColor() else p5.strokeDark.asColor()
    val shape = if (isP5) RoundedCornerShape(2.dp) else RoundedCornerShape(12.dp)
    val bg = if (isP5) {
        if (selected) p5.brandFill.asColor() else if (t.dark) Color(0xFF1F1F24) else n.surfaceMuted.asColor()
    } else {
        if (selected) a.accentSubtle.asColor() else n.surfaceMuted.asColor()
    }
    val borderColor = if (isP5) {
        if (selected) p5Stroke else p5Stroke.copy(alpha = if (t.dark) 0.45f else 0.75f)
    } else {
        if (selected) a.accentAction.asColor() else n.divider.asColor()
    }
    val tint = if (isP5) {
        if (selected) p5.onBrandFill.asColor() else n.textPrimary.asColor()
    } else {
        n.textPrimary.asColor()
    }
    Box(
        Modifier.size(48.dp)
            .clip(shape)
            .background(bg, shape)
            .border(if (selected) 2.dp else if (isP5) 1.5.dp else 1.dp, borderColor, shape)
            .semantics { contentDescription = label }
            .selectable(selected, role = Role.RadioButton, onClick = onSelect),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tint)
    }
}
