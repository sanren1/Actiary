package com.actiary.ui.settings

import com.actiary.theme.Hsv
import com.actiary.theme.normalizeHex
import com.actiary.theme.parseHex
import com.actiary.theme.toHsv
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ColorDraftTest {
    private val base = ColorDraft(hex = "#77879D", input = "#77879D")

    private fun assertColorClose(expected: String, actual: String) {
        val e = parseHex(expected)!!
        val a = parseHex(actual)!!
        for (shift in listOf(16, 8, 0)) {
            assertTrue("$expected vs $actual channel shift $shift", abs(((e shr shift) and 0xFF) - ((a shr shift) and 0xFF)) <= 1)
        }
    }

    @Test
    fun validInputUpdatesHex() {
        val d = base.withInput("3a7bd5")
        assertEquals("#3A7BD5", d.hex)
        assertEquals("3a7bd5", d.input)
        assertTrue(d.canApply)

        val withDecoration = base.withInput(" #FF0000 ")
        assertEquals("#FF0000", withDecoration.hex)
        assertEquals(" #FF0000 ", withDecoration.input)
        assertTrue(withDecoration.canApply)
    }

    @Test
    fun invalidInputKeepsHexAndBlocksApply() {
        for (bad in listOf("", "#12345", "#GGGGGG", "#12 456", "12345", "#1234567", "   ")) {
            val d = base.withInput(bad)
            assertEquals("hex must stay unchanged for '$bad'", "#77879D", d.hex)
            assertEquals(bad, d.input)
            assertFalse("'$bad' must not be applicable", d.canApply)
        }
    }

    @Test
    fun hsvUpdatesInput() {
        val red = base.withHsv(Hsv(0.0, 1.0, 1.0))
        assertEquals("#FF0000", red.hex)
        assertEquals("#FF0000", red.input)
        assertTrue(red.canApply)

        val gray = base.withHsv(Hsv(120.0, 0.0, 0.5))
        assertEquals(gray.hex, gray.input)
        assertEquals(gray.hex, normalizeHex(gray.input))
    }

    @Test
    fun inputRoundTripThroughHsv() {
        val viaInput = base.withInput(" ff0000 ")
        assertEquals("#FF0000", viaInput.hex)
        val viaHsv = viaInput.withHsv(parseHex(viaInput.hex)!!.toHsv())
        assertEquals("#FF0000", viaHsv.hex)
        assertEquals("#FF0000", viaHsv.input)
        assertTrue(viaHsv.canApply)

        val seed = base.withInput("3a7bd5")
        val back = seed.withHsv(parseHex(seed.hex)!!.toHsv())
        assertColorClose(seed.hex, back.hex)
        assertEquals(back.hex, normalizeHex(back.input))
        assertTrue(back.canApply)
    }
}
