package com.actiary.glass

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Test

class GlassMenuPlacementTest {
    private val safe = IntRect(16, 28, 304, 620)
    private val anchor = IntRect(158, 200, 246, 248)

    @Test fun prefersBelowAndAlignsToTrailingAnchorInBothDirections() {
        val size = IntSize(140, 120)
        assertEquals(IntOffset(106, 254), positionGlassMenu(anchor, safe, size, 6, ltr = true))
        assertEquals(IntOffset(158, 254), positionGlassMenu(anchor, safe, size, 6, ltr = false))
    }

    @Test fun usesTheSpaceAboveWhenTheBottomEdgeCannotFitTheMenu() {
        val nearBottom = IntRect(240, 542, 290, 594)
        assertEquals(508, glassMenuMaxHeight(nearBottom, safe, 6))
        assertEquals(IntOffset(150, 416), positionGlassMenu(nearBottom, safe, IntSize(140, 120), 6, ltr = true))
    }

    @Test fun constrainsWideMenuToSafeHorizontalBounds() {
        val nearLeft = IntRect(16, 300, 30, 348)
        assertEquals(IntOffset(16, 354), positionGlassMenu(nearLeft, safe, IntSize(245, 100), 6, ltr = true))
    }
}
