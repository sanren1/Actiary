package com.actiary.ui.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionTest {
    @Test fun reducedWhenScaleZero() {
        assertTrue(reducedMotion(animatorScale = 0f, powerSave = false))
    }

    @Test fun reducedWhenPowerSave() {
        assertTrue(reducedMotion(animatorScale = 1f, powerSave = true))
    }

    @Test fun notReducedByDefault() {
        assertFalse(reducedMotion(animatorScale = 1f, powerSave = false))
        assertFalse(reducedMotion(animatorScale = 0.5f, powerSave = false))
    }

    @Test fun parallaxOffsetAtEnds() {
        assertEquals(0f, parallaxOffsetPx(progress = 0f, widthPx = 200f), 0.001f)
        assertEquals(-60f, parallaxOffsetPx(progress = 1f, widthPx = 200f), 0.001f)
    }

    @Test fun dimAlphaAtEnds() {
        assertEquals(0f, dimAlpha(progress = 0f), 0.001f)
        assertEquals(0.10f, dimAlpha(progress = 1f), 0.001f)
    }

    @Test fun pushSpecStiffnessMatchesResponse() {
        val spec = Motion.pushSpec()
        assertEquals(438.65f, spec.stiffness, 0.5f)
        assertEquals(1f, spec.dampingRatio, 0f)
    }
}
