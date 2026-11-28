package com.actiary.p5

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.actiary.R
import com.actiary.data.appearance.P5MotionLevel
import com.actiary.glass.GlassAnchoredMenuState
import com.actiary.glass.LocalGlassAnchoredMenu
import com.actiary.glass.OverlayBackHandler
import com.actiary.glass.glassMenuMaxHeight
import com.actiary.glass.overlayFadeIn
import com.actiary.glass.overlayFadeOut
import com.actiary.glass.overlayInteraction
import com.actiary.glass.overlayPresent
import com.actiary.glass.positionGlassMenu
import com.actiary.glass.rememberOverlayVisibility
import com.actiary.glass.sheetScrimAlpha
import com.actiary.glass.shouldDismissSheet
import com.actiary.theme.LocalP5Tokens
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor
import com.actiary.ui.motion.Motion
import com.actiary.ui.motion.PredictiveBack
import com.actiary.ui.motion.rememberMotionReduced
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val P5ScrimColor = Color.Black.copy(alpha = 0.56f)
private const val P5SheetBackShift = 0.12f
private const val P5SheetBackScale = 0.05f

private fun Modifier.p5Scrim(onTap: () -> Unit) =
    clickable(interactionSource = null, indication = null, onClick = onTap).clearAndSetSemantics {}

private fun Modifier.p5ModalPane(title: String) = semantics {
    if (title.isNotEmpty()) paneTitle = title
    isTraversalGroup = true
}

private fun Modifier.p5ConsumeTaps() = pointerInput(Unit) { detectTapGestures { } }

private fun p5OverlayEnter(still: Boolean): EnterTransition =
    if (still) EnterTransition.None else fadeIn(tween(140)) + scaleIn(
        initialScale = 0.92f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
    )

