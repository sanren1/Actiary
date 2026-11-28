package com.actiary.theme

import com.actiary.data.appearance.AccentPreset
import com.actiary.data.appearance.AppearanceSettings
import com.actiary.data.appearance.P5Palette
import com.actiary.data.appearance.ThemeMode
import com.actiary.data.appearance.UiStyle
import com.actiary.p5.resolveP5Tokens
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

    @Test fun persona5IgnoresLiquidGlassSettings() {
        for (palette in P5Palette.entries) {
            val baseline = resolveTheme(
                AppearanceSettings(
                    uiStyle = UiStyle.PERSONA_5,
                    p5Palette = palette,
                    themeMode = ThemeMode.LIGHT,
                    amoledBlack = false,
                    accentPreset = AccentPreset.SAGE,
                    ambientBackground = true,
                ),
                systemDark = false,
            )
            val expectedDark = palette != P5Palette.CALLING_CARD
            assertEquals("Expected dark=$expectedDark for $palette", expectedDark, baseline.dark)
            assertFalse("Expected ambientEnabled=false for $palette", baseline.ambientEnabled)

            val variants = listOf(
                AppearanceSettings(
                    uiStyle = UiStyle.PERSONA_5,
                    p5Palette = palette,
                    themeMode = ThemeMode.DARK,
                    amoledBlack = true,
                    accentPreset = AccentPreset.CUSTOM,
                    customAccentHex = "#123456",
                    ambientBackground = true,
                ),
                AppearanceSettings(
                    uiStyle = UiStyle.PERSONA_5,
                    p5Palette = palette,
                    themeMode = ThemeMode.SYSTEM,
                    amoledBlack = false,
                    accentPreset = AccentPreset.SAGE,
                    ambientBackground = false,
                ),
                AppearanceSettings(
                    uiStyle = UiStyle.PERSONA_5,
                    p5Palette = palette,
                    themeMode = ThemeMode.LIGHT,
                    amoledBlack = true,
                    accentPreset = AccentPreset.CUSTOM,
                    customAccentHex = "#FF00AA",
                    ambientBackground = true,
                ),
            )
            for (settings in variants) {
                for (systemDark in listOf(false, true)) {
                    val actual = resolveTheme(settings, systemDark = systemDark, dynamicSeed = 0xFF336699.toInt())
                    assertEquals("Expected identical ThemeTokens for $palette with $settings (systemDark=$systemDark)", baseline, actual)
                }
            }
        }
    }

    @Test fun persona5AllPalettesMeetWcagContrast() {
        for (palette in P5Palette.entries) {
            val settings = AppearanceSettings(uiStyle = UiStyle.PERSONA_5, p5Palette = palette)
            val t = resolveTheme(settings, systemDark = false)
            assertTrue("$palette textPrimary/bg", contrast(t.neutrals.textPrimary, t.neutrals.background) >= 4.5)
            assertTrue("$palette textPrimary/surface", contrast(t.neutrals.textPrimary, t.neutrals.surface) >= 4.5)
            assertTrue("$palette textSecondary/bg", contrast(t.neutrals.textSecondary, t.neutrals.background) >= 4.5)
            assertTrue("$palette textSecondary/surface", contrast(t.neutrals.textSecondary, t.neutrals.surface) >= 4.5)
            assertTrue("$palette onAccentAction/accentAction", contrast(t.accent.onAccentAction, t.accent.accentAction) >= 4.5)
            assertTrue("$palette accentAction/surface", contrast(t.accent.accentAction, t.neutrals.surface) >= 4.5)
            assertTrue("$palette accentStroke/surface", contrast(t.accent.accentStroke, t.neutrals.surface) >= 3.0)
            assertTrue("$palette accentChart/bg", contrast(t.accent.accentChart, t.neutrals.background) >= 3.0)

            val p5 = resolveP5Tokens(settings)
            assertTrue("$palette onBrandFill/brandFill", contrast(p5.onBrandFill, p5.brandFill) >= 4.5)
            assertTrue("$palette onHighlightBadge/highlightBadge", contrast(p5.onHighlightBadge, p5.highlightBadge) >= 4.5)
            assertTrue("$palette onLightCard/lightCard", contrast(p5.onLightCard, p5.lightCard) >= 4.5)
            assertTrue("$palette onLightCardMuted/lightCard", contrast(p5.onLightCardMuted, p5.lightCard) >= 4.5)
            if (palette != P5Palette.CALLING_CARD) {
                assertTrue("$palette accentOnDark/canvas", contrast(p5.accentOnDark, p5.canvas) >= 4.5)
            } else {
                assertTrue("$palette accentAction/canvas", contrast(t.accent.accentAction, p5.canvas) >= 4.5)
                assertTrue("$palette accentAction/lightCard", contrast(t.accent.accentAction, p5.lightCard) >= 4.5)
            }
            assertTrue("$palette accentOnDark/darkCard", contrast(p5.accentOnDark, p5.darkCard) >= 4.5)
            assertTrue("$palette accentChartOnLight/lightCard", contrast(p5.accentChartOnLight, p5.lightCard) >= 3.0)
        }
    }

    @Test fun callingCardUsesLightThemeTokens() {
        val settings = AppearanceSettings(uiStyle = UiStyle.PERSONA_5, p5Palette = P5Palette.CALLING_CARD)
        val t = resolveTheme(settings, systemDark = true)
        assertFalse(t.dark)
        assertEquals(0xFFF7F6F2.toInt(), t.neutrals.background)
        assertEquals(0xFFFFFFFF.toInt(), t.neutrals.surface)
        assertEquals(0xFFEAE8E1.toInt(), t.neutrals.surfaceMuted)
        assertEquals(0xFF0D0D0D.toInt(), t.neutrals.textPrimary)
        assertEquals(0xFF45454A.toInt(), t.neutrals.textSecondary)
        assertEquals(0xFF1F1F23.toInt(), t.neutrals.divider)
        assertEquals(0xFFFFFFFF.toInt(), t.accent.onAccentAction)
    }

    @Test fun royalGoldUsesDarkInkOnBrandFill() {
        val p5 = resolveP5Tokens(AppearanceSettings(uiStyle = UiStyle.PERSONA_5, p5Palette = P5Palette.ROYAL_GOLD))
        assertEquals(0xFFE5A910.toInt(), p5.brandFill)
        assertEquals(0xFF0D0D0D.toInt(), p5.onBrandFill)
    }
}
