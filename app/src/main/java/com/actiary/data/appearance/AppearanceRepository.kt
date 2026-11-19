package com.actiary.data.appearance

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.actiary.theme.parseHex
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.appearanceDataStore: DataStore<Preferences> by preferencesDataStore(name = "appearance")

private val THEME_MODE = stringPreferencesKey("theme_mode")
private val ACCENT_PRESET = stringPreferencesKey("accent_preset")
private val CUSTOM_ACCENT_HEX = stringPreferencesKey("custom_accent_hex")
private val GLASS_LEVEL = stringPreferencesKey("glass_level")
private val GLASS_TINT_STRENGTH = stringPreferencesKey("glass_tint_strength")
private val CHART_PALETTE = stringPreferencesKey("chart_palette")
private val AMBIENT_BACKGROUND = booleanPreferencesKey("ambient_background")

private inline fun <reified E : Enum<E>> Preferences.enumOr(key: Preferences.Key<String>, default: E): E =
    this[key]?.let { n -> enumValues<E>().firstOrNull { it.name == n } } ?: default

fun Preferences.toAppearanceSettings(): AppearanceSettings {
    val d = AppearanceSettings()
    return AppearanceSettings(
        themeMode = enumOr(THEME_MODE, d.themeMode),
        accentPreset = enumOr(ACCENT_PRESET, d.accentPreset),
        customAccentHex = this[CUSTOM_ACCENT_HEX]?.takeIf { parseHex(it) != null } ?: d.customAccentHex,
        glassLevel = enumOr(GLASS_LEVEL, d.glassLevel),
        glassTintStrength = enumOr(GLASS_TINT_STRENGTH, d.glassTintStrength),
        chartPalette = enumOr(CHART_PALETTE, d.chartPalette),
        ambientBackground = this[AMBIENT_BACKGROUND] ?: d.ambientBackground,
    )
}

class AppearanceRepository(private val store: DataStore<Preferences>) {
    val settings: Flow<AppearanceSettings> = store.data.map { it.toAppearanceSettings() }

    suspend fun setThemeMode(m: ThemeMode) { store.edit { it[THEME_MODE] = m.name } }
    suspend fun setAccentPreset(p: AccentPreset) { store.edit { it[ACCENT_PRESET] = p.name } }
    suspend fun setGlassLevel(l: GlassLevel) { store.edit { it[GLASS_LEVEL] = l.name } }
    suspend fun setGlassTintStrength(s: GlassTintStrength) { store.edit { it[GLASS_TINT_STRENGTH] = s.name } }
    suspend fun setChartPalette(p: ChartPalette) { store.edit { it[CHART_PALETTE] = p.name } }
    suspend fun setAmbientBackground(enabled: Boolean) { store.edit { it[AMBIENT_BACKGROUND] = enabled } }

    suspend fun setCustomAccent(hex: String) {
        require(parseHex(hex) != null) { "Invalid hex color: $hex" }
        store.edit {
            it[CUSTOM_ACCENT_HEX] = hex
            it[ACCENT_PRESET] = AccentPreset.CUSTOM.name
        }
    }

    suspend fun resetAppearance() { store.edit { it.clear() } }
}