private fun p5OverlayExit(still: Boolean): ExitTransition =
    if (still) ExitTransition.None else fadeOut(tween(Motion.FADE_OUT_MS)) + scaleOut(
        targetScale = 0.94f,
        animationSpec = tween(Motion.FADE_OUT_MS),
    )

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun P5AnchoredMenu(
    state: GlassAnchoredMenuState,
    visibility: MutableTransitionState<Boolean>,
) {
    val request = state.request
    var retained by remember { mutableStateOf<GlassAnchoredMenuState.Request?>(null) }
    SideEffect {
        if (request != null) retained = request
        else if (!visibility.overlayPresent) retained = null
    }
    OverlayBackHandler(visibility, state::dismiss)
    val t = LocalThemeTokens.current
    val isLight = !t.dark
    val p5 = LocalP5Tokens.current
    val still = p5.motionLevel == P5MotionLevel.COMPACT || rememberMotionReduced()
    val insets = WindowInsets.safeDrawing

    AnimatedVisibility(visibility, enter = EnterTransition.None, exit = ExitTransition.None) {
        val shown = (request ?: retained) ?: return@AnimatedVisibility
        val bounds = shown.anchor
        val anchor = IntRect(
            bounds.left.roundToInt(),
            bounds.top.roundToInt(),
            bounds.right.roundToInt(),
            bounds.bottom.roundToInt(),
        )
        val requester = remember { FocusRequester() }
        val panelOpacity = transition.animateFloat(
            transitionSpec = {
                if (still) snap() else tween(
                    if (targetState == EnterExitState.Visible) Motion.FADE_IN_MS else Motion.FADE_OUT_MS,
                )
            },
            label = "P5 menu opacity",
        ) { if (it == EnterExitState.Visible) 1f else 0f }
        val panelScale = transition.animateFloat(
            transitionSpec = {
                if (still) snap()
                else if (targetState == EnterExitState.Visible) {
                    spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium)
                } else {
                    tween(Motion.FADE_OUT_MS)
                }
            },
            label = "P5 menu scale",
        ) { enterExit ->
            when (enterExit) {
                EnterExitState.PreEnter -> 0.88f
                EnterExitState.Visible -> 1f
                EnterExitState.PostExit -> 0.94f
            }
        }
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.matchParentSize().p5Scrim(state::dismiss))
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
                val opensBelow = (safe.bottom - (anchor.bottom + gap)) >= ((anchor.top - gap) - safe.top)
                val pivotX = if (layoutDirection == LayoutDirection.Ltr) 0.85f else 0.15f
                val pivotY = if (opensBelow) 0f else 1f
                val panel = subcompose(Unit) {
                    Column(
                        Modifier
                            .widthIn(min = 152.dp, max = 272.dp)
                            .width(IntrinsicSize.Max)
                            .graphicsLayer {
                                alpha = panelOpacity.value
                                if (!still) {
                                    scaleX = panelScale.value
                                    scaleY = panelScale.value
                                    transformOrigin = TransformOrigin(pivotX, pivotY)
                                }
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
                            .p5ModalPane("")
                            .p5Surface(
                                variant = P5ShapeVariant.CARD,
                                fillColor = if (isLight) p5.lightCard.asColor() else p5.darkCard.asColor(),
                                strokeColor = if (isLight) p5.strokeDark.asColor() else p5.strokeLight.asColor(),
                                shadowColor = if (isLight) p5.brandFill.asColor() else p5.shadowColor.asColor(),
                                strokeWidth = 2.dp,
                                shadowOffset = DpOffset(4.dp, 4.dp),
                            )
                            .p5ConsumeTaps()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 6.dp),
                    ) {
                        shown.items.forEachIndexed { index, item ->
                            val color = if (item.destructive) {
                                if (isLight) Color(0xFFB3261E) else Color(0xFFFF7A7A)
                            } else {
                                t.neutrals.textPrimary.asColor()
                            }
                            val action = {
                                state.dismiss()
                                item.onClick()
                            }
                            val select = if (item.selected == null) {
                                Modifier.clickable(role = Role.Button, onClick = action)
                            } else {
                                Modifier.selectable(item.selected, role = Role.RadioButton, onClick = action)
                            }
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .then(if (index == 0) Modifier.focusRequester(requester) else Modifier)
                                    .then(select)
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                item.icon?.let { Icon(it, null, Modifier.size(20.dp), tint = color) }
                                Text(
                                    text = item.label,
                                    modifier = Modifier.weight(1f),
                                    color = color,
                                    fontWeight = if (item.selected == true) FontWeight.Bold else FontWeight.Medium,
                                )
                                if (item.selected == true) {
                                    Icon(
                                        Icons.Outlined.Check,
                                        null,
                                        Modifier.size(20.dp),
                                        tint = t.accent.accentAction.asColor(),
                                    )
                                }
                            }
                        }
                    }
                    LaunchedEffect(request) {
                        if (request != null && shown.items.isNotEmpty()) requester.requestFocus()
                    }
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
fun P5Dialog(
    visible: Boolean,
    text: String,
    confirm: String,
    dismiss: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    visibility: MutableTransitionState<Boolean> = rememberOverlayVisibility(visible),
) {
    val t = LocalThemeTokens.current
    val isLight = !t.dark
    val p5 = LocalP5Tokens.current
    P5OverlayPanel(
        visible = visible,
        onDismiss = onDismiss,
        contentPadding = PaddingValues(start = 24.dp, top = 24.dp, end = 16.dp, bottom = 12.dp),
        title = text,
        visibility = visibility,
    ) {
        Text(
            text = text,
            color = t.neutrals.textPrimary.asColor(),
            modifier = Modifier.padding(end = 8.dp, bottom = 12.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = t.neutrals.textSecondary.asColor(),
                ),
            ) {
                Text(dismiss, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .p5Surface(
                        variant = P5ShapeVariant.BADGE,
                        fillColor = p5.brandFill.asColor(),
                        strokeColor = if (isLight) p5.strokeDark.asColor() else p5.strokeLight.asColor(),
                        shadowColor = p5.strokeDark.asColor(),
                        strokeWidth = 1.5.dp,
                        shadowOffset = DpOffset(2.dp, 2.dp),
                    )
                    .clickable(role = Role.Button, onClick = onConfirm)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = confirm,
                    color = p5.onBrandFill.asColor(),
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@Composable
fun P5OverlayPanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    contentPadding: PaddingValues,
    title: String = "",
    visibility: MutableTransitionState<Boolean> = rememberOverlayVisibility(visible),
    content: @Composable ColumnScope.() -> Unit,
) {
    OverlayBackHandler(visibility, onDismiss)
    val t = LocalThemeTokens.current
    val isLight = !t.dark
    val p5 = LocalP5Tokens.current
    val still = p5.motionLevel == P5MotionLevel.COMPACT || rememberMotionReduced()
    AnimatedVisibility(visibility, enter = EnterTransition.None, exit = ExitTransition.None) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .overlayInteraction(visible),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .animateEnterExit(overlayFadeIn(still), overlayFadeOut(still))
                    .background(P5ScrimColor)
                    .p5Scrim(onDismiss),
            )
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .widthIn(max = 360.dp)
                    .animateEnterExit(p5OverlayEnter(still), p5OverlayExit(still))
                    .p5ModalPane(title)
                    .p5Surface(
                        variant = P5ShapeVariant.DIALOG,
                        fillColor = if (isLight) p5.lightCard.asColor() else p5.darkCard.asColor(),
                        strokeColor = if (isLight) p5.strokeDark.asColor() else p5.strokeLight.asColor(),
                        shadowColor = if (isLight) p5.brandFill.asColor() else p5.shadowColor.asColor(),
                        strokeWidth = 2.5.dp,
                        shadowOffset = DpOffset(5.dp, 5.dp),
                    )
                    .p5ConsumeTaps()
                    .padding(contentPadding),
                content = content,
            )
        }
    }
}

