// Adapted from Kyant0/AndroidLiquidGlass catalog (utils/DampedDragAnimation.kt), Apache-2.0, Copyright 2025 Kyant.
// Modified for Actiary: rewritten as a callback-free state holder, System.nanoTime instead of kotlin.time.Clock,
// pure math extracted for JVM tests. Spring and flex numbers (damping ratio / response seconds, 0.22 s minimum lift,
// tab tracking / resting springs, flex springs) follow the values documented by NEORUAA/Glasmio (Apache-2.0);
// numbers only, no code copied.
package com.actiary.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs

// Feel constants (user-visible): tune here. Springs are damping ratio + response (seconds); k = (2*pi/response)^2.
/** Max velocity stretch / squash while dragging (fraction, each axis). Glasmio allows 0.10-0.25 by size; kept small on purpose. */
internal const val DEFORM_LIMIT = 0.1f
/** A lifted thumb stays lifted at least this long from lift start, so short taps still show the lift. */
internal const val MIN_LIFT_SECONDS = 0.22f

internal class SpringParams(val damping: Float, val response: Float)

/** Stiffness (unit mass) of a spring with the given response time in seconds. */
fun stiffnessFromResponse(response: Float): Float {
    val w = (2.0 * PI / response).toFloat()
    return w * w
}

internal fun SpringParams.spec(threshold: Float): SpringSpec<Float> = spring(damping, stiffnessFromResponse(response), threshold)

// Large variant (tab bar thumb, segmented indicator): critical lift / unlift.
internal val SPRING_LARGE_LIFT = SpringParams(1f, 0.25f)
internal val SPRING_LARGE_UNLIFT = SpringParams(1f, 0.25f)
internal val SPRING_LARGE_TRACKING = SpringParams(0.85f, 0.20f)
internal val SPRING_LARGE_RESTING = SpringParams(0.85f, 0.40f)
internal val SPRING_LARGE_FLEX_TRACKING = SpringParams(0.90f, 0.50f)
internal val SPRING_LARGE_FLEX_RESTING = SpringParams(1f, 0.50f)

// Small variant (switch, slider thumb): bouncy, overshoot on unlift is kept.
internal val SPRING_SMALL_LIFT = SpringParams(0.625f, 0.27f)
internal val SPRING_SMALL_UNLIFT = SpringParams(0.7f, 0.5f)
internal val SPRING_SMALL_TRACKING = SpringParams(0.625f, 0.262f)
internal val SPRING_SMALL_RESTING = SpringParams(0.85f, 0.40f)
internal val SPRING_SMALL_FLEX_TRACKING = SpringParams(0.56f, 0.444f)
internal val SPRING_SMALL_FLEX_RESTING = SpringParams(0.56f, 0.444f)

internal class ThumbMotion(
    val lift: SpringParams,
    val unlift: SpringParams,
    val tracking: SpringParams,
    val resting: SpringParams,
    val flexTracking: SpringParams,
    val flexResting: SpringParams,
)

internal val LargeThumbMotion = ThumbMotion(
    SPRING_LARGE_LIFT, SPRING_LARGE_UNLIFT, SPRING_LARGE_TRACKING, SPRING_LARGE_RESTING,
    SPRING_LARGE_FLEX_TRACKING, SPRING_LARGE_FLEX_RESTING,
)
internal val SmallThumbMotion = ThumbMotion(
    SPRING_SMALL_LIFT, SPRING_SMALL_UNLIFT, SPRING_SMALL_TRACKING, SPRING_SMALL_RESTING,
    SPRING_SMALL_FLEX_TRACKING, SPRING_SMALL_FLEX_RESTING,
)

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
 * are the lift scale. Getters read Animatable state: call them from draw / graphicsLayer lambdas only.
 * Drag: [press] lifts, [updateValue] tracks, [animateToValue] settles (resting spring) and relaxes after the
 * minimum lift time. Tap: [tapTo] slides on the tracking spring and stays lifted through the travel.
 * A new lift cancels an unlift that has not finished.
 */
