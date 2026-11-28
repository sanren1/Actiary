package com.actiary.glass

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.actiary.data.appearance.UiStyle
import com.actiary.p5.P5Toggle
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.LocalUiStyle
import com.actiary.theme.ThemeTokens
import com.actiary.theme.asColor
import com.actiary.theme.contrast
import com.actiary.theme.mix
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.isRuntimeShaderSupported
import kotlinx.coroutines.launch
import kotlin.math.abs

private val ToggleTrackWidth = 64.dp
private val ToggleThumbWidth = 40.dp
private val ToggleThumbInset = 2.dp
private val ToggleTravel = ToggleTrackWidth - ToggleThumbWidth - ToggleThumbInset * 2
private const val ToggleLiftScale = 1.28f
private const val ToggleFlingThreshold = 5f // Thumb travel lengths per second.
private const val ToggleFlingPauseMs = 40L // A held position ends the fling.

internal fun toggleTarget(value: Float, velocity: Float): Boolean =
    if (abs(velocity) >= ToggleFlingThreshold) velocity > 0f else value > 0.5f

internal fun trackColorFraction(value: Float): Float = value.coerceIn(0f, 1f)

internal fun toggleGlassEnabled(degraded: Boolean, animationsOff: Boolean, shaderSupported: Boolean): Boolean =
    !degraded && !animationsOff && shaderSupported

internal fun toggleTrackColor(tokens: ThemeTokens, checked: Boolean): Int {
    val neutrals = tokens.neutrals
    return when {
        checked -> tokens.accent.accentAction
        tokens.dark -> neutrals.surfaceMuted
        else -> mix(neutrals.surfaceMuted, neutrals.textSecondary, 0.80)
    }
}

internal fun toggleThumbRestingColor(tokens: ThemeTokens, trackColor: Color): Color =
    if (tokens.dark && contrast(Color.White.toArgb(), trackColor.toArgb()) < 3.0) {
        tokens.accent.onAccentAction.asColor()
    } else Color.White

