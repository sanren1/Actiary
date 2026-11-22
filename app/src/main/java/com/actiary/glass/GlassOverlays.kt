package com.actiary.glass

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.actiary.R
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor
import com.actiary.ui.motion.Motion
import com.actiary.ui.motion.PredictiveBack
import com.actiary.ui.motion.rememberMotionReduced
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Overlays stay in the GlassHost overlay slot so they can sample the content layer.
private val ScrimColor = Color.Black.copy(alpha = 0.32f)
private const val SheetBackShift = 0.12f
private const val SheetBackScale = 0.05f

@Composable
internal fun rememberOverlayVisibility(visible: Boolean): MutableTransitionState<Boolean> =
    remember { MutableTransitionState(false) }.apply { targetState = visible }

internal val MutableTransitionState<Boolean>.overlayPresent: Boolean
    get() = currentState || targetState || !isIdle

// Unlabeled for TalkBack: the panel exposes its own dismiss affordance.
private fun Modifier.scrim(onTap: () -> Unit) =
    clickable(interactionSource = null, indication = null, onClick = onTap).clearAndSetSemantics {}

private fun Modifier.modalPane(title: String) = semantics {
    if (title.isNotEmpty()) paneTitle = title
    isTraversalGroup = true
}

private fun Modifier.consumeTaps() = pointerInput(Unit) { detectTapGestures { } }

internal fun Modifier.overlayInteraction(enabled: Boolean): Modifier =
    if (enabled) this else clearAndSetSemantics { hideFromAccessibility() }
        .onPreviewKeyEvent { true }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
            }
        }

@Composable
internal fun OverlayBackHandler(visibility: MutableTransitionState<Boolean>, onDismiss: () -> Unit) {
    BackHandler(visibility.overlayPresent) {
        if (visibility.targetState) onDismiss()
    }
}

internal fun overlayFadeIn(reduced: Boolean): EnterTransition =
    if (reduced) EnterTransition.None else fadeIn(tween(Motion.FADE_IN_MS))

internal fun overlayFadeOut(reduced: Boolean): ExitTransition =
    if (reduced) ExitTransition.None else fadeOut(tween(Motion.FADE_OUT_MS))

internal fun overlayEnter(reduced: Boolean): EnterTransition =
    if (reduced) EnterTransition.None else overlayFadeIn(false) +
        scaleIn(initialScale = Motion.DIALOG_SCALE, animationSpec = Motion.pushSpec(Motion.RELEASE_DAMPING))

internal fun overlayExit(reduced: Boolean): ExitTransition =
    if (reduced) ExitTransition.None else overlayFadeOut(false) +
        scaleOut(targetScale = Motion.DIALOG_SCALE, animationSpec = Motion.pushSpec(Motion.RELEASE_DAMPING))

internal fun glassMenuMaxHeight(anchor: IntRect, safe: IntRect, gap: Int): Int =
    maxOf(
        safe.bottom - (anchor.bottom + gap).coerceIn(safe.top, safe.bottom),
        (anchor.top - gap).coerceIn(safe.top, safe.bottom) - safe.top,
    )

