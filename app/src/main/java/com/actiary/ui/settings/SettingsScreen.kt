package com.actiary.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.actiary.R
import com.actiary.data.appearance.AccentPreset
import com.actiary.data.appearance.AppearanceSettings
import com.actiary.data.appearance.ChartPalette
import com.actiary.data.appearance.GlassLevel
import com.actiary.data.appearance.GlassTintStrength
import com.actiary.data.appearance.ThemeMode
import com.actiary.data.appearance.WeekStart
import com.actiary.glass.GlassPanel

@Composable
internal fun SettingsScreen(
    page: SettingsPage,
    onPage: (SettingsPage) -> Unit,
    settings: AppearanceSettings,
    weekStart: WeekStart,
    onWeekStart: (WeekStart) -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    onPreset: (AccentPreset) -> Unit,
    onGlassLevel: (GlassLevel) -> Unit,
    onGlassTint: (GlassTintStrength) -> Unit,
    onAmbient: (Boolean) -> Unit,
    onResetAppearance: () -> Unit,
    onCustomColor: () -> Unit,
    onChartPalette: (ChartPalette) -> Unit,
    recent: List<String>,
    onApplyCustomAccent: (String) -> Unit,
    typesContent: @Composable (() -> Unit) -> Unit,
) {
    val onBack = {
        val parent = page.parent()
        if (parent != null) onPage(parent)
    }
    when (page) {
        SettingsPage.HOME -> SettingsPageLayout(stringResource(R.string.settings_title)) {
            val chevron = Icons.AutoMirrored.Outlined.KeyboardArrowRight
            GlassPanel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsRow(
                        stringResource(R.string.appearance_title), Icons.Outlined.Palette, chevron,
                    ) { onPage(SettingsPage.APPEARANCE) }
                    SettingsRow(
                        stringResource(R.string.settings_general), Icons.Outlined.Tune, chevron,
                    ) { onPage(SettingsPage.GENERAL) }
                    SettingsRow(
                        stringResource(R.string.settings_custom_types), Icons.Outlined.FitnessCenter, chevron,
                    ) { onPage(SettingsPage.TYPES) }
                    SettingsRow(
                        stringResource(R.string.settings_about), Icons.Outlined.Info, chevron,
                    ) { onPage(SettingsPage.ABOUT) }
                }
            }
        }
        SettingsPage.APPEARANCE -> AppearanceScreen(
            settings, onThemeMode, onPreset, onGlassLevel, onGlassTint, onAmbient, onBack, onResetAppearance,
            onCustomColor, onChartPalette,
        )
        SettingsPage.GENERAL -> GeneralScreen(weekStart, onWeekStart, onBack)
        SettingsPage.TYPES -> typesContent(onBack)
        SettingsPage.ABOUT -> AboutScreen(onBack)
        SettingsPage.CUSTOM_COLOR -> ColorEditorScreen(
            settings = settings,
            recent = recent,
            onApply = onApplyCustomAccent,
            onBack = onBack,
        )
    }
}
