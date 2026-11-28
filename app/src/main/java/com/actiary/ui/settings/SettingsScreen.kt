package com.actiary.ui.settings

import android.content.pm.ApplicationInfo
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.actiary.R
import com.actiary.data.appearance.AppLanguage
import com.actiary.data.appearance.AppearanceSettings
import com.actiary.data.appearance.ChartPalette
import com.actiary.data.appearance.GlassLevel
import com.actiary.data.appearance.P5MotionLevel
import com.actiary.data.appearance.P5Palette
import com.actiary.data.appearance.ThemeMode
import com.actiary.data.appearance.UiStyle
import com.actiary.data.appearance.WeekStart

@Composable
internal fun SettingsScreen(
    page: SettingsPage,
    onPage: (SettingsPage) -> Unit,
    settings: AppearanceSettings,
    dynamicSeed: Int,
    appLanguage: AppLanguage,
    onAppLanguage: (AppLanguage) -> Unit,
    weekStart: WeekStart,
    onWeekStart: (WeekStart) -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    onAmoledBlack: (Boolean) -> Unit,
    accentColors: List<String>,
    onSelectDynamicAccent: () -> Unit,
    onSelectAccent: (String) -> Unit,
    onRemoveAccent: (String) -> Unit,
    onGlassLevel: (GlassLevel) -> Unit,
    onAmbient: (Boolean) -> Unit,
    onResetAppearance: () -> Unit,
    onCustomColor: (String?) -> Unit,
    onChartPalette: (ChartPalette) -> Unit,
    onUiStyle: (UiStyle) -> Unit,
    onP5Palette: (P5Palette) -> Unit,
    onP5RansomTitle: (Boolean) -> Unit,
    onP5MotionLevel: (P5MotionLevel) -> Unit,
    onReduceInAppMotion: (Boolean) -> Unit = {},
    isP5IconEnabled: Boolean = false,
    onSelectAppIcon: (Boolean) -> Unit = {},
    typesContent: @Composable (() -> Unit) -> Unit,
    workoutPlansContent: @Composable (() -> Unit) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onCleanOrphanPhotos: () -> Unit,
    onStorageDetails: () -> Unit,
    storageRefreshKey: Int,
    onOpenNotices: () -> Unit,
    checkingUpdate: Boolean,
    onCheckUpdate: () -> Unit,
    onPreviewCheckIn: () -> Unit = {},
    onPreviewMilestone: () -> Unit = {},
) {
    val onBack = {
        val parent = page.parent()
        if (parent != null) onPage(parent)
    }
    when (page) {
        SettingsPage.HOME -> SettingsPageLayout(stringResource(R.string.settings_title)) {
            val chevron = Icons.AutoMirrored.Outlined.KeyboardArrowRight
            SettingsSectionHeader(stringResource(R.string.settings_section_preferences))
            SettingsGroup { rowShape ->
                SettingsRow(
                    stringResource(R.string.appearance_title), Icons.Outlined.Palette, chevron, shape = rowShape,
                ) { onPage(SettingsPage.APPEARANCE) }
                SettingsRow(
                    stringResource(R.string.settings_general), Icons.Outlined.Tune, chevron, shape = rowShape,
                ) { onPage(SettingsPage.GENERAL) }
            }
            SettingsSectionHeader(stringResource(R.string.settings_section_training))
            SettingsGroup { rowShape ->
                SettingsRow(
                    stringResource(R.string.settings_custom_types), Icons.Outlined.FitnessCenter, chevron, shape = rowShape,
                ) { onPage(SettingsPage.TYPES) }
                SettingsRow(
                    stringResource(R.string.strength_plans_title), Icons.AutoMirrored.Outlined.ListAlt, chevron, shape = rowShape,
                ) { onPage(SettingsPage.WORKOUT_PLANS) }
            }
            SettingsSectionHeader(stringResource(R.string.settings_section_system))
            SettingsGroup { rowShape ->
                SettingsRow(
                    stringResource(R.string.settings_data), Icons.Outlined.Folder, chevron, shape = rowShape,
                ) { onPage(SettingsPage.DATA) }
                SettingsRow(
                    stringResource(R.string.settings_about), Icons.Outlined.Info, chevron, shape = rowShape,
                ) { onPage(SettingsPage.ABOUT) }
            }
            val isDebugBuild = (LocalContext.current.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            if (isDebugBuild) {
                SettingsSectionHeader(stringResource(R.string.settings_debug_preview_section))
                SettingsGroup { rowShape ->
                    SettingsRow(
                        stringResource(R.string.settings_debug_preview_checkin),
                        Icons.Outlined.CheckCircle,
                        chevron,
                        shape = rowShape,
                        onClick = onPreviewCheckIn,
                    )
                    SettingsRow(
                        stringResource(R.string.settings_debug_preview_milestone),
                        Icons.Outlined.LocalFireDepartment,
                        chevron,
                        shape = rowShape,
                        onClick = onPreviewMilestone,
                    )
                }
            }
        }
        SettingsPage.APPEARANCE -> AppearanceScreen(
            settings = settings,
            dynamicSeed = dynamicSeed,
            onThemeMode = onThemeMode,
            onAmoledBlack = onAmoledBlack,
            accentColors = accentColors,
            onSelectDynamicAccent = onSelectDynamicAccent,
            onSelectAccent = onSelectAccent,
            onRemoveAccent = onRemoveAccent,
            onGlassLevel = onGlassLevel,
            onAmbient = onAmbient,
            onBack = onBack,
            onResetAppearance = onResetAppearance,
            onCustomColor = onCustomColor,
            onChartPalette = onChartPalette,
            onUiStyle = onUiStyle,
            onP5Palette = onP5Palette,
            onP5RansomTitle = onP5RansomTitle,
            onP5MotionLevel = onP5MotionLevel,
            isP5IconEnabled = isP5IconEnabled,
            onSelectAppIcon = onSelectAppIcon,
        )
        SettingsPage.GENERAL -> GeneralScreen(
            appLanguage = appLanguage,
            onAppLanguage = onAppLanguage,
            weekStart = weekStart,
            onWeekStart = onWeekStart,
            reduceInAppMotion = settings.reduceInAppMotion,
            onReduceInAppMotion = onReduceInAppMotion,
            onBack = onBack,
        )
        SettingsPage.TYPES -> typesContent(onBack)
        SettingsPage.WORKOUT_PLANS -> workoutPlansContent(onBack)
        SettingsPage.DATA -> DataScreen(onBack, onExportBackup, onImportBackup, onCleanOrphanPhotos, onStorageDetails, storageRefreshKey)
        SettingsPage.ABOUT -> AboutScreen(onBack, onOpenNotices, checkingUpdate, onCheckUpdate)
    }
}
