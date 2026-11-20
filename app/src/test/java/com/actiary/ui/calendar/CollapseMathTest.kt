package com.actiary.ui.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Test
import org.junit.Assert.assertEquals

class CollapseMathTest {
    @Test fun heightEndpoints() {
        assertEquals(288f, collapsedHeight(288f, 48f, 0f), 0f)
        assertEquals(48f, collapsedHeight(288f, 48f, 1f), 0f)
        assertEquals(168f, collapsedHeight(288f, 48f, 0.5f), 0f)
    }

    @Test fun offsetEndpoints() {
        assertEquals(0f, rowOffset(3, 48f, 0f), 0f)
        assertEquals(-144f, rowOffset(3, 48f, 1f), 0f)
        assertEquals(-72f, rowOffset(3, 48f, 0.5f), 0f)
        assertEquals(0f, rowOffset(0, 48f, 1f), 0f)
    }

    @Test fun snapAtHalf() {
        assertEquals(0f, snapTarget(0.49f), 0f)
        assertEquals(1f, snapTarget(0.5f), 0f)
    }

    @Test fun keptRowSundayFirst() {
        // Nov 2026 starts on Sunday; Nov 15 is the start of the third week.
        val m = YearMonth.of(2026, 11)
        assertEquals(0, keptRowIndex(m, LocalDate.of(2026, 11, 1), DayOfWeek.SUNDAY))
        assertEquals(2, keptRowIndex(m, LocalDate.of(2026, 11, 15), DayOfWeek.SUNDAY))
        assertEquals(4, keptRowIndex(m, LocalDate.of(2026, 11, 30), DayOfWeek.SUNDAY))
    }

    @Test fun keptRowMondayFirst() {
        val m = YearMonth.of(2026, 11)
        // Monday-first: Nov 1 (Sun) is last day of week 0; Nov 2 starts week 1.
        assertEquals(0, keptRowIndex(m, LocalDate.of(2026, 11, 1), DayOfWeek.MONDAY))
        assertEquals(1, keptRowIndex(m, LocalDate.of(2026, 11, 2), DayOfWeek.MONDAY))
        assertEquals(5, keptRowIndex(m, LocalDate.of(2026, 11, 30), DayOfWeek.MONDAY))
    }

    @Test fun keptRowLeapFebruary() {
        // Feb 2028 (leap) starts Tuesday and has 29 days.
        val m = YearMonth.of(2028, 2)
        assertEquals(0, keptRowIndex(m, LocalDate.of(2028, 2, 5), DayOfWeek.SUNDAY))
        assertEquals(4, keptRowIndex(m, LocalDate.of(2028, 2, 29), DayOfWeek.SUNDAY))
    }

    @Test fun selectedOutsideMonthKeepsFirstRow() {
        assertEquals(0, keptRowIndex(YearMonth.of(2026, 11), LocalDate.of(2026, 12, 20), DayOfWeek.SUNDAY))
    }
}
