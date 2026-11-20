package com.actiary.glass

import com.actiary.data.appearance.AppearanceSettings
import com.actiary.data.appearance.GlassLevel
import com.actiary.data.appearance.GlassTintStrength
import com.actiary.data.appearance.ThemeMode
import com.actiary.theme.mix
import com.actiary.theme.resolveTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class GlassProfileTest {
    private val tokensList = listOf(
        resolveTheme(AppearanceSettings(), systemDark = false),
        resolveTheme(AppearanceSettings(themeMode = ThemeMode.DARK), systemDark = false),
    )

    @Test fun levelsAreMonotonic() {
        for (t in tokensList) {
            val (s, b, c) = GlassLevel.entries.map { resolveGlass(it, GlassTintStrength.MEDIUM, t, false) }
            assertTrue(s.blurDp > b.blurDp && b.blurDp > c.blurDp)
            assertTrue(s.refractionAmountDp < b.refractionAmountDp && b.refractionAmountDp < c.refractionAmountDp)
            assertTrue(c.chromaticAberration && !s.chromaticAberration && !b.chromaticAberration)
            assertTrue((c.surfaceColor ushr 24) >= (0.26 * 255).roundToInt())
        }
    }

    @Test fun tintWeightsShowInRgb() {
        for (t in tokensList) for ((tint, w) in listOf(
            GlassTintStrength.LOW to 0.03, GlassTintStrength.MEDIUM to 0.065, GlassTintStrength.HIGH to 0.105,
        )) {
            val p = resolveGlass(GlassLevel.BALANCED, tint, t, false)
            val expected = mix(t.neutrals.glassNeutralTint, t.accent.glassAccentTint, w)
            assertEquals(expected and 0xFFFFFF, p.surfaceColor and 0xFFFFFF)
            assertEquals((0.42 * 255).roundToInt(), p.surfaceColor ushr 24)
        }
    }

    @Test fun degradedDropsEffects() {
        for (t in tokensList) for (l in GlassLevel.entries) {
            val p = resolveGlass(l, GlassTintStrength.HIGH, t, true)
            assertTrue(p.degraded)
            assertEquals(0f, p.blurDp); assertEquals(0f, p.refractionHeightDp); assertEquals(0f, p.refractionAmountDp)
            assertFalse(p.chromaticAberration)
            assertEquals((0.85 * 255).roundToInt(), p.surfaceColor ushr 24)
        }
    }
}
