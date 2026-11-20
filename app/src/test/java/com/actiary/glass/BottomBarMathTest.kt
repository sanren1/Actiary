package com.actiary.glass

import org.junit.Assert.assertEquals
import org.junit.Test

class BottomBarMathTest {
    @Test fun thumbOffsetFollowsValueAndMirrors() {
        assertEquals(150f, thumbOffsetPx(1.5f, 100f, true), 1e-4f)
        assertEquals(-150f, thumbOffsetPx(1.5f, 100f, false), 1e-4f)
        assertEquals(0f, thumbOffsetPx(0f, 100f, true), 1e-4f)
    }

    @Test fun dragValueMovesByTabsAndClamps() {
        assertEquals(1.5f, dragValue(1f, 50f, 100f, 3, true), 1e-4f)
        assertEquals(0.5f, dragValue(1f, 50f, 100f, 3, false), 1e-4f)
        assertEquals(2f, dragValue(1.9f, 500f, 100f, 3, true), 1e-4f)
        assertEquals(0f, dragValue(0.2f, -500f, 100f, 3, true), 1e-4f)
    }

    @Test fun settleIndexRoundsAndClamps() {
        assertEquals(1, settleIndex(1.4f, 3))
        assertEquals(2, settleIndex(1.5f, 3))
        assertEquals(0, settleIndex(-0.7f, 3))
        assertEquals(2, settleIndex(9f, 3))
    }

    @Test fun panelScaleGrowsByPixelsAtFullPress() {
        assertEquals(1f, panelScale(0f, 320f, 48f), 1e-5f)
        assertEquals(1.15f, panelScale(1f, 320f, 48f), 1e-5f)
        assertEquals(1.075f, panelScale(0.5f, 320f, 48f), 1e-5f)
    }

    @Test fun interactingUntilPressAndScaleSettle() {
        assertEquals(false, isInteracting(0f, 1f, 1f))
        assertEquals(false, isInteracting(0.0005f, 1.001f, 0.999f))
        assertEquals(true, isInteracting(0.01f, 1f, 1f))
        assertEquals(true, isInteracting(0f, 1.1f, 1f))
        assertEquals(true, isInteracting(0f, 1f, 0.99f))
    }
}
