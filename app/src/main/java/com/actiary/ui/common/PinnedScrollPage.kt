package com.actiary.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.actiary.glass.headerBackdropBlur
import com.actiary.glass.headerBackdropScrim

private enum class PageSlot { Header, Body }

/** Measures the pinned header before giving its height to the body as scroll content padding. */
@Composable
internal fun PinnedScrollPage(
    scrollOffset: () -> Int,
    header: @Composable () -> Unit,
    body: @Composable (headerHeight: Dp, viewportHeight: Dp) -> Unit,
) {
    SubcomposeLayout(Modifier.fillMaxSize()) { constraints ->
        val headerPlaceable = subcompose(PageSlot.Header) {
            Box(
                Modifier.fillMaxWidth()
                    .headerBackdropScrim(scrollOffset)
                    .semantics {
                        isTraversalGroup = true
                        traversalIndex = -1f
                    }
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                    // The pinned header receives hits instead of controls scrolling beneath it.
                    .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
            ) { header() }
        }.single().measure(constraints.copy(minHeight = 0))
        val headerHeight = headerPlaceable.height.toDp()
        val viewportHeight = constraints.maxHeight.toDp()
        val bodyPlaceable = subcompose(PageSlot.Body) {
            Box(Modifier.fillMaxSize().headerBackdropBlur(headerHeight, scrollOffset)) {
                body(headerHeight, viewportHeight)
            }
        }.single().measure(Constraints.fixed(constraints.maxWidth, constraints.maxHeight))
        layout(bodyPlaceable.width, bodyPlaceable.height) {
            bodyPlaceable.place(0, 0)
            headerPlaceable.place(0, 0, zIndex = 1f)
        }
    }
}
