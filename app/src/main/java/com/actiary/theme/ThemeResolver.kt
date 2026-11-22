package com.actiary.theme

import com.actiary.data.appearance.AccentPreset
import com.actiary.data.appearance.AppearanceSettings
import com.actiary.data.appearance.ThemeMode

fun resolveTheme(settings: AppearanceSettings, systemDark: Boolean, dynamicSeed: Int? = null): ThemeTokens {
    val dark = when (settings.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDark
    }
    val seed = when (settings.accentPreset) {
        AccentPreset.DYNAMIC -> dynamicSeed ?: DEFAULT_SEED
        AccentPreset.CUSTOM -> parseHex(settings.customAccentHex) ?: DEFAULT_SEED
        else -> settings.accentPreset.seed() ?: DEFAULT_SEED
    }
    val n = when {
        !dark -> LightNeutrals
        settings.amoledBlack -> AmoledDarkNeutrals
        else -> DarkNeutrals
    }
    return ThemeTokens(dark, n, deriveAccent(seed, n, dark), deriveAmbient(seed, n, dark), settings.ambientBackground)
}
