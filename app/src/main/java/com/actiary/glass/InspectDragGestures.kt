// Adapted from Kyant0/AndroidLiquidGlass catalog (utils/DragGestureInspector.kt), Apache-2.0, Copyright 2025 Kyant.
// Modified for Actiary: Modifier wrapper, simplified drag loop.
package com.actiary.glass

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange

/**
 * Reports a press-and-drag without consuming events, so inner clickables still fire. The down is seen on the
 * Initial pass (before children), there is no touch slop, and the gesture is cancelled if another node consumes it.
 * Callbacks are captured once (key = Unit): read changing state through rememberUpdatedState or a state holder.
 */
fun Modifier.inspectDragGestures(
    onDragStart: (down: PointerInputChange) -> Unit = {},
    onDragEnd: (change: PointerInputChange) -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val down = awaitFirstDown(requireUnconsumed = false)
        onDragStart(down)
        onDrag(down, Offset.Zero)
        var pointer = down.id
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == pointer }
            if (change == null) {
                onDragCancel()
                return@awaitEachGesture
            }
            if (change.changedToUpIgnoreConsumed()) {
                val other = event.changes.firstOrNull { it.pressed }
                if (other == null) {
                    onDragEnd(change)
                    return@awaitEachGesture
                }
                pointer = other.id
            } else if (change.isConsumed) {
                onDragCancel()
                return@awaitEachGesture
            } else if (change.previousPosition != change.position) {
                onDrag(change, change.positionChange())
            }
        }
    }
}
