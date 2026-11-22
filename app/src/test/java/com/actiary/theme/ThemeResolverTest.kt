package com.actiary.theme

import com.actiary.data.appearance.AccentPreset
import com.actiary.data.appearance.AppearanceSettings
import com.actiary.data.appearance.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeResolverTest {
    @Test fun defaultsIgnoreSystemDark() {
        val t = resolveTheme(AppearanceSettings(), systemDark = true)
        assertFalse(t.dark)
        assertEquals(0xFF77879D.toInt(), t.accent.accentSeed)
    }

    @Test fun systemModeFollowsSystem() {
        assertTrue(resolveTheme(AppearanceSettings(themeMode = ThemeMode.SYSTEM), true).dark)
        assertFalse(resolveTheme(AppearanceSettings(themeMode = ThemeMode.SYSTEM), false).dark)
        assertTrue(resolveTheme(AppearanceSettings(themeMode = ThemeMode.DARK), false).dark)
    }

    @Test fun customSeed() {
        val ok = AppearanceSettings(accentPreset = AccentPreset.CUSTOM, customAccentHex = "#123456")
        assertEquals(0xFF123456.toInt(), resolveTheme(ok, false).accent.accentSeed)
        val bad = ok.copy(customAccentHex = "oops")
        assertEquals(DEFAULT_SEED, resolveTheme(bad, false).accent.accentSeed)
    }

    @Test fun ambientToggle() {
        assertFalse(resolveTheme(AppearanceSettings(ambientBackground = false), false).ambientEnabled)
    }

    @Test fun dynamicWallpaperAccentUsesProvidedSeedOrFallsBack() {
        val settings = AppearanceSettings(accentPreset = AccentPreset.DYNAMIC)
        assertEquals(0xFF336699.toInt(), resolveTheme(settings, false, dynamicSeed = 0xFF336699.toInt()).accent.accentSeed)
        assertEquals(DEFAULT_SEED, resolveTheme(settings, false, dynamicSeed = null).accent.accentSeed)
    }

    @Test fun amoledBlackAppliesOnlyInDarkTheme() {
        val amoledLight = resolveTheme(AppearanceSettings(themeMode = ThemeMode.LIGHT, amoledBlack = true), false)
        assertEquals(LightNeutrals, amoledLight.neutrals)

        val amoledDark = resolveTheme(AppearanceSettings(themeMode = ThemeMode.DARK, amoledBlack = true), false)
        assertEquals(AmoledDarkNeutrals, amoledDark.neutrals)
        assertEquals(0xFF000000.toInt(), amoledDark.neutrals.background)
    }
}
