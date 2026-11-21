package com.actiary.glass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToggleMathTest {
    @Test fun toggleTargetSnapsByHalf() {
        assertFalse(toggleTarget(0.49f, 0f))
        assertFalse(toggleTarget(0.5f, 0f))
        assertTrue(toggleTarget(0.51f, 0f))
    }

    @Test fun toggleTargetFollowsFastFling() {
        assertTrue(toggleTarget(0.3f, 6f))
        assertFalse(toggleTarget(0.7f, -6f))
        assertFalse(toggleTarget(0.3f, 2f))
        assertTrue(toggleTarget(0.7f, -2f))
    }

    @Test fun trackFractionClamps() {
        assertEquals(0f, trackColorFraction(-0.2f), 0f)
        assertEquals(0.5f, trackColorFraction(0.5f), 0f)
        assertEquals(1f, trackColorFraction(1.2f), 0f)
    }

    @Test fun lensRequiresMotionAndShaderSupport() {
        assertTrue(toggleGlassEnabled(degraded = false, animationsOff = false, shaderSupported = true))
        assertFalse(toggleGlassEnabled(degraded = true, animationsOff = false, shaderSupported = true))
        assertFalse(toggleGlassEnabled(degraded = false, animationsOff = true, shaderSupported = true))
        assertFalse(toggleGlassEnabled(degraded = false, animationsOff = false, shaderSupported = false))
    }
}
