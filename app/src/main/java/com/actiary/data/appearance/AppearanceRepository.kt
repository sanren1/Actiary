package com.actiary.data.appearance

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.actiary.theme.normalizeHex
import com.actiary.theme.parseHex
import com.actiary.theme.seed
import com.actiary.theme.toHex
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.appearanceDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "appearance",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

private val THEME_MODE = stringPreferencesKey("theme_mode")
private val ACCENT_PRESET = stringPreferencesKey("accent_preset")
private val CUSTOM_ACCENT_HEX = stringPreferencesKey("custom_accent_hex")
private val GLASS_LEVEL = stringPreferencesKey("glass_level")
private val GLASS_TINT_STRENGTH = stringPreferencesKey("glass_tint_strength")
private val CHART_PALETTE = stringPreferencesKey("chart_palette")
internal val AMBIENT_BACKGROUND = booleanPreferencesKey("ambient_background")
private val RECENT_COLORS = stringPreferencesKey("recent_colors")
private val ACCENT_COLORS = stringPreferencesKey("accent_colors")

internal val builtInAccentColors = AccentPreset.entries.mapNotNull { preset ->
    preset.seed()?.let { it.toHex() to preset }
}.toMap()

private val appearanceValidators = mapOf<Preferences.Key<String>, (String) -> Boolean>(
    THEME_MODE to { value -> ThemeMode.entries.any { it.name == value } },
    ACCENT_PRESET to { value -> AccentPreset.entries.any { it.name == value } },
    CUSTOM_ACCENT_HEX to { parseHex(it) != null },
    GLASS_LEVEL to { value -> GlassLevel.entries.any { it.name == value } },
    GLASS_TINT_STRENGTH to { value -> GlassTintStrength.entries.any { it.name == value } },
    CHART_PALETTE to { value -> ChartPalette.entries.any { it.name == value } },
    ACCENT_COLORS to { value -> value.isEmpty() || value.split(',').all { normalizeHex(it) != null } },
)
internal val backupAppearanceStringKeys = appearanceValidators.keys

internal inline fun <reified E : Enum<E>> Preferences.enumOr(key: Preferences.Key<String>, default: E): E =
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

/** 丢弃非法项与重复项，保留最多 5 个、最新的在前。 */
private fun parseRecent(raw: String?): List<String> =
    if (raw.isNullOrBlank()) emptyList()
    else raw.split(',').mapNotNull(::normalizeHex).distinct().take(5)

/** 规范化 hex 后置顶，去重，最多保留 5 色。非法 hex 时原样返回。 */
fun pushRecent(list: List<String>, hex: String): List<String> {
    val normalized = normalizeHex(hex) ?: return list
    val result = ArrayList<String>(5)
    result.add(normalized)
    for (item in list) {
        val n = normalizeHex(item) ?: continue
        if (n !in result) result.add(n)
        if (result.size == 5) break
    }
    return result
}

internal fun AppearanceSettings.activeAccentHex(): String =
    accentPreset.seed()?.toHex() ?: checkNotNull(normalizeHex(customAccentHex))

private fun Preferences.palette(): List<String> {
    val saved = this[ACCENT_COLORS]
    // Before the palette was editable, recent colors and the active accent were its only custom swatches.
    return if (saved == null) {
        (builtInAccentColors.keys + parseRecent(this[RECENT_COLORS]) + toAppearanceSettings().activeAccentHex()).toList()
    } else {
        saved.split(',').mapNotNull(::normalizeHex).distinct()
    }
}

private fun MutablePreferences.activatePaletteColor(hex: String) {
    val preset = builtInAccentColors[hex] ?: AccentPreset.CUSTOM
    this[ACCENT_PRESET] = preset.name
    if (preset == AccentPreset.CUSTOM) this[CUSTOM_ACCENT_HEX] = hex
}

class AppearanceRepository(private val store: DataStore<Preferences>) {
    val settings: Flow<AppearanceSettings> = store.data.map { it.toAppearanceSettings() }
    val recentColors: Flow<List<String>> = store.data.map { parseRecent(it[RECENT_COLORS]) }
    val accentColors: Flow<List<String>> = store.data.map { it.palette() }

