package com.actiary.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsPageTest {
    @Test
    fun homeShowsBottomBar() {
        assertTrue(SettingsPage.HOME.showsBottomBar())
        assertNull(SettingsPage.HOME.parent())
    }

    @Test
    fun subpagesHideBottomBarUntilBackToHome() {
        val subpages = listOf(
            SettingsPage.APPEARANCE,
            SettingsPage.GENERAL,
            SettingsPage.TYPES,
            SettingsPage.DATA,
            SettingsPage.ABOUT,
        )
        for (page in subpages) {
            assertFalse(page.name, page.showsBottomBar())
            val parent = requireNotNull(page.parent())
            assertEquals(SettingsPage.HOME, parent)
            assertTrue(parent.showsBottomBar())
        }
    }

    @Test
    fun customColorKeepsBottomBarHiddenOnReturnToAppearance() {
        val page = SettingsPage.CUSTOM_COLOR
        assertFalse(page.showsBottomBar())
        val appearance = requireNotNull(page.parent())
        assertEquals(SettingsPage.APPEARANCE, appearance)
        assertFalse(appearance.showsBottomBar())
        val home = requireNotNull(appearance.parent())
        assertEquals(SettingsPage.HOME, home)
        assertTrue(home.showsBottomBar())
    }
}
