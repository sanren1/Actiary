package com.actiary.theme

import com.actiary.data.appearance.AppearanceSettings
import com.actiary.data.appearance.ThemeMode

fun resolveTheme(settings: AppearanceSettings, systemDark: Boolean): ThemeTokens {
    val dark = when (settings.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDark
    }
    val seed = settings.accentPreset.seed()
        ?: parseHex(settings.customAccentHex) ?: DEFAULT_SEED
    val n = if (dark) DarkNeutrals else LightNeutrals
    return ThemeTokens(dark, n, deriveAccent(seed, n, dark), deriveAmbient(seed, n, dark), settings.ambientBackground)
}
