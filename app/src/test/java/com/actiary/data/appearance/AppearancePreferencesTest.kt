package com.actiary.data.appearance

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.actiary.domain.ChartMetric
import com.actiary.domain.RangeKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AppearancePreferencesTest {
    private fun repo() = AppearanceRepository(inMemoryPreferencesStore())

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

    @Test fun customAccentIsStoredUppercase() = runBlocking {
        val appearance = repo()
        appearance.setCustomAccent("#3a7bd5")
        assertEquals("#3A7BD5", appearance.settings.first().customAccentHex)
    }

    @Test fun resetKeepsNonAppearanceKeys() = runBlocking {
        val store = inMemoryPreferencesStore()
        val appearance = AppearanceRepository(store)
        val records = RecordsPrefsRepository(store)
        val general = GeneralPrefsRepository(store)

        records.setKind(RangeKind.WEEK)
        records.setMetric(ChartMetric.CALORIES)
        general.setWeekStart(WeekStart.SUNDAY)
        appearance.setThemeMode(ThemeMode.DARK)
        appearance.setCustomAccent("#123456")
        appearance.setGlassLevel(GlassLevel.CLEAR)
        appearance.setGlassTintStrength(GlassTintStrength.HIGH)
        appearance.setChartPalette(ChartPalette.FOLLOW_ACCENT)
        appearance.setAmbientBackground(false)

        appearance.resetAppearance()

        assertEquals(AppearanceSettings(), appearance.settings.first())
        assertEquals(RecordsView(RangeKind.WEEK, ChartMetric.CALORIES), records.view.first())
        assertEquals(WeekStart.SUNDAY, general.weekStart.first())
    }

    @Test fun pushRecentDedupesAndCaps() {
        val base = listOf("#BBBBBB", "#CCCCCC", "#DDDDDD", "#EEEEEE", "#FFFFFF")
        assertEquals(
            listOf("#AAAAAA", "#BBBBBB", "#CCCCCC", "#DDDDDD", "#EEEEEE"),
            pushRecent(base, "#AAAAAA"),
        )
        // 已存在的颜色被移到最前并去重
        assertEquals(
            listOf("#CCCCCC", "#AAAAAA", "#BBBBBB", "#DDDDDD", "#EEEEEE"),
            pushRecent(listOf("#AAAAAA", "#BBBBBB", "#CCCCCC", "#DDDDDD", "#EEEEEE"), "#CCCCCC"),
        )
    }

    @Test fun pushRecentNormalizesCase() {
        assertEquals(listOf("#3A7BD5"), pushRecent(emptyList(), "3a7bd5"))
        assertEquals(listOf("#3A7BD5"), pushRecent(emptyList(), " #3a7bd5 "))
        assertEquals(listOf("#3A7BD5"), pushRecent(listOf("#3a7bd5"), "#3A7BD5"))
    }

    @Test fun recentColorsDropsInvalid() = runBlocking {
        val store = inMemoryPreferencesStore()
        store.updateData {
            mutablePreferencesOf(
                stringPreferencesKey("recent_colors") to
                    "#111111,garbage,#12345,#222222,#333333,#444444,#555555,#666666",
            )
        }
        val r = AppearanceRepository(store)
        assertEquals(
            listOf("#111111", "#222222", "#333333", "#444444", "#555555"),
            r.recentColors.first(),
        )
    }

    @Test fun applyCustomAccentSetsPresetAndRecent() = runBlocking {
        val r = repo()
        r.applyCustomAccent("#3a7bd5")
        val s = r.settings.first()
        assertEquals(AccentPreset.CUSTOM, s.accentPreset)
        assertEquals("#3A7BD5", s.customAccentHex)
        assertEquals(listOf("#3A7BD5"), r.recentColors.first())
    }

    @Test fun resetClearsRecentColorsAndRestoresDefaultPalette() = runBlocking {
        val r = repo()
        val defaultPalette = r.accentColors.first()
        r.applyCustomAccent("#111111")
        r.applyCustomAccent("#222222")
        assertEquals(listOf("#222222", "#111111"), r.recentColors.first())
        r.resetAppearance()
        assertEquals(AppearanceSettings(), r.settings.first())
        assertEquals(emptyList<String>(), r.recentColors.first())
        assertEquals(defaultPalette, r.accentColors.first())
    }

    @Test fun removingAllPaletteColorsFallsBackToDefaultActiveAccent() = runBlocking {
        val r = repo()
        val initial = r.accentColors.first()
        for (hex in initial) {
            r.removePaletteAccent(hex)
        }
        assertEquals(emptyList<String>(), r.accentColors.first())
        assertEquals(AppearanceSettings().activeAccentHex(), r.settings.first().activeAccentHex())
    }

    @Test fun paletteEditsPreserveOrderAndUpdateOnlyActiveSwatch() = runBlocking {
        val r = repo()
        val original = r.accentColors.first()
        r.savePaletteAccent("#111111")
        r.savePaletteAccent("#222222")
        r.selectPaletteAccent("#111111")

        r.savePaletteAccent("#333333", previous = "#222222")
        assertEquals(original + listOf("#111111", "#333333"), r.accentColors.first())
        assertEquals("#111111", r.settings.first().customAccentHex)

        r.savePaletteAccent("#333333", previous = "#111111")
        assertEquals(original + "#333333", r.accentColors.first())
        assertEquals("#333333", r.settings.first().customAccentHex)

        r.removePaletteAccent("#333333")
        assertEquals(original, r.accentColors.first())
        assertEquals(original.first(), r.settings.first().activeAccentHex())
    }

    @Test fun paletteCanEditAndRemovePresetsAcrossBackup() = runBlocking {
        val source = repo()
        val original = source.accentColors.first()
        val active = source.settings.first().activeAccentHex()
        source.savePaletteAccent("#111111", previous = active)
        source.removePaletteAccent(original.first())
        assertEquals("#111111", source.settings.first().activeAccentHex())
        val destination = repo()
        destination.importAppearance(source.exportAppearance(), replace = true)
        assertEquals(source.accentColors.first(), destination.accentColors.first())
        assertEquals("#111111", destination.settings.first().activeAccentHex())
    }

    @Test fun exportImportAppearanceRoundTrip() = runBlocking {
        val sourceStore = inMemoryPreferencesStore()
        val source = AppearanceRepository(sourceStore)
        source.setThemeMode(ThemeMode.DARK)
        source.setAmbientBackground(false)
        source.applyCustomAccent("#123456")
        GeneralPrefsRepository(sourceStore).setWeekStart(WeekStart.SUNDAY)

        val destinationStore = inMemoryPreferencesStore()
        val destination = AppearanceRepository(destinationStore)
        destination.setGlassLevel(GlassLevel.CLEAR)
        destination.applyCustomAccent("#ABCDEF")
        val records = RecordsPrefsRepository(destinationStore)
        records.setKind(RangeKind.WEEK)
        records.setMetric(ChartMetric.CALORIES)
        val values = source.exportAppearance()

        assertEquals(mapOf(
            "theme_mode" to "DARK", "ambient_background" to "false",
            "custom_accent_hex" to "#123456", "accent_preset" to "CUSTOM",
        ), values)
        destination.importAppearance(values, replace = false)
        assertEquals(GlassLevel.CLEAR, destination.settings.first().glassLevel)
        destination.importAppearance(values, replace = true)
        assertEquals(source.settings.first(), destination.settings.first())
        assertEquals(listOf("#ABCDEF"), destination.recentColors.first())
        assertEquals(RecordsView(RangeKind.WEEK, ChartMetric.CALORIES), records.view.first())
        assertEquals(WeekStart.SYSTEM, GeneralPrefsRepository(destinationStore).weekStart.first())
    }

    @Test fun importDropsUnknownEnumValues() = runBlocking {
        val appearance = repo()
        appearance.setThemeMode(ThemeMode.DARK)
        appearance.setAmbientBackground(false)
        appearance.importAppearance(mapOf("theme_mode" to "PURPLE", "custom_accent_hex" to "bad"), replace = true)
        assertEquals(emptyMap<String, String>(), appearance.exportAppearance())
        assertEquals(AppearanceSettings(), appearance.settings.first())
    }

    @Test fun amoledBlackAndDynamicAccentRoundTripAndReset() = runBlocking {
        val r = repo()
        r.setAmoledBlack(true)
        r.selectDynamicAccent()
        val s = r.settings.first()
        assertEquals(true, s.amoledBlack)
        assertEquals(AccentPreset.DYNAMIC, s.accentPreset)

        val exported = r.exportAppearance()
        assertEquals("true", exported["amoled_black"])
        assertEquals("DYNAMIC", exported["accent_preset"])

        val dst = repo()
        dst.importAppearance(exported, replace = true)
        assertEquals(true, dst.settings.first().amoledBlack)
        assertEquals(AccentPreset.DYNAMIC, dst.settings.first().accentPreset)

        r.resetAppearance()
        assertEquals(false, r.settings.first().amoledBlack)
        assertEquals(AccentPreset.SLATE_BLUE, r.settings.first().accentPreset)
    }

    @Test fun defaultsIncludeLiquidGlassAndP5Defaults() {
        val defaults = AppearanceSettings()
        assertEquals(UiStyle.LIQUID_GLASS, defaults.uiStyle)
        assertEquals(P5Palette.PHANTOM_RED, defaults.p5Palette)
        assertEquals(true, defaults.p5RansomTitle)
        assertEquals(P5MotionLevel.STANDARD, defaults.p5MotionLevel)
        assertEquals(defaults, emptyPreferences().toAppearanceSettings())
    }

    @Test fun switchingUiStylePreservesOtherStyleSettings() = runBlocking {
        val r = repo()
        r.setThemeMode(ThemeMode.DARK)
        r.setAccentPreset(AccentPreset.SAGE)
        r.setGlassLevel(GlassLevel.CLEAR)

        r.setUiStyle(UiStyle.PERSONA_5)
        r.setP5Palette(P5Palette.ROYAL_GOLD)
        r.setP5RansomTitle(false)
        r.setP5MotionLevel(P5MotionLevel.COMPACT)

        r.setUiStyle(UiStyle.LIQUID_GLASS)

        val s = r.settings.first()
        assertEquals(UiStyle.LIQUID_GLASS, s.uiStyle)
        assertEquals(ThemeMode.DARK, s.themeMode)
        assertEquals(AccentPreset.SAGE, s.accentPreset)
        assertEquals(GlassLevel.CLEAR, s.glassLevel)
        assertEquals(P5Palette.ROYAL_GOLD, s.p5Palette)
        assertEquals(false, s.p5RansomTitle)
        assertEquals(P5MotionLevel.COMPACT, s.p5MotionLevel)
    }

    @Test fun resetAppearanceOnlyResetsActiveStyle() = runBlocking {
        val r = repo()
        r.setThemeMode(ThemeMode.DARK)
        r.setAccentPreset(AccentPreset.SAGE)
        r.setGlassLevel(GlassLevel.CLEAR)

        r.setUiStyle(UiStyle.PERSONA_5)
        r.setP5Palette(P5Palette.ROYAL_GOLD)
        r.setP5RansomTitle(false)
        r.setP5MotionLevel(P5MotionLevel.COMPACT)

        r.resetAppearance()
        val afterP5Reset = r.settings.first()
        assertEquals(UiStyle.PERSONA_5, afterP5Reset.uiStyle)
        assertEquals(P5Palette.PHANTOM_RED, afterP5Reset.p5Palette)
        assertEquals(true, afterP5Reset.p5RansomTitle)
        assertEquals(P5MotionLevel.STANDARD, afterP5Reset.p5MotionLevel)
        assertEquals(ThemeMode.DARK, afterP5Reset.themeMode)
        assertEquals(AccentPreset.SAGE, afterP5Reset.accentPreset)
        assertEquals(GlassLevel.CLEAR, afterP5Reset.glassLevel)

        r.setP5Palette(P5Palette.VELVET_BLUE)
        r.setP5RansomTitle(false)
        r.setP5MotionLevel(P5MotionLevel.COMPACT)

        r.setUiStyle(UiStyle.LIQUID_GLASS)
        r.resetAppearance()
        val afterGlassReset = r.settings.first()
        val defaults = AppearanceSettings()
        assertEquals(UiStyle.LIQUID_GLASS, afterGlassReset.uiStyle)
        assertEquals(defaults.themeMode, afterGlassReset.themeMode)
        assertEquals(defaults.accentPreset, afterGlassReset.accentPreset)
        assertEquals(defaults.glassLevel, afterGlassReset.glassLevel)
        assertEquals(P5Palette.VELVET_BLUE, afterGlassReset.p5Palette)
        assertEquals(false, afterGlassReset.p5RansomTitle)
        assertEquals(P5MotionLevel.COMPACT, afterGlassReset.p5MotionLevel)
    }

    @Test fun invalidP5EnumValuesFallbackToDefaults() = runBlocking {
        val store = inMemoryPreferencesStore()
        store.updateData {
            mutablePreferencesOf(
                stringPreferencesKey("ui_style") to "INVALID_STYLE",
                stringPreferencesKey("p5_palette") to "INVALID_PALETTE",
                stringPreferencesKey("p5_motion_level") to "INVALID_MOTION",
            )
        }
        val s = AppearanceRepository(store).settings.first()
        assertEquals(UiStyle.LIQUID_GLASS, s.uiStyle)
        assertEquals(P5Palette.PHANTOM_RED, s.p5Palette)
        assertEquals(P5MotionLevel.STANDARD, s.p5MotionLevel)
    }

    @Test fun reduceInAppMotionAndCallingCardRoundTripAndSurviveStyleReset() = runBlocking {
        val r = repo()
        assertEquals(false, r.settings.first().reduceInAppMotion)

        r.setReduceInAppMotion(true)
        r.setUiStyle(UiStyle.PERSONA_5)
        r.setP5Palette(P5Palette.CALLING_CARD)
        val updated = r.settings.first()
        assertEquals(true, updated.reduceInAppMotion)
        assertEquals(P5Palette.CALLING_CARD, updated.p5Palette)

        val exported = r.exportAppearance()
        assertEquals("true", exported["reduce_in_app_motion"])
        assertEquals("CALLING_CARD", exported["p5_palette"])

        val dst = repo()
        dst.importAppearance(exported, replace = true)
        assertEquals(true, dst.settings.first().reduceInAppMotion)
        assertEquals(P5Palette.CALLING_CARD, dst.settings.first().p5Palette)

        // resetAppearance resets only style-specific appearance keys, preserving reduceInAppMotion.
        r.resetAppearance()
        assertEquals(P5Palette.PHANTOM_RED, r.settings.first().p5Palette)
        assertEquals(true, r.settings.first().reduceInAppMotion)

        r.setUiStyle(UiStyle.LIQUID_GLASS)
        r.setThemeMode(ThemeMode.DARK)
        r.resetAppearance()
        assertEquals(ThemeMode.LIGHT, r.settings.first().themeMode)
        assertEquals(true, r.settings.first().reduceInAppMotion)
    }
}

// The file-based DataStore fails its tmp-file rename over an existing file on Windows JVM.
internal fun inMemoryPreferencesStore(): DataStore<Preferences> = object : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())
    override val data: Flow<Preferences> = state
    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        val next = transform(state.value)
        state.value = next
        return next
    }
}