internal fun positionGlassMenu(
    anchor: IntRect,
    safe: IntRect,
    size: IntSize,
    gap: Int,
    ltr: Boolean,
): IntOffset {
    val below = (anchor.bottom + gap).coerceIn(safe.top, safe.bottom)
    val above = (anchor.top - gap).coerceIn(safe.top, safe.bottom) - size.height
    val x = if (ltr) anchor.right - size.width else anchor.left
    val y = if (below + size.height <= safe.bottom) below else above
    return IntOffset(
        x.coerceIn(safe.left, safe.right - size.width),
        y.coerceIn(safe.top, safe.bottom - size.height),
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun GlassAnchoredMenu(state: GlassAnchoredMenuState, visibility: MutableTransitionState<Boolean>) {
    val request = state.request
    var retained by remember { mutableStateOf<GlassAnchoredMenuState.Request?>(null) }
    SideEffect {
        if (request != null) retained = request
        else if (!visibility.overlayPresent) retained = null
    }
    OverlayBackHandler(visibility, state::dismiss)
    val reduced = rememberMotionReduced()
    val insets = WindowInsets.safeDrawing
    AnimatedVisibility(visibility, enter = EnterTransition.None, exit = ExitTransition.None) {
        val shown = (request ?: retained) ?: return@AnimatedVisibility
        val bounds = shown.anchor
        val anchor = IntRect(bounds.left.roundToInt(), bounds.top.roundToInt(), bounds.right.roundToInt(), bounds.bottom.roundToInt())
        val requester = remember { FocusRequester() }
        val tokens = LocalThemeTokens.current
        val profile = LocalGlassProfile.current
        val backdrop = LocalGlassLayers.current.content
        val menuShape = RoundedCornerShape(20.dp)
        val panelOpacity = transition.animateFloat(
            transitionSpec = {
                if (reduced) snap() else tween(
                    if (targetState == EnterExitState.Visible) Motion.FADE_IN_MS else Motion.FADE_OUT_MS,
                )
            },
            label = "Menu opacity",
        ) { if (it == EnterExitState.Visible) 1f else 0f }
        Box(Modifier.fillMaxSize()) {
            // Keep catching outside touches through the exit transition.
            Box(Modifier.matchParentSize().scrim(state::dismiss))
            SubcomposeLayout(Modifier.matchParentSize()) { constraints ->
                val margin = 8.dp.roundToPx()
                val width = constraints.maxWidth
                val height = constraints.maxHeight
                val left = (insets.getLeft(this, layoutDirection) + margin).coerceAtMost(width)
                val top = (insets.getTop(this) + margin).coerceAtMost(height)
                val safe = IntRect(
                    left, top,
                    (width - insets.getRight(this, layoutDirection) - margin).coerceAtLeast(left),
                    (height - insets.getBottom(this) - margin).coerceAtLeast(top),
                )
                val gap = 4.dp.roundToPx()
                val maxHeight = minOf(glassMenuMaxHeight(anchor, safe, gap), 320.dp.roundToPx())
                if (safe.width == 0 || maxHeight == 0) return@SubcomposeLayout layout(width, height) {}
                val panel = subcompose(Unit) {
                    Column(
                        Modifier.widthIn(min = 152.dp, max = 272.dp).width(IntrinsicSize.Max)
                            // Fade the rounded surface with its outline shadow at the anchor.
                            .graphicsLayer {
                                shape = menuShape
                                clip = true
                                alpha = panelOpacity.value
                                shadowElevation = if (profile.degraded) 0f else 8.dp.toPx()
                            }
                            .overlayInteraction(request != null)
                            .focusProperties { onExit = { cancelFocusChange() } }
                            .focusGroup()
                            .onPreviewKeyEvent {
                                if (it.key == Key.Escape && it.type == KeyEventType.KeyDown) {
                                    state.dismiss()
                                    true
                                } else false
                            }
                            .modalPane("")
                            .glass(backdrop, menuShape, profile, tokens.dark, flat = true)
                            .background(tokens.neutrals.surface.asColor().copy(alpha = 0.62f), menuShape)
                            .consumeTaps()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 6.dp),
                    ) {
                        shown.items.forEachIndexed { index, item ->
                            val color = when {
                                !item.destructive -> tokens.neutrals.textPrimary.asColor()
                                tokens.dark -> Color(0xFFFFB4AB)
                                else -> Color(0xFFA12721)
                            }
                            val action = { state.dismiss(); item.onClick() }
                            val select = if (item.selected == null) Modifier.clickable(role = Role.Button, onClick = action)
                                else Modifier.selectable(item.selected, role = Role.RadioButton, onClick = action)
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                    .then(if (index == 0) Modifier.focusRequester(requester) else Modifier)
                                    .then(select)
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                item.icon?.let { Icon(it, null, Modifier.size(20.dp), tint = color) }
                                Text(item.label, Modifier.weight(1f), color = color)
                                if (item.selected == true) {
                                    Icon(
                                        Icons.Outlined.Check, null, Modifier.size(20.dp),
                                        tint = tokens.accent.accentStroke.asColor(),
                                    )
                                }
                            }
                        }
                    }
                    LaunchedEffect(request) { if (request != null && shown.items.isNotEmpty()) requester.requestFocus() }
                }.single().measure(Constraints(maxWidth = safe.width, maxHeight = maxHeight))
                val offset = positionGlassMenu(
                    anchor, safe, IntSize(panel.width, panel.height), gap, layoutDirection == LayoutDirection.Ltr,
                )
                layout(width, height) { panel.place(offset.x, offset.y) }
            }
        }
    }
}

@Composable
fun GlassDialog(
    visible: Boolean,
    text: String,
    confirm: String,
    dismiss: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    visibility: MutableTransitionState<Boolean> = rememberOverlayVisibility(visible),
) {
    val t = LocalThemeTokens.current
    GlassOverlayPanel(
        visible, onDismiss, PaddingValues(start = 24.dp, top = 24.dp, end = 12.dp, bottom = 8.dp), text, visibility,
    ) {
        Text(text, color = t.neutrals.textPrimary.asColor(), modifier = Modifier.padding(end = 12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onDismiss, Modifier.heightIn(min = 48.dp), colors = ButtonDefaults.textButtonColors(contentColor = t.neutrals.textSecondary.asColor())) { Text(dismiss) }
            TextButton(onConfirm, Modifier.heightIn(min = 48.dp), colors = ButtonDefaults.textButtonColors(contentColor = t.accent.accentAction.asColor())) { Text(confirm) }
        }
    }
}

