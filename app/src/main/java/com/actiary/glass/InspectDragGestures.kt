// Adapted from Kyant0/AndroidLiquidGlass catalog (utils/DragGestureInspector.kt), Apache-2.0, Copyright 2025 Kyant.
// Modified for Actiary: Modifier wrapper, simplified drag loop.
package com.actiary.glass

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

/**
 * Leaves taps with the child selectable. Only a horizontal drag beyond touch slop takes ownership.
 * A scrollable track observes presses without taking ownership from either scroll axis.
 */
internal fun Modifier.inspectDragGestures(
    state: PressDragState,
    allowDrag: Boolean,
    onPress: (Offset) -> Boolean,
    onRelease: (dragged: Boolean) -> Unit,
    onCancel: () -> Unit,
    onDrag: (dragX: Float) -> Unit,
): Modifier = pointerInput(state, allowDrag) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        if (!onPress(down.position)) return@awaitEachGesture
        var dragging = false
        var released = false
        try {
            while (true) {
                // Compose dispatches cancellation as an up that is already consumed in Initial.
                val initial = awaitPointerEvent(PointerEventPass.Initial)
                val initialChange = initial.changes.firstOrNull { it.id == down.id } ?: break
                if (initialChange.changedToUpIgnoreConsumed() && initialChange.isConsumed) break

                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (change.changedToUpIgnoreConsumed()) {
                    // Main dispatches to children first. The selectable consumes a successful tap's up.
                    released = dragging || change.isConsumed
                    if (dragging) change.consume()
                    if (released) onRelease(dragging)
                    break
                }
                if (change.isConsumed) break
                val delta = change.position - down.position
                if (!dragging && abs(delta.y) > viewConfiguration.touchSlop && abs(delta.y) >= abs(delta.x)) break
                if (allowDrag && (dragging || abs(delta.x) > viewConfiguration.touchSlop && abs(delta.x) > abs(delta.y))) {
                    dragging = true
                    change.consume()
                    onDrag(delta.x)
                } else {
                    // Ancestor horizontal/vertical scrollers consume in Main, after this observer.
                    val final = awaitPointerEvent(PointerEventPass.Final)
                    if (final.changes.any { it.id == down.id && it.isConsumed }) break
                }
            }
        } finally {
            if (!released) onCancel()
        }
    }
}