@Composable
fun P5Sheet(
    visible: Boolean,
    onRequestClose: () -> Boolean,
    onExited: () -> Unit = {},
    title: String = "",
    covered: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val t = LocalThemeTokens.current
    val isLight = !t.dark
    val p5 = LocalP5Tokens.current
    val still = p5.motionLevel == P5MotionLevel.COMPACT || rememberMotionReduced()
    val exited by rememberUpdatedState(onExited)
    var entered by rememberSaveable { mutableStateOf(false) }
    var exitNotified by remember { mutableStateOf(false) }
    val openProgress = remember { Animatable(if (visible && entered) 1f else 0f) }

    LaunchedEffect(visible, still) {
        if (visible) {
            entered = true
            exitNotified = false
        }
        val target = if (visible) 1f else 0f
        if (still) openProgress.snapTo(target)
        else openProgress.animateTo(target, spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMedium))
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
            if (still) {
                offset = 0f
                backProgress = 0f
            } else {
                animate(
                    1f,
                    0f,
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMedium),
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
            if (interactive && !still) {
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

    val closeLabel = stringResource(R.string.overlay_close)
    val drag = rememberDraggableState { offset = (offset + it).coerceAtLeast(0f) }
    BoxWithConstraints(Modifier.fillMaxSize().overlayInteraction(interactive)) {
        val maxSheetHeight = maxHeight * 0.92f
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    val alpha = if (still) 1f else openProgress.value.coerceIn(0f, 1f) *
                        sheetScrimAlpha(offset + heightPx * P5SheetBackShift * backProgress, heightPx.toFloat())
                    drawRect(P5ScrimColor.copy(alpha = P5ScrimColor.alpha * alpha))
                }
                .p5Scrim { onRequestClose() },
        )
        Box(Modifier.align(Alignment.BottomCenter).imePadding()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxSheetHeight)
                    .onSizeChanged { heightPx = it.height }
                    .graphicsLayer {
                        translationY = if (!still) {
                            (1f - openProgress.value) * size.height +
                                offset + size.height * P5SheetBackShift * backProgress
                        } else {
                            offset
                        }
                        if (!still) {
                            scaleX = 1f - P5SheetBackScale * backProgress
                            scaleY = scaleX
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        }
                    }
                    .p5ModalPane(title)
                    .p5Surface(
                        variant = P5ShapeVariant.PANEL,
                        fillColor = if (isLight) p5.lightCard.asColor() else p5.darkCard.asColor(),
                        strokeColor = if (isLight) p5.strokeDark.asColor() else p5.strokeLight.asColor(),
                        shadowColor = if (isLight) p5.brandFill.asColor() else p5.shadowColor.asColor(),
                        strokeWidth = 2.5.dp,
                        shadowOffset = DpOffset(0.dp, -3.dp),
                    )
                    .p5ConsumeTaps(),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 32.dp)
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
                            drag,
                            Orientation.Vertical,
                            enabled = interactive,
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
                    Box(
                        Modifier
                            .size(width = 44.dp, height = 5.dp)
                            .clip(p5Shape(P5ShapeVariant.BADGE))
                            .background(p5.brandFill.asColor()),
                    )
                }
                content()
            }
        }
    }
}