    suspend fun setThemeMode(m: ThemeMode) { store.edit { it[THEME_MODE] = m.name } }
    suspend fun setAccentPreset(p: AccentPreset) { store.edit { it[ACCENT_PRESET] = p.name } }
    suspend fun setGlassLevel(l: GlassLevel) { store.edit { it[GLASS_LEVEL] = l.name } }
    suspend fun setGlassTintStrength(s: GlassTintStrength) { store.edit { it[GLASS_TINT_STRENGTH] = s.name } }
    suspend fun setChartPalette(p: ChartPalette) { store.edit { it[CHART_PALETTE] = p.name } }
    suspend fun setAmbientBackground(enabled: Boolean) { store.edit { it[AMBIENT_BACKGROUND] = enabled } }

    suspend fun setCustomAccent(hex: String) {
        require(parseHex(hex) != null) { "Invalid hex color: $hex" }
        store.edit {
            it[CUSTOM_ACCENT_HEX] = hex.uppercase()
            it[ACCENT_PRESET] = AccentPreset.CUSTOM.name
        }
    }

    suspend fun applyCustomAccent(hex: String) {
        val normalized = normalizeHex(hex) ?: throw IllegalArgumentException("Invalid hex color: $hex")
        store.edit {
            it[CUSTOM_ACCENT_HEX] = normalized
            it[ACCENT_PRESET] = AccentPreset.CUSTOM.name
            it[RECENT_COLORS] = pushRecent(parseRecent(it[RECENT_COLORS]), normalized).joinToString(",")
        }
    }

    suspend fun selectPaletteAccent(hex: String) {
        store.edit { prefs ->
            if (hex in prefs.palette()) prefs.activatePaletteColor(hex)
        }
    }

    suspend fun savePaletteAccent(hex: String, previous: String? = null) {
        val normalized = requireNotNull(normalizeHex(hex)) { "Invalid hex color: $hex" }
        store.edit { prefs ->
            val colors = prefs.palette()
            if (previous != null && previous !in colors) return@edit
            val next = when {
                previous == null -> (colors + normalized).distinct()
                normalized == previous -> colors
                normalized in colors -> colors - previous
                else -> colors.map { if (it == previous) normalized else it }
            }
            prefs[ACCENT_COLORS] = next.joinToString(",")
            val history = parseRecent(prefs[RECENT_COLORS]).filterNot { it == previous }
            prefs[RECENT_COLORS] = pushRecent(history, normalized).joinToString(",")
            if (previous == null || prefs.toAppearanceSettings().activeAccentHex() == previous) {
                prefs.activatePaletteColor(normalized)
            }
        }
    }

    suspend fun removePaletteAccent(hex: String) {
        store.edit { prefs ->
            val colors = prefs.palette()
            if (hex !in colors) return@edit
            val next = colors - hex
            prefs[ACCENT_COLORS] = next.joinToString(",")
            prefs[RECENT_COLORS] = (parseRecent(prefs[RECENT_COLORS]) - hex).joinToString(",")
            if (prefs.toAppearanceSettings().activeAccentHex() == hex) {
                prefs.activatePaletteColor(next.firstOrNull() ?: AppearanceSettings().activeAccentHex())
            }
        }
    }

    suspend fun resetAppearance() = restoreAppearance(emptyMap(), clearRecent = true)

    suspend fun exportAppearance(): Map<String, String> {
        val preferences = store.data.first()
        return buildMap {
            for (key in backupAppearanceStringKeys) preferences[key]?.let { put(key.name, it) }
            preferences[AMBIENT_BACKGROUND]?.let { put(AMBIENT_BACKGROUND.name, it.toString()) }
        }
    }

    suspend fun importAppearance(values: Map<String, String>, replace: Boolean) {
        if (!replace) return
        val validated = buildMap {
            for ((key, isValid) in appearanceValidators) {
                values[key.name]?.takeIf(isValid)?.let { put(key.name, it) }
            }
            values[AMBIENT_BACKGROUND.name]?.toBooleanStrictOrNull()?.let {
                put(AMBIENT_BACKGROUND.name, it.toString())
            }
        }
        restoreAppearance(validated)
    }

    // Restore the exact local snapshot if a Room commit fails after the DataStore edit.
    internal suspend fun restoreAppearance(values: Map<String, String>, clearRecent: Boolean = false) {
        store.edit { preferences ->
            for (key in backupAppearanceStringKeys) {
                val value = values[key.name]
                if (value == null) preferences.remove(key) else preferences[key] = value
            }
            val ambient = values[AMBIENT_BACKGROUND.name]
            if (ambient == null) preferences.remove(AMBIENT_BACKGROUND)
            else preferences[AMBIENT_BACKGROUND] = ambient.toBooleanStrict()
            if (clearRecent) preferences.remove(RECENT_COLORS)
        }
    }
}
