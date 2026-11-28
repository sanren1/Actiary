package com.actiary.p5

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class P5GeometryTest {

    @Test
    fun polygonPointsClampedWithinBoundsForTinyAndZeroSizes() {
        val sizes = listOf(
            0f to 0f,
            12f to 12f,
            320f to 120f,
        )
        for (variant in P5ShapeVariant.entries) {
            for ((width, height) in sizes) {
                val points = computeP5PolygonPoints(width, height, variant)
                assertEquals(
                    "Expected 4 vertices for $variant at ($width, $height)",
                    4,
                    points.size,
                )
                val baseCorners = listOf(
                    0f to 0f,
                    width to 0f,
                    width to height,
                    0f to height,
                )
                val maxOffset = min(width, height) * 0.14f + 0.001f
                points.forEachIndexed { index, (x, y) ->
                    assertTrue("x must be finite for $variant[$index]", x.isFinite())
                    assertTrue("y must be finite for $variant[$index]", y.isFinite())
                    assertTrue(
                        "x=$x out of [0, $width] for $variant[$index]",
                        x in 0f..width,
                    )
                    assertTrue(
                        "y=$y out of [0, $height] for $variant[$index]",
                        y in 0f..height,
                    )
                    val (bx, by) = baseCorners[index]
                    val dx = abs(x - bx)
                    val dy = abs(y - by)
                    val dist = hypot(dx, dy)
                    assertTrue(
                        "dx=$dx exceeds maxOffset=$maxOffset for $variant[$index] at ($width, $height)",
                        dx <= maxOffset,
                    )
                    assertTrue(
                        "dy=$dy exceeds maxOffset=$maxOffset for $variant[$index] at ($width, $height)",
                        dy <= maxOffset,
                    )
                    assertTrue(
                        "dist=$dist exceeds maxOffset=$maxOffset for $variant[$index] at ($width, $height)",
                        dist <= maxOffset,
                    )
                }
            }
        }
    }

    @Test
    fun ransomTitleIsDeterministicAndBounded() {
        val first = layoutRansomTitle("ACTIARY", enabled = true, reducedMotion = false)
        val second = layoutRansomTitle("ACTIARY", enabled = true, reducedMotion = false)

        assertEquals(first, second)
        assertEquals(7, first.size)
        assertTrue(first.all { it.rotationDeg in -4f..4f })
        assertTrue(first.first().accent)
    }

    @Test
    fun ransomTitleDisabledOrReducedMotionHasZeroRotation() {
        val disabled = layoutRansomTitle("ACTIARY", enabled = false, reducedMotion = false)
        assertTrue(disabled.isNotEmpty())
        assertTrue(disabled.all { it.rotationDeg == 0f })

        val reduced = layoutRansomTitle("ACTIARY", enabled = true, reducedMotion = true)
        assertTrue(reduced.isNotEmpty())
        assertTrue(reduced.all { it.rotationDeg == 0f })

        val both = layoutRansomTitle("ACTIARY", enabled = false, reducedMotion = true)
        assertTrue(both.isNotEmpty())
        assertTrue(both.all { it.rotationDeg == 0f })
    }

    @Test
    fun ransomTitleHandlesEmptyAndBlankSafely() {
        assertEquals(emptyList<RansomGlyphSpec>(), layoutRansomTitle("", enabled = true, reducedMotion = false))
        assertEquals(emptyList<RansomGlyphSpec>(), layoutRansomTitle("   ", enabled = true, reducedMotion = false))
        assertEquals(emptyList<RansomGlyphSpec>(), layoutRansomTitle("", enabled = false, reducedMotion = false))
        assertEquals(emptyList<RansomGlyphSpec>(), layoutRansomTitle("   ", enabled = true, reducedMotion = true))
    }
}
