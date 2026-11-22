package com.actiary.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.actiary.R
import com.actiary.data.appearance.AppearanceSettings
import com.actiary.data.appearance.ChartPalette
import com.actiary.data.appearance.GlassLevel
import com.actiary.data.appearance.ThemeMode
import com.actiary.data.appearance.WeekStart

@Composable
internal fun SettingsScreen(
    page: SettingsPage,
    onPage: (SettingsPage) -> Unit,
    settings: AppearanceSettings,
    weekStart: WeekStart,
    onWeekStart: (WeekStart) -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    accentColors: List<String>,
    onSelectAccent: (String) -> Unit,
    onRemoveAccent: (String) -> Unit,
    onGlassLevel: (GlassLevel) -> Unit,
    onAmbient: (Boolean) -> Unit,
    onResetAppearance: () -> Unit,
    onCustomColor: (String?) -> Unit,
    onChartPalette: (ChartPalette) -> Unit,
    typesContent: @Composable (() -> Unit) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onCleanOrphanPhotos: () -> Unit,
) {
    val onBack = {
        val parent = page.parent()
        if (parent != null) onPage(parent)
    }
    when (page) {
        SettingsPage.HOME -> SettingsPageLayout(stringResource(R.string.settings_title)) {
            val chevron = Icons.AutoMirrored.Outlined.KeyboardArrowRight
            SettingsGroup { rowShape ->
                SettingsRow(
                    stringResource(R.string.appearance_title), Icons.Outlined.Palette, chevron, shape = rowShape,
                ) { onPage(SettingsPage.APPEARANCE) }
                SettingsRow(
                    stringResource(R.string.settings_general), Icons.Outlined.Tune, chevron, shape = rowShape,
                ) { onPage(SettingsPage.GENERAL) }
                SettingsRow(
                    stringResource(R.string.settings_custom_types), Icons.Outlined.FitnessCenter, chevron, shape = rowShape,
                ) { onPage(SettingsPage.TYPES) }
                SettingsRow(
                    stringResource(R.string.settings_data), Icons.Outlined.Folder, chevron, shape = rowShape,
                ) { onPage(SettingsPage.DATA) }
                SettingsRow(
                    stringResource(R.string.settings_about), Icons.Outlined.Info, chevron, shape = rowShape,
                ) { onPage(SettingsPage.ABOUT) }
            }
        }
        SettingsPage.APPEARANCE -> AppearanceScreen(
            settings, onThemeMode, accentColors, onSelectAccent, onRemoveAccent,
            onGlassLevel, onAmbient, onBack, onResetAppearance, onCustomColor, onChartPalette,
        )
        SettingsPage.GENERAL -> GeneralScreen(weekStart, onWeekStart, onBack)
        SettingsPage.TYPES -> typesContent(onBack)
        SettingsPage.DATA -> DataScreen(onBack, onExportBackup, onImportBackup, onCleanOrphanPhotos)
        SettingsPage.ABOUT -> AboutScreen(onBack)
    }
}
