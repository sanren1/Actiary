package com.actiary.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.zIndex
import com.actiary.ui.settings.SettingsPage
import com.actiary.ui.settings.parent

internal sealed interface PushState {
    val top: SettingsPage

    data class Idle(override val top: SettingsPage) : PushState

    class Animating(
        val lower: SettingsPage,
        val upper: SettingsPage,
        override val top: SettingsPage,
        val startCoverage: Float,
        val releasing: Boolean = false,
    ) : PushState

    data class Dragging(
        val lower: SettingsPage,
        override val top: SettingsPage,
        val startCoverage: Float,
        val progress: Float,
    ) : PushState {
        val coverage: Float get() = startCoverage * (1f - progress)
    }
}

internal fun PushState.target(to: SettingsPage, coverage: Float? = null): PushState = when {
    top == to -> this
    this is PushState.Animating && (to == lower || to == upper) ->
        PushState.Animating(lower, upper, to, coverage ?: startCoverage)
    this is PushState.Dragging && to == lower -> settle(commit = true, coverage = coverage ?: this.coverage)
    to.parent() == top -> PushState.Animating(top, to, to, 0f)
    else -> PushState.Animating(to, top, to, 1f)
}

internal fun PushState.dragTo(progress: Float, coverage: Float = 1f): PushState {
    if (this is PushState.Dragging) return copy(progress = progress)
    val parent = top.parent() ?: return this
    val start = if (this is PushState.Animating && upper == top) coverage else 1f
    return PushState.Dragging(parent, top, start, progress)
}

internal fun PushState.settle(commit: Boolean, coverage: Float? = null): PushState {
    if (this !is PushState.Dragging) return this
    return PushState.Animating(lower, top, if (commit) lower else top, coverage ?: this.coverage, releasing = true)
}

/** A coverage of 0 exposes the parent and 1 covers it with the child. */
internal class PushMotion(initial: SettingsPage) {
    var state by mutableStateOf<PushState>(PushState.Idle(initial))
        private set

    var coverage by mutableFloatStateOf(1f)
        private set

    private var pending: SettingsPage? = null

    fun navigate(to: SettingsPage, reduced: Boolean = false) {
        val current = state
        if (reduced) {
            pending = null
            state = PushState.Idle(to)
        } else if (current is PushState.Animating && to != current.lower && to != current.upper) {
            // Let this pair finish before mounting the next pair.
            pending = to
        } else {
            pending = null
            setTarget(current.target(to, coverage))
        }
    }

    fun drag(progress: Float) {
        val current = state
        if (pending != null || (current is PushState.Animating && current.top != current.upper)) return
        val drag = current.dragTo(progress, coverage) as? PushState.Dragging ?: return
        if (current !is PushState.Dragging) state = drag
        coverage = drag.coverage
    }

    fun cancelDrag() {
        setTarget(state.settle(commit = false, coverage = coverage))
    }

    fun finish(transition: PushState.Animating, reduced: Boolean = false) {
        if (state !== transition) return
        val next = pending
        pending = null
        state = PushState.Idle(transition.top)
        if (next != null) navigate(next, reduced)
    }

    fun update(transition: PushState.Animating, value: Float) {
        if (state === transition) coverage = value
    }

    private fun setTarget(next: PushState) {
        if (state !== next) {
            if (next is PushState.Animating) coverage = next.startCoverage
            state = next
        }
    }
}

internal fun Modifier.inactiveNavigation(): Modifier =
    clearAndSetSemantics { hideFromAccessibility() }
        .focusProperties { canFocus = false }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                }
            }
        }

@Composable
internal fun rememberPushMotion(initial: SettingsPage, reduced: Boolean): PushMotion {
    val motion = remember { PushMotion(initial) }
    val state = motion.state
    LaunchedEffect(state, reduced) {
        val transition = state as? PushState.Animating ?: return@LaunchedEffect
        if (!reduced) {
            val spec = if (transition.releasing) {
                Motion.pushSpec(Motion.RELEASE_DAMPING, Motion.RELEASE_RESPONSE)
            } else {
                Motion.pushSpec()
            }
            val target = if (transition.top == transition.upper) 1f else 0f
            Animatable(motion.coverage).animateTo(target, spec) { motion.update(transition, value) }
        }
        motion.finish(transition, reduced)
    }
    return motion
}

@Composable
internal fun PushHost(
    motion: PushMotion,
    content: @Composable (SettingsPage) -> Unit,
) {
    val reduced = rememberMotionReduced()
    val state = motion.state
    val ltr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val direction = if (ltr) 1f else -1f

    val lower = if (reduced) null else when (state) {
        is PushState.Idle -> null
        is PushState.Dragging -> state.lower
        is PushState.Animating -> state.lower
    }
    val upper = if (reduced) null else when (state) {
        is PushState.Idle -> null
        is PushState.Dragging -> state.top
        is PushState.Animating -> state.upper
    }
    val pages = if (lower != null && upper != null) listOf(lower, upper) else listOf(state.top)
    Box(Modifier.fillMaxSize().clipToBounds()) {
        for (page in pages) key(page) {
            val isUpper = page == upper
            val isLower = page == lower
            val transform = Modifier
                .zIndex(if (isUpper) 1f else 0f)
                .graphicsLayer {
                    translationX = when {
                        isUpper -> direction * size.width * (1f - motion.coverage.coerceIn(0f, 1f))
                        isLower -> direction * parallaxOffsetPx(motion.coverage.coerceIn(0f, 1f), size.width)
                        else -> 0f
                    }
                }
                .drawWithContent {
                    if (isLower) {
                        val coverage = motion.coverage.coerceIn(0f, 1f)
                        // Clip to the portion of the parent still uncovered by the moving, translucent child.
                        val extent = size.width * (1f - coverage) - parallaxOffsetPx(coverage, size.width)
                        clipRect(left = if (ltr) 0f else size.width - extent, right = if (ltr) extent else size.width) {
                            this@drawWithContent.drawContent()
                            drawRect(Color.Black.copy(alpha = dimAlpha(coverage)))
                        }
                    } else {
                        drawContent()
                    }
                }
            Box(
                Modifier.fillMaxSize().then(transform)
                    .then(if (page != state.top) Modifier.inactiveNavigation() else Modifier),
            ) {
                content(page)
            }
        }
    }
}