class PressDragState internal constructor(
    initial: Float,
    val range: ClosedFloatingPointRange<Float>,
    private val scope: CoroutineScope,
    private val pressedScale: Float,
    motion: ThumbMotion,
) {
    constructor(initial: Float, range: ClosedFloatingPointRange<Float>, scope: CoroutineScope, pressedScale: Float = 1f) :
        this(initial, range, scope, pressedScale, LargeThumbMotion)

    private val liftSpec = motion.lift.spec(0.001f)
    private val unliftSpec = motion.unlift.spec(0.001f)
    private val trackingSpec = motion.tracking.spec(VALUE_THRESHOLD)
    private val restingSpec = motion.resting.spec(VALUE_THRESHOLD)
    private val flexTrackingSpec = motion.flexTracking.spec(VALUE_THRESHOLD * 10f)
    private val flexRestingSpec = motion.flexResting.spec(VALUE_THRESHOLD * 10f)

    private val valueAnim = Animatable(initial, VALUE_THRESHOLD)
    private val velocityAnim = Animatable(0f, 5f)
    private val pressAnim = Animatable(0f, 0.001f)
    private val scaleXAnim = Animatable(1f, 0.001f)
    private val scaleYAnim = Animatable(1f, 0.001f)
    private val mutex = MutatorMutex()
    private val tracker = VelocityTracker()
    private var liftStartNanos = 0L
    private var unliftJob: Job? = null

    val value: Float get() = valueAnim.value
    val targetValue: Float get() = valueAnim.targetValue
    /** Clamped: the small unlift spring overshoots below 0. */
    val pressProgress: Float get() = pressAnim.value.coerceIn(0f, 1f)
    val scaleX: Float get() = scaleXAnim.value
    val scaleY: Float get() = scaleYAnim.value
    val velocity: Float get() = velocityAnim.value

    /** Velocity divided by 10, the scale [deformScale] expects. */
    val normalizedVelocity: Float get() = velocity / 10f

    private fun startLift() {
        unliftJob?.cancel()
        liftStartNanos = System.nanoTime()
        scope.launch {
            launch { pressAnim.animateTo(1f, liftSpec) }
            launch { scaleXAnim.animateTo(pressedScale, liftSpec) }
            launch { scaleYAnim.animateTo(pressedScale, liftSpec) }
        }
    }

    /** Finger down on the thumb: lift with glass progress. */
    fun press() {
        tracker.resetTracking()
        startLift()
    }

    /** Waits for the thumb to settle near its target and for the minimum lift time, then unlifts. */
    fun release(cancelled: Boolean = false) {
        unliftJob?.cancel()
        unliftJob = scope.launch {
            if (!cancelled) {
                withFrameNanos { }
                snapshotFlow { abs(value - targetValue) }.filter { it <= VALUE_THRESHOLD }.first()
                val remainingMs = (MIN_LIFT_SECONDS * 1000f).toLong() - (System.nanoTime() - liftStartNanos) / 1_000_000L
                if (remainingMs > 0) delay(remainingMs)
            }
            launch { pressAnim.animateTo(0f, unliftSpec) }
            launch { scaleXAnim.animateTo(1f, unliftSpec) }
            launch { scaleYAnim.animateTo(1f, unliftSpec) }
        }
    }

    /** Live value while dragging (tracking spring). */
    fun updateValue(v: Float) {
        val target = v.coerceIn(range)
        scope.launch { valueAnim.animateTo(target, trackingSpec) { trackVelocity(flexTrackingSpec) } }
    }

    /** Drag release: settle on the resting spring, then unlift (the drag already lifted). */
    suspend fun animateToValue(v: Float) {
        mutex.mutate {
            val lift = liftStartNanos
            coroutineScope {
                launch { valueAnim.animateTo(v.coerceIn(range), restingSpec) { trackVelocity(flexRestingSpec) } }
                if (velocity != 0f) launch { velocityAnim.animateTo(0f, flexRestingSpec) }
            }
            if (liftStartNanos == lift) release()
        }
    }

    /** Tap or outside selection change: tracking-spring slide with optional lift. */
    suspend fun tapTo(v: Float, lift: Boolean = true) {
        mutex.mutate {
            if (lift) press()
            val liftStartedAt = liftStartNanos
            coroutineScope {
                launch { valueAnim.animateTo(v.coerceIn(range), trackingSpec) { trackVelocity(flexTrackingSpec) } }
                if (velocity != 0f) launch { velocityAnim.animateTo(0f, flexRestingSpec) }
            }
            if (lift && liftStartNanos == liftStartedAt) release()
        }
    }

    /** Jump without animation (reduced motion / degraded). */
    suspend fun snapTo(v: Float) {
        valueAnim.snapTo(v.coerceIn(range))
    }

    private fun trackVelocity(spec: SpringSpec<Float>) {
        tracker.addPosition(System.nanoTime() / 1_000_000L, Offset(value, 0f))
        val target = tracker.calculateVelocity().x / (range.endInclusive - range.start).coerceAtLeast(1f)
        scope.launch { velocityAnim.animateTo(target, spec) }
    }

    private companion object {
        const val VALUE_THRESHOLD = 0.002f
    }
}