/** [label] names one Switch node. Optional [labelContent] preserves the full row as a tap target. */
@Composable
fun GlassToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: String,
    labelContent: (@Composable () -> Unit)? = null,
) {
    if (LocalUiStyle.current == UiStyle.PERSONA_5) {
        P5Toggle(checked, onCheckedChange, modifier, label, labelContent)
        return
    }
    val tokens = LocalThemeTokens.current
    val profile = LocalGlassProfile.current
    val animationsOff = rememberAnimationsOff()
    val still = profile.degraded || animationsOff
    val enhance = toggleGlassEnabled(profile.degraded, animationsOff, isRuntimeShaderSupported())
    val indication = if (enhance) null else LocalIndication.current
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Ltr) 1f else -1f
    val scope = rememberCoroutineScope()
    val checkedNow by rememberUpdatedState(checked)
    val onCheckedChangeNow by rememberUpdatedState(onCheckedChange)
    val anchor = if (checked) 1f else 0f
    val state = remember(still) { PressDragState(anchor, 0f..1f, scope, ToggleLiftScale, SmallThumbMotion) }
    val interactive by remember(state) { derivedStateOf { isInteracting(state.pressProgress, state.scaleX, state.scaleY) } }
    val glassOn = enhance && interactive
    var dragging by remember(state) { mutableStateOf(false) }

    LaunchedEffect(checked, state) {
        if (!dragging && state.targetValue != anchor) {
            if (still) state.snapTo(anchor) else state.tapTo(anchor, lift = false)
        }
    }

    Row(
        modifier
            .heightIn(min = 48.dp)
            .glassControlFocus(enhance, RoundedCornerShape(12.dp))
            .toggleable(value = checked, interactionSource = null, indication = indication, role = Role.Switch) { next ->
                // Keyboard, accessibility and a short touch all use the same lift and state change.
                if (enhance) {
                    state.press()
                    state.release()
                }
                onCheckedChange(next)
            }
            .semantics { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (labelContent != null) {
            Box(Modifier.weight(1f).clearAndSetSemantics {}, content = { labelContent() })
        }
        Box(
            Modifier.size(ToggleTrackWidth, 48.dp).pointerInput(state, direction, enhance) {
                val travel = ToggleTravel.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (enhance) state.press()
                    var preview = trackColorFraction(state.value)
                    var settling = false
                    fun move(dx: Float) {
                        preview = trackColorFraction(preview + dx / travel * direction)
                        val position = preview
                        if (still) scope.launch { state.snapTo(position) } else state.updateValue(position)
                    }
                    try {
                        // Leave taps to the enclosing toggleable and vertical drags to the page.
                        val start = awaitTouchSlopOrCancellation(down.id) { change, overSlop ->
                            if (abs(overSlop.x) > abs(overSlop.y)) {
                                dragging = true
                                change.consume()
                                move(overSlop.x)
                            }
                        } ?: return@awaitEachGesture
                        val velocity = VelocityTracker().apply {
                            addPointerInputChange(down)
                            addPointerInputChange(start)
                        }
                        var lastMoveAt = start.uptimeMillis
                        val completed = horizontalDrag(start.id) { change ->
                            velocity.addPointerInputChange(change)
                            lastMoveAt = change.uptimeMillis
                            move(change.positionChange().x)
                            change.consume()
                        }
                        val releasedAt = currentEvent.changes.first().uptimeMillis
                        val speed = if (releasedAt - lastMoveAt <= ToggleFlingPauseMs) {
                            velocity.calculateVelocity().x / travel * direction
                        } else 0f
                        val next = if (completed) toggleTarget(preview, speed) else checkedNow
                        val target = if (next) 1f else 0f
                        scope.launch {
                            if (still) state.snapTo(target) else state.animateToValue(target)
                        }
                        settling = true
                        if (next != checkedNow) onCheckedChangeNow(next)
                    } finally {
                        dragging = false
                        if (enhance && !settling) state.release(cancelled = true)
                    }
                }
            },
            contentAlignment = Alignment.Center,
        ) {
            // Record just the track under the lens, as in AndroidLiquidGlass's LiquidSlider.
            val trackBackdrop = if (glassOn) rememberGlassLayer() else null
            val offColor = toggleTrackColor(tokens, checked = false).asColor()
            val onColor = toggleTrackColor(tokens, checked = true).asColor()
            val trackColor = remember(state, offColor, onColor) {
                derivedStateOf { lerp(offColor, onColor, trackColorFraction(state.value)) }
            }
            val thumbRestingColor by remember(trackColor, tokens) {
                derivedStateOf(structuralEqualityPolicy()) { toggleThumbRestingColor(tokens, trackColor.value) }
            }
            Box(
                Modifier.size(ToggleTrackWidth, 28.dp)
                    .then(if (trackBackdrop != null) Modifier.layerBackdrop(trackBackdrop) else Modifier)
                    .drawBehind {
                        drawRoundRect(trackColor.value, cornerRadius = CornerRadius(size.height / 2f))
                    },
            )

            val scale: GraphicsLayerScope.() -> Unit = {
                val (x, y) = deformScale(state.normalizedVelocity * direction)
                scaleX = state.scaleX * x
                scaleY = state.scaleY * y
            }
            Box(
                Modifier.align(Alignment.CenterStart).padding(start = ToggleThumbInset)
                    .graphicsLayer {
                        translationX = ToggleTravel.toPx() * trackColorFraction(state.value) * direction
                    }
                    .drawBehind {
                        val rest = if (glassOn) 1f - state.pressProgress else 1f
                        drawRoundRect(Color.Black, Offset(0f, 1.dp.toPx()), cornerRadius = CornerRadius(size.height / 2f), alpha = 0.12f * rest)
                    }
                    .then(
                        if (trackBackdrop != null) Modifier.glass(
                            trackBackdrop, CircleShape, profile, tokens.dark,
                            press = { state.pressProgress }, layerBlock = scale, thumbRestingColor = thumbRestingColor,
                        ) else Modifier.drawBehind { drawRoundRect(thumbRestingColor, cornerRadius = CornerRadius(size.height / 2f)) },
                    )
                    .size(ToggleThumbWidth, 24.dp),
            )
        }
    }
}
