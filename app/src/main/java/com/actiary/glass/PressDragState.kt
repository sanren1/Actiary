// Adapted from Kyant0/AndroidLiquidGlass catalog (utils/DampedDragAnimation.kt), Apache-2.0, Copyright 2025 Kyant.
// Modified for Actiary: rewritten as a callback-free state holder, System.nanoTime instead of kotlin.time.Clock,
// pure math extracted for JVM tests.
package com.actiary.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

// Feel constants (user-visible): tune here.
/** Max velocity stretch / squash while dragging (fraction, each axis). */
internal const val DEFORM_LIMIT = 0.1f
/** Tap slide spring: fast and nearly critically damped (~150-200 ms), no press bounce. */
internal const val TAP_DAMPING = 0.95f
internal const val TAP_STIFFNESS = 2000f

/** Velocity deformation factors (x, y) for an already normalised velocity; each deviation is capped at [DEFORM_LIMIT]. */
fun deformScale(velocity: Float): Pair<Float, Float> {
    val x = 1f - (velocity * 0.75f).coerceIn(-DEFORM_LIMIT, DEFORM_LIMIT)
    val y = 1f - (velocity * 0.25f).coerceIn(-DEFORM_LIMIT, DEFORM_LIMIT)
    return 1f / x to y
}

/** Option cell containing [offsetPx] when [widthPx] is split into [count] equal cells; clamped to the valid range. */
fun indexAt(offsetPx: Float, widthPx: Float, count: Int): Int {
    if (count <= 1 || widthPx <= 0f) return 0
    return Math.floor((offsetPx / (widthPx / count)).toDouble()).toInt().coerceIn(0, count - 1)
}

/** Thumb value (in option units) for [index]. */
fun valueFor(index: Int, count: Int): Float = index.coerceIn(0, (count - 1).coerceAtLeast(0)).toFloat()

/**
 * Drives a draggable thumb: [value] follows a spring, [pressProgress] is 0..1 while pressed, [scaleX]/[scaleY]
 * are the pressed bounce. Getters read Animatable state: call them from draw / graphicsLayer lambdas only.
 */
class PressDragState(
    initial: Float,
    val range: ClosedFloatingPointRange<Float>,
    private val scope: CoroutineScope,
    private val pressedScale: Float = 1f,
) {
    private val valueSpec = spring<Float>(1f, 1000f, VALUE_THRESHOLD)
    private val velocitySpec = spring<Float>(0.5f, 300f, VALUE_THRESHOLD * 10f)
    private val tapSpec = spring<Float>(TAP_DAMPING, TAP_STIFFNESS, VALUE_THRESHOLD)
    private val pressSpec = spring<Float>(1f, 1000f, 0.001f)
    private val scaleXSpec = spring<Float>(0.6f, 250f, 0.001f)
    private val scaleYSpec = spring<Float>(0.7f, 250f, 0.001f)

    private val valueAnim = Animatable(initial, VALUE_THRESHOLD)
    private val velocityAnim = Animatable(0f, 5f)
    private val pressAnim = Animatable(0f, 0.001f)
    private val scaleXAnim = Animatable(1f, 0.001f)
    private val scaleYAnim = Animatable(1f, 0.001f)
    private val mutex = MutatorMutex()
    private val tracker = VelocityTracker()

    val value: Float get() = valueAnim.value
    val targetValue: Float get() = valueAnim.targetValue
    val pressProgress: Float get() = pressAnim.value
    val scaleX: Float get() = scaleXAnim.value
    val scaleY: Float get() = scaleYAnim.value
    val velocity: Float get() = velocityAnim.value

    /** Velocity divided by 10, the scale [deformScale] expects. */
    val normalizedVelocity: Float get() = velocity / 10f

    fun press() {
        tracker.resetTracking()
        scope.launch {
            launch { pressAnim.animateTo(1f, pressSpec) }
            launch { scaleXAnim.animateTo(pressedScale, scaleXSpec) }
            launch { scaleYAnim.animateTo(pressedScale, scaleYSpec) }
        }
    }

    /** Waits for the thumb to settle near its target, then relaxes the press. */
    fun release() {
        scope.launch {
            withFrameNanos { }
            if (value != targetValue) {
                val threshold = (range.endInclusive - range.start) * 0.025f
                snapshotFlow { valueAnim.value }.filter { abs(it - valueAnim.targetValue) < threshold }.first()
            }
            launch { pressAnim.animateTo(0f, pressSpec) }
            launch { scaleXAnim.animateTo(1f, scaleXSpec) }
            launch { scaleYAnim.animateTo(1f, scaleYSpec) }
        }
    }

    /** Live value while dragging. */
    fun updateValue(v: Float) {
        val target = v.coerceIn(range)
        scope.launch { valueAnim.animateTo(target, valueSpec) { trackVelocity() } }
    }

    /** Animated move with a press bounce, e.g. after a tap or when the selection changes from outside. */
    suspend fun animateToValue(v: Float) {
        mutex.mutate {
            press()
            coroutineScope {
                launch { valueAnim.animateTo(v.coerceIn(range), valueSpec) { trackVelocity() } }
                if (velocity != 0f) launch { velocityAnim.animateTo(0f, velocitySpec) }
            }
            release()
        }
    }

    /** Plain fast slide for a tap or an outside selection change: no press, no glass. */
    suspend fun tapTo(v: Float) {
        mutex.mutate {
            coroutineScope {
                launch { valueAnim.animateTo(v.coerceIn(range), tapSpec) { trackVelocity() } }
                if (velocity != 0f) launch { velocityAnim.animateTo(0f, velocitySpec) }
            }
            release() // relaxes a press that a preempted animateToValue left behind
        }
    }

    /** Jump without animation (reduced motion / degraded). */
    suspend fun snapTo(v: Float) {
        valueAnim.snapTo(v.coerceIn(range))
    }

    private fun trackVelocity() {
        tracker.addPosition(System.nanoTime() / 1_000_000L, Offset(value, 0f))
        val target = tracker.calculateVelocity().x / (range.endInclusive - range.start)
        scope.launch { velocityAnim.animateTo(target, velocitySpec) }
    }

    private companion object {
        const val VALUE_THRESHOLD = 0.002f
    }
}
