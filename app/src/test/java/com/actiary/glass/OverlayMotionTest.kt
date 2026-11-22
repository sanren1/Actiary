package com.actiary.glass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayMotionTest {
    @Test fun scrimFollowsOffset() {
        assertEquals(0.75f, sheetScrimAlpha(100f, 400f), 0.001f)
    }

    @Test fun scrimClamped() {
        assertEquals(1f, sheetScrimAlpha(-10f, 400f), 0.001f)
        assertEquals(0f, sheetScrimAlpha(500f, 400f), 0.001f)
        assertEquals(1f, sheetScrimAlpha(0f, 0f), 0.001f)
    }

    @Test fun dismissByDistance() {
        assertTrue(shouldDismissSheet(101f, 300f, 0f))
    }

    @Test fun dismissByVelocity() {
        assertTrue(shouldDismissSheet(1f, 300f, 1501f))
    }

    @Test fun noDismissSmallDrag() {
        assertFalse(shouldDismissSheet(100f, 300f, 1500f))
    }
}
