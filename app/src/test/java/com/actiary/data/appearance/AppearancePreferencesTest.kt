package com.actiary.data.appearance

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AppearancePreferencesTest {
    // In-memory store: the file-based DataStore fails its tmp-file rename over an existing file on Windows JVM.
    private fun repo() = AppearanceRepository(object : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        override val data: Flow<Preferences> = state
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            val next = transform(state.value)
            state.value = next
            return next
        }
    })

    @Test fun emptyIsDefault() {
        assertEquals(AppearanceSettings(), emptyPreferences().toAppearanceSettings())
    }

    @Test fun unknownEnumAndBadHexFallBack() {
        val p = mutablePreferencesOf(
            stringPreferencesKey("theme_mode") to "PURPLE",
            stringPreferencesKey("custom_accent_hex") to "#XYZ",
        )
        val s = p.toAppearanceSettings()
        assertEquals(ThemeMode.LIGHT, s.themeMode)
        assertEquals("#77879D", s.customAccentHex)
    }

    @Test fun allKeysMap() {
        val p = mutablePreferencesOf(
            stringPreferencesKey("theme_mode") to "DARK",
            stringPreferencesKey("accent_preset") to "SAND",
            stringPreferencesKey("custom_accent_hex") to "#123456",
            stringPreferencesKey("glass_level") to "CLEAR",
            stringPreferencesKey("glass_tint_strength") to "HIGH",
            stringPreferencesKey("chart_palette") to "FOLLOW_ACCENT",
            booleanPreferencesKey("ambient_background") to false,
        )
        assertEquals(
            AppearanceSettings(
                ThemeMode.DARK, AccentPreset.SAND, "#123456", GlassLevel.CLEAR,
                GlassTintStrength.HIGH, ChartPalette.FOLLOW_ACCENT, false,
            ),
            p.toAppearanceSettings(),
        )
    }

    @Test fun repositoryBehavior() = runBlocking {
        val r = repo()
        r.setCustomAccent("#123456")
        assertEquals(AppearanceSettings(accentPreset = AccentPreset.CUSTOM, customAccentHex = "#123456"), r.settings.first())
        r.setAccentPreset(AccentPreset.SAGE)
        assertEquals("#123456", r.settings.first().customAccentHex)
        assertEquals(AccentPreset.SAGE, r.settings.first().accentPreset)
        val before = r.settings.first()
        assertThrows(IllegalArgumentException::class.java) { runBlocking { r.setCustomAccent("bad") } }
        assertEquals(before, r.settings.first())
        r.setThemeMode(ThemeMode.DARK)
        r.setAmbientBackground(false)
        r.resetAppearance()
        assertEquals(AppearanceSettings(), r.settings.first())
    }

    @Test fun settersRoundTrip() = runBlocking {
        val r = repo()
        r.setGlassLevel(GlassLevel.CLEAR)
        r.setGlassTintStrength(GlassTintStrength.HIGH)
        r.setChartPalette(ChartPalette.FOLLOW_ACCENT)
        r.setAmbientBackground(false)
        val s = r.settings.first()
        assertEquals(GlassLevel.CLEAR, s.glassLevel)
        assertEquals(GlassTintStrength.HIGH, s.glassTintStrength)
        assertEquals(ChartPalette.FOLLOW_ACCENT, s.chartPalette)
        assertEquals(false, s.ambientBackground)
    }
}
