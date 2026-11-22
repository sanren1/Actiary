package com.actiary.ui.motion

import com.actiary.Tab
import com.actiary.ui.settings.SettingsPage
import org.junit.Assert.assertEquals
import org.junit.Test

class BackTargetTest {
    @Test
    fun overlayFirst() {
        assertEquals(BackTarget.CloseOverlay, backTarget(true, Tab.SETTINGS, SettingsPage.CUSTOM_COLOR))
        assertEquals(BackTarget.CloseOverlay, backTarget(true, Tab.CALENDAR, SettingsPage.HOME))
    }

    @Test
    fun settingsSubpageBeforeTab() {
        assertEquals(BackTarget.SettingsParent, backTarget(false, Tab.SETTINGS, SettingsPage.CUSTOM_COLOR))
        assertEquals(BackTarget.SettingsParent, backTarget(false, Tab.SETTINGS, SettingsPage.APPEARANCE))
    }

    @Test
    fun otherTabGoesToCalendar() {
        assertEquals(BackTarget.ToCalendar, backTarget(false, Tab.SETTINGS, SettingsPage.HOME))
        assertEquals(BackTarget.ToCalendar, backTarget(false, Tab.RECORDS, SettingsPage.HOME))
    }

    @Test
    fun calendarRootIsNone() {
        assertEquals(BackTarget.None, backTarget(false, Tab.CALENDAR, SettingsPage.HOME))
    }
}
