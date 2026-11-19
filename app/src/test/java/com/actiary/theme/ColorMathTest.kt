package com.actiary.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ColorMathTest {
    private val white = 0xFFFFFFFF.toInt()
    private val black = 0xFF000000.toInt()

    @Test fun parseHexAcceptsOnlyRrggbb() {
        assertEquals(0xFF77879D.toInt(), parseHex("#77879d"))
        assertNull(parseHex("77879D"))
        assertNull(parseHex("#FF77879D"))
        assertNull(parseHex("#GGGGGG"))
        assertNull(parseHex(" #77879D"))
    }

    @Test fun toHexIsUpperCase() {
        assertEquals("#77879D", 0xFF77879D.toInt().toHex())
    }

    @Test fun contrastValues() {
        assertEquals(21.0, contrast(white, black), 0.01)
        assertEquals(1.0, contrast(0xFF77879D.toInt(), 0xFF77879D.toInt()), 0.0)
        assertTrue(contrast(0xFF77879D.toInt(), white) in 2.8..3.8)
    }

    @Test fun roundTrip() {
        val hexes = listOf("#758B84", "#77879D", "#8C9683", "#AC9680", "#928A9F", "#82858A",
            "#FF0000", "#00FF00", "#0000FF", "#FFFFFF", "#000000")
        for (hex in hexes) {
            val c = parseHex(hex)!!
            val r = c.toOklch().toArgb()
            for (shift in listOf(16, 8, 0)) {
                val d = abs(((c shr shift) and 0xFF) - ((r shr shift) and 0xFF))
                assertTrue("$hex shift $shift diff $d", d <= 1)
            }
        }
    }

    @Test fun outOfGamutKeepsLightnessAndHue() {
        val o = Oklch(0.7, 0.4, 150.0)
        val back = o.toArgb().toOklch()
        assertEquals(0.7, back.l, 0.02)
        val dh = abs(back.h - 150.0).let { minOf(it, 360 - it) }
        assertTrue("chroma ${back.c}", back.c < 0.4)
        assertTrue("hue diff $dh", dh <= 3.0)
    }

    @Test fun mixMidpoint() {
        val m = mix(white, black, 0.5)
        for (shift in listOf(16, 8, 0)) assertTrue(((m shr shift) and 0xFF) in 127..128)
    }
}
