package com.actiary.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsPageTest {
    @Test
    fun parentChain() {
        assertEquals(SettingsPage.APPEARANCE, SettingsPage.CUSTOM_COLOR.parent())
        assertEquals(SettingsPage.HOME, SettingsPage.APPEARANCE.parent())
        assertEquals(SettingsPage.HOME, SettingsPage.TYPES.parent())
        assertEquals(null, SettingsPage.HOME.parent())
    }
}
