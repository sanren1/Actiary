package com.actiary.glass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PressDragMathTest {
    @Test fun deformScaleZeroIsIdentity() {
        assertEquals(1f to 1f, deformScale(0f))
    }

    @Test fun deformScaleClamps() {
        val (x, y) = deformScale(100f)
        assertEquals(1f / 0.8f, x, 1e-5f)
        assertEquals(0.8f, y, 1e-5f)
        val (nx, ny) = deformScale(-100f)
        assertEquals(1f / 1.2f, nx, 1e-5f)
        assertEquals(1.2f, ny, 1e-5f)
    }

    @Test fun deformScaleSmallVelocityIsProportional() {
        val (x, y) = deformScale(0.1f)
        assertEquals(1f / (1f - 0.075f), x, 1e-5f)
        assertEquals(1f - 0.025f, y, 1e-5f)
        assertTrue(x > 1f && y < 1f)
    }

    @Test fun indexAtRoundsAndClamps() {
        assertEquals(0, indexAt(-50f, 300f, 3))
        assertEquals(0, indexAt(99f, 300f, 3))
        assertEquals(1, indexAt(100f, 300f, 3))
        assertEquals(2, indexAt(299f, 300f, 3))
        assertEquals(2, indexAt(900f, 300f, 3))
    }

    @Test fun indexAtDegenerateInputs() {
        assertEquals(0, indexAt(50f, 0f, 3))
        assertEquals(0, indexAt(50f, 100f, 1))
        assertEquals(0, indexAt(50f, 100f, 0))
    }

    @Test fun valueForRoundTrip() {
        val count = 4
        val width = 400f
        for (i in 0 until count) {
            assertEquals(i.toFloat(), valueFor(i, count), 0f)
            assertEquals(i, indexAt((valueFor(i, count) + 0.5f) * width / count, width, count))
        }
        assertEquals(3f, valueFor(9, count), 0f)
        assertEquals(0f, valueFor(-2, count), 0f)
    }
}
