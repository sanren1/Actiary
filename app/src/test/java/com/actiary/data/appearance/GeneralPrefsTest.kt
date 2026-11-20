package com.actiary.data.appearance

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.actiary.domain.RangeKind
import com.actiary.domain.rangeOf
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GeneralPrefsTest {
    private val weekStartKey = stringPreferencesKey("week_start")

    @Test fun defaultIsSystem() {
        assertEquals(WeekStart.SYSTEM, emptyPreferences().toWeekStart())
    }

    @Test fun unknownValueFallsBackToSystem() {
        assertEquals(WeekStart.SYSTEM, preferencesOf(weekStartKey to "WEDNESDAY").toWeekStart())
    }

    @Test fun roundTrip() = runBlocking {
        val store = inMemoryPreferencesStore()
        val general = GeneralPrefsRepository(store)
        general.setWeekStart(WeekStart.SUNDAY)
        assertEquals("SUNDAY", store.data.first()[weekStartKey])
        assertEquals(WeekStart.SUNDAY, general.weekStart.first())
    }

    @Test fun resolveFixedValues() {
        assertEquals(DayOfWeek.MONDAY, WeekStart.MONDAY.resolve())
        assertEquals(DayOfWeek.SUNDAY, WeekStart.SUNDAY.resolve())
    }

    @Test fun systemFollowsLocale() {
        val original = Locale.getDefault()
        val display = Locale.getDefault(Locale.Category.DISPLAY)
        val format = Locale.getDefault(Locale.Category.FORMAT)
        try {
            Locale.setDefault(Locale.US)
            assertEquals(DayOfWeek.SUNDAY, WeekStart.SYSTEM.resolve())
            Locale.setDefault(Locale.FRANCE)
            assertEquals(DayOfWeek.MONDAY, WeekStart.SYSTEM.resolve())
        } finally {
            Locale.setDefault(original)
            Locale.setDefault(Locale.Category.DISPLAY, display)
            Locale.setDefault(Locale.Category.FORMAT, format)
        }
    }

    @Test fun changingWeekStartChangesWeeklyRange() = runBlocking {
        val general = GeneralPrefsRepository(inMemoryPreferencesStore())
        val sunday = LocalDate.of(2025, 3, 9)

        general.setWeekStart(WeekStart.MONDAY)
        val mondayStart = rangeOf(sunday, RangeKind.WEEK, general.weekStart.first().resolve())
        assertEquals(LocalDate.of(2025, 3, 3), mondayStart.from)

        general.setWeekStart(WeekStart.SUNDAY)
        val sundayStart = rangeOf(sunday, RangeKind.WEEK, general.weekStart.first().resolve())
        assertEquals(sunday, sundayStart.from)
    }
}
