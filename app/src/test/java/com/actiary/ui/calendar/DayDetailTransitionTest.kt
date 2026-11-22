package com.actiary.ui.calendar

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DayDetailTransitionTest {
    private val today = LocalDate.of(2026, 11, 21)
    private val yesterday = today.minusDays(1)

    @Test fun pendingOrOutgoingDetailCannotBeEdited() {
        assertFalse(canEditDayDetail(selected = today, loaded = null, displayed = null, today = today))
        assertFalse(canEditDayDetail(selected = today, loaded = yesterday, displayed = yesterday, today = today))
        assertFalse(canEditDayDetail(selected = today, loaded = today, displayed = yesterday, today = today))
    }

    @Test fun matchedDayCanBeEditedUnlessInFuture() {
        assertTrue(canEditDayDetail(selected = today, loaded = today, displayed = today, today = today))
        val tomorrow = today.plusDays(1)
        assertFalse(canEditDayDetail(selected = tomorrow, loaded = tomorrow, displayed = tomorrow, today = today))
    }
}
