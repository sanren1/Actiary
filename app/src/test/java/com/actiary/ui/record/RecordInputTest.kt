package com.actiary.ui.record

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecordInputTest {
    @Test fun minutes() {
        assertEquals(45, parseMinutes("45"))
        assertEquals(0, parseMinutes("0"))
        assertNull(parseMinutes(""))
        assertNull(parseMinutes("-5"))
        assertNull(parseMinutes("4.5"))
        assertNull(parseMinutes("abc"))
        assertNull(parseMinutes("99999999999"))
    }

    @Test fun km() {
        assertEquals(5000, parseKmToMeters("5"))
        assertEquals(5200, parseKmToMeters("5.2"))
        assertEquals(5200, parseKmToMeters("5,2"))
        assertEquals(500, parseKmToMeters(".5"))
        assertEquals(5000, parseKmToMeters("5."))
        assertNull(parseKmToMeters(""))
        assertNull(parseKmToMeters("."))
        assertNull(parseKmToMeters("-1"))
        assertNull(parseKmToMeters("5.25"))
        assertNull(parseKmToMeters("1.2.3"))
        assertNull(parseKmToMeters("abc"))
        assertNull(parseKmToMeters("99999999"))
    }

    @Test fun sanitize() {
        assertEquals("5.2", sanitizeKm("5,2"))
        assertEquals("5.2", sanitizeKm("5.25"))
        assertEquals("5.2", sanitizeKm("5.2.3"))
        assertEquals("12", sanitizeKm("1a2"))
        assertEquals("45", sanitizeMinutes("4x5"))
        assertEquals("100000", sanitizeMinutes("100000"))
        assertEquals("99999", sanitizeMinutes("999999"))
        assertEquals("10000.0", sanitizeKm("10000.0"))
        assertEquals("1000", sanitizeKm("10001"))
        assertEquals("10000", sanitizeKm("10000.5").substringBefore('.'))
        assertEquals("5.", sanitizeKm("5."))
        assertEquals(".", sanitizeKm("."))
    }

    @Test fun format() {
        assertEquals("5", formatKm(5000))
        assertEquals("5.2", formatKm(5200))
        assertEquals("0.1", formatKm(100))
        assertEquals("", formatKm(null))
        assertEquals("30", formatMinutes(30))
        assertEquals("", formatMinutes(null))
    }
}