/** Centered glass panel over a scrim; back and scrim tap call [onDismiss]. */
@Composable
fun GlassOverlayPanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    contentPadding: PaddingValues,
    title: String = "",
    visibility: MutableTransitionState<Boolean> = rememberOverlayVisibility(visible),
    content: @Composable ColumnScope.() -> Unit,
) {
    OverlayBackHandler(visibility, onDismiss)
    val reduced = rememberMotionReduced()
    AnimatedVisibility(visibility, enter = EnterTransition.None, exit = ExitTransition.None) {
        val t = LocalThemeTokens.current
        val shape = RoundedCornerShape(28.dp)
        Box(Modifier.fillMaxSize().overlayInteraction(visible), contentAlignment = Alignment.Center) {
            Box(
                Modifier.matchParentSize()
                    .animateEnterExit(overlayFadeIn(reduced), overlayFadeOut(reduced))
                    .background(ScrimColor).scrim(onDismiss),
            )
            Column(
                Modifier
                    .padding(32.dp).widthIn(max = 360.dp)
                    .animateEnterExit(overlayEnter(reduced), overlayExit(reduced))
                    .modalPane(title)
                    .glassSurface(shape, GlassSampling.Content)
                    .background(t.neutrals.surface.asColor().copy(alpha = 0.72f), shape)
                    .consumeTaps()
                    .padding(contentPadding),
                content = content,
            )
        }
    }
}

/** [onRequestClose] reports whether an exit started. [onExited] runs when it finishes. */
@Composable
fun GlassSheet(
    visible: Boolean,
    onRequestClose: () -> Boolean,
    onExited: () -> Unit = {},
    title: String = "",
    covered: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val reduced = rememberMotionReduced()
    val exited by rememberUpdatedState(onExited)
    var entered by rememberSaveable { mutableStateOf(false) }
    var exitNotified by remember { mutableStateOf(false) }
    val openProgress = remember { Animatable(if (visible && entered) 1f else 0f) }
    LaunchedEffect(visible, reduced) {
        if (visible) {
            entered = true
            exitNotified = false
        }
        val target = if (visible) 1f else 0f
        if (reduced) openProgress.snapTo(target)
        else openProgress.animateTo(target, Motion.pushSpec())
        if (!visible && !exitNotified) {
            exitNotified = true
            exited()
        }
    }

    val effectiveCovered = covered || LocalGlassAnchoredMenu.current.present
    val present = visible || !exitNotified
    val interactive = visible && !effectiveCovered
    var offset by remember { mutableFloatStateOf(0f) }
    var backProgress by remember { mutableFloatStateOf(0f) }
    var heightPx by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    var settle by remember { mutableStateOf<Job?>(null) }

    fun returnToRest() {
        settle?.cancel()
        settle = scope.launch {
            val dragOffset = offset
            val predictiveProgress = backProgress
            if (reduced) {
                offset = 0f
                backProgress = 0f
            } else {
                animate(
                    1f, 0f, animationSpec = Motion.pushSpec(Motion.RELEASE_DAMPING, Motion.RELEASE_RESPONSE),
                ) { fraction, _ ->
                    offset = dragOffset * fraction
                    backProgress = predictiveProgress * fraction
                }
            }
        }
    }

    PredictiveBack(
        enabled = present && !effectiveCovered,
        onProgress = {
            if (interactive && !reduced) {
                settle?.cancel()
                backProgress = it
            }
        },
        onCommit = {
            if (interactive && !onRequestClose()) returnToRest()
        },
        onCancel = { if (visible) returnToRest() },
    )
    if (!present) return

    val t = LocalThemeTokens.current
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val closeLabel = stringResource(R.string.overlay_close)
    val drag = rememberDraggableState { offset = (offset + it).coerceAtLeast(0f) }
    BoxWithConstraints(Modifier.fillMaxSize().overlayInteraction(interactive)) {
        val maxSheetHeight = maxHeight * 0.92f
        Box(
            Modifier.fillMaxSize().drawBehind {
                val alpha = if (reduced) 1f else openProgress.value.coerceIn(0f, 1f) *
                    sheetScrimAlpha(offset + heightPx * SheetBackShift * backProgress, heightPx.toFloat())
                drawRect(ScrimColor.copy(alpha = ScrimColor.alpha * alpha))
            }.scrim { onRequestClose() },
        )
        Box(Modifier.align(Alignment.BottomCenter).imePadding()) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = maxSheetHeight)
                    .onSizeChanged { heightPx = it.height }
                    .graphicsLayer {
                        translationY = if (!reduced) {
                            (1f - openProgress.value) * size.height +
                                offset + size.height * SheetBackShift * backProgress
                        } else {
                            offset
                        }
                        if (!reduced) {
                            scaleX = 1f - SheetBackScale * backProgress
                            scaleY = scaleX
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        }
                    }
                    .modalPane(title)
                    .glassSurface(shape, GlassSampling.Content)
                    .background(t.neutrals.surface.asColor().copy(alpha = 0.72f), shape)
                    .consumeTaps(),
            ) {
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 32.dp)
                        .semantics {
                            if (interactive) {
                                contentDescription = closeLabel
                                customActions = listOf(CustomAccessibilityAction(closeLabel) {
                                    onRequestClose()
                                    true
                                })
                            }
                        }
                        .draggable(
                            drag, Orientation.Vertical, enabled = interactive,
                            onDragStarted = { settle?.cancel() },
                            onDragStopped = { velocity ->
                                if (interactive) {
                                    val dismiss = shouldDismissSheet(offset, heightPx.toFloat(), velocity)
                                    if (!dismiss || !onRequestClose()) returnToRest()
                                }
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(t.neutrals.divider.asColor()))
                }
                content()
            }
        }
    }
}
