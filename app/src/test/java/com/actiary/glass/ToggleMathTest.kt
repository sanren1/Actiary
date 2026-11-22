package com.actiary.glass

import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import com.actiary.data.appearance.AccentPreset
import com.actiary.data.appearance.AppearanceSettings
import com.actiary.data.appearance.ThemeMode
import com.actiary.theme.asColor
import com.actiary.theme.contrast
import com.actiary.theme.resolveTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToggleMathTest {
    @Test fun toggleTargetSnapsByHalf() {
        assertFalse(toggleTarget(0.49f, 0f))
        assertFalse(toggleTarget(0.5f, 0f))
        assertTrue(toggleTarget(0.51f, 0f))
    }

    @Test fun toggleTargetFollowsFastFling() {
        assertTrue(toggleTarget(0.3f, 6f))
        assertFalse(toggleTarget(0.7f, -6f))
        assertFalse(toggleTarget(0.3f, 2f))
        assertTrue(toggleTarget(0.7f, -2f))
    }

    @Test fun trackFractionClamps() {
        assertEquals(0f, trackColorFraction(-0.2f), 0f)
        assertEquals(0.5f, trackColorFraction(0.5f), 0f)
        assertEquals(1f, trackColorFraction(1.2f), 0f)
    }

    @Test fun restingThumbContrastsWithTrackThroughoutDrag() {
        val preset = AppearanceSettings(themeMode = ThemeMode.SYSTEM)
        val custom = preset.copy(accentPreset = AccentPreset.CUSTOM, customAccentHex = "#FFFFFF")
        for (settings in listOf(preset, custom)) for (dark in listOf(false, true)) {
            val tokens = resolveTheme(settings, systemDark = dark)
            val offColor = toggleTrackColor(tokens, checked = false).asColor()
            val onColor = toggleTrackColor(tokens, checked = true).asColor()
            for (step in 0..20) {
                val position = step / 20f
                val trackColor = lerp(offColor, onColor, trackColorFraction(position))
                val thumbColor = toggleThumbRestingColor(tokens, trackColor)
                val ratio = contrast(thumbColor.toArgb(), trackColor.toArgb())
                assertTrue("${settings.accentPreset} dark=$dark position=$position: $ratio < 3", ratio >= 3.0)
            }
        }
    }

    @Test fun lensRequiresMotionAndShaderSupport() {
        assertTrue(toggleGlassEnabled(degraded = false, animationsOff = false, shaderSupported = true))
        assertFalse(toggleGlassEnabled(degraded = true, animationsOff = false, shaderSupported = true))
        assertFalse(toggleGlassEnabled(degraded = false, animationsOff = true, shaderSupported = true))
        assertFalse(toggleGlassEnabled(degraded = false, animationsOff = false, shaderSupported = false))
    }
}
