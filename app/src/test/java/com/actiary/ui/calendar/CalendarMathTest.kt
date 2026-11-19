package com.actiary.ui.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CalendarMathTest {
    @Test fun leapFebruary() =
        assertEquals(LocalDate.of(2024, 2, 1)..LocalDate.of(2024, 2, 29), monthRange(YearMonth.of(2024, 2)))

    @Test fun decemberEndsOnThe31st() =
        assertEquals(LocalDate.of(2025, 12, 31), monthRange(YearMonth.of(2025, 12)).endInclusive)

    @Test fun future() {
        val today = LocalDate.of(2025, 12, 31)
        assertTrue(isFuture(today.plusDays(1), today))
        assertFalse(isFuture(today, today))
        assertFalse(isFuture(today.minusDays(1), today))
    }
}
