package com.actiary.glass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SegmentedMathTest {
    private val equal = listOf(100f, 100f, 100f)
    private val varied = listOf(60f, 100f, 80f)

    @Test fun equalWidthOffset() {
        assertEquals(0f, thumbOffset(0f, equal), 1e-4f)
        assertEquals(100f, thumbOffset(1f, equal), 1e-4f)
        assertEquals(150f, thumbOffset(1.5f, equal), 1e-4f)
        assertEquals(100f, thumbWidth(1.3f, equal), 1e-4f)
    }

    @Test fun variableWidthOffsetInterpolates() {
        assertEquals(60f, thumbOffset(1f, varied), 1e-4f)
        assertEquals(110f, thumbOffset(1.5f, varied), 1e-4f)
        assertEquals(160f, thumbOffset(2f, varied), 1e-4f)
        assertEquals(80f, thumbWidth(0.5f, varied), 1e-4f)
        assertEquals(90f, thumbWidth(1.5f, varied), 1e-4f)
    }

    @Test fun clampsOutOfRangeIndex() {
        assertEquals(0f, thumbOffset(-3f, varied), 1e-4f)
        assertEquals(160f, thumbOffset(9f, varied), 1e-4f)
        assertEquals(80f, thumbWidth(9f, varied), 1e-4f)
    }

    @Test fun singleItem() {
        assertEquals(0f, thumbOffset(0f, listOf(120f)), 1e-4f)
        assertEquals(0f, thumbOffset(0.7f, listOf(120f)), 1e-4f)
        assertEquals(120f, thumbWidth(0f, listOf(120f)), 1e-4f)
    }

    @Test fun pressTargetsTheTouchedOptionWithVariableWidthsAndRtl() {
        assertEquals(0, segmentIndexAt(30f, varied, true))
        assertEquals(1, segmentIndexAt(60f, varied, true))
        assertEquals(2, segmentIndexAt(170f, varied, true))
        assertEquals(2, segmentIndexAt(30f, varied, false))
        assertEquals(1, segmentIndexAt(100f, varied, false))
        assertEquals(0, segmentIndexAt(200f, varied, false))
        assertNull(segmentIndexAt(-1f, varied, true))
        assertNull(segmentIndexAt(240f, varied, false))
    }
}
