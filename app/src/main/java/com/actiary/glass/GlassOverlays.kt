package com.actiary.glass

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Overlays are in-tree (no Popup/Dialog windows) so they can sample the content layer.
// Place them in the GlassHost `overlay` slot, never inside the content layer.

private val ScrimColor = Color.Black.copy(alpha = 0.32f)

// Unlabeled to TalkBack: dismiss stays reachable through Back and the visible buttons.
private fun Modifier.scrim(onTap: () -> Unit) =
    clickable(interactionSource = null, indication = null, onClick = onTap).clearAndSetSemantics {}

private fun Modifier.modalPane(title: String) = semantics { paneTitle = title; isTraversalGroup = true }

private fun Modifier.consumeTaps() = pointerInput(Unit) { detectTapGestures { } }

@Composable
fun GlassMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    items: List<Pair<String, () -> Unit>>,
    modifier: Modifier = Modifier,
    anchor: Alignment = Alignment.TopEnd,
    panelPadding: PaddingValues = PaddingValues(),
) {
    BackHandler(expanded, onDismiss)
    val still = rememberAnimationsOff()
    AnimatedVisibility(
        expanded, modifier.fillMaxSize(),
        enter = if (still) EnterTransition.None else fadeIn(),
        exit = if (still) ExitTransition.None else fadeOut(),
    ) {
        Box(Modifier.fillMaxSize().scrim(onDismiss)) {
            val t = LocalThemeTokens.current
            Column(
                Modifier
                    .align(anchor).padding(panelPadding).padding(16.dp).widthIn(min = 180.dp)
                    .glassSurface(RoundedCornerShape(20.dp), GlassSampling.Content)
                    .background(t.neutrals.surface.asColor().copy(alpha = 0.72f), RoundedCornerShape(20.dp))
                    .consumeTaps()
                    .padding(vertical = 8.dp),
            ) {
                items.forEach { (label, action) ->
                    Box(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .clickable(role = Role.Button) { action() }.padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) { Text(label, color = t.neutrals.textPrimary.asColor()) }
                }
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
) {
    val t = LocalThemeTokens.current
    GlassOverlayPanel(visible, onDismiss, PaddingValues(start = 24.dp, top = 24.dp, end = 12.dp, bottom = 8.dp), text) {
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
    content: @Composable ColumnScope.() -> Unit,
) {
    BackHandler(visible, onDismiss)
    val still = rememberAnimationsOff()
    AnimatedVisibility(
        visible,
        enter = if (still) EnterTransition.None else fadeIn(),
        exit = if (still) ExitTransition.None else fadeOut(),
    ) {
        val t = LocalThemeTokens.current
        val shape = RoundedCornerShape(28.dp)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(Modifier.matchParentSize().background(ScrimColor).scrim(onDismiss))
            Column(
                Modifier
                    .padding(32.dp).widthIn(max = 360.dp)
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

/**
 * Closing is the caller's call: [onRequestClose] fires on scrim tap, back and a long enough downward drag.
 * The sheet owns imePadding; callers must not add it again. [onExited] fires after the exit animation when [visible] is false.
 */
@Composable
fun GlassSheet(
    visible: Boolean,
    onRequestClose: () -> Unit,
    onExited: () -> Unit = {},
    title: String = "",
    content: @Composable ColumnScope.() -> Unit,
) {
    BackHandler(visible, onRequestClose)
    val still = rememberAnimationsOff()
    val exited by rememberUpdatedState(onExited)
    // Slide-out is a ~300ms default transition; report once it has finished (immediately when animations are off).
    LaunchedEffect(visible) { if (!visible) { if (!still) delay(350); exited() } }
    val t = LocalThemeTokens.current
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible,
            enter = if (still) EnterTransition.None else fadeIn(),
            exit = if (still) ExitTransition.None else fadeOut(),
        ) { Box(Modifier.fillMaxSize().background(ScrimColor).scrim(onRequestClose)) }
        AnimatedVisibility(
            visible, Modifier.align(Alignment.BottomCenter).imePadding(),
            enter = if (still) EnterTransition.None else slideInVertically { it },
            exit = if (still) ExitTransition.None else slideOutVertically { it },
        ) {
            var offset by remember { mutableFloatStateOf(0f) }
            var heightPx by remember { mutableIntStateOf(0) }
            val scope = rememberCoroutineScope()
            val stillOpen by rememberUpdatedState(visible)
            val drag = rememberDraggableState { offset = (offset + it).coerceAtLeast(0f) }
            Column(
                Modifier
                    .fillMaxWidth().heightIn(max = maxHeight * 0.92f)
                    .onSizeChanged { heightPx = it.height }
                    .offset { IntOffset(0, offset.roundToInt()) }
                    .modalPane(title)
                    .glassSurface(shape, GlassSampling.Content)
                    .background(t.neutrals.surface.asColor().copy(alpha = 0.72f), shape)
                    .consumeTaps(),
            ) {
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 32.dp).draggable(
                        drag, Orientation.Vertical,
                        onDragStopped = { velocity ->
                            val close = offset > heightPx / 3f || velocity > 1500f
                            scope.launch {
                                if (close) {
                                    onRequestClose()
                                    androidx.compose.runtime.withFrameNanos { }
                                }
                                if (!close || stillOpen) animate(offset, 0f, animationSpec = spring()) { v, _ -> offset = v }
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
