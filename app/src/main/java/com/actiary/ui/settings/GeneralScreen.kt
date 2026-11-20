package com.actiary.ui.settings

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.actiary.R
import com.actiary.data.appearance.WeekStart
import com.actiary.glass.GlassSegmentedPicker
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor

@Composable
internal fun GeneralScreen(weekStart: WeekStart, onWeekStart: (WeekStart) -> Unit, onBack: () -> Unit) {
    val n = LocalThemeTokens.current.neutrals
    SettingsPageLayout(stringResource(R.string.settings_general), onBack) {
        BasicText(stringResource(R.string.settings_week_start), style = TextStyle(n.textPrimary.asColor(), 16.sp))
        GlassSegmentedPicker(
            WeekStart.entries.map { weekStartName(it) },
            weekStart.ordinal,
            { onWeekStart(WeekStart.entries[it]) },
        )
    }
}

@Composable
internal fun weekStartName(value: WeekStart): String = stringResource(
    when (value) {
        WeekStart.SYSTEM -> R.string.settings_week_system
        WeekStart.MONDAY -> R.string.settings_week_monday
        WeekStart.SUNDAY -> R.string.settings_week_sunday
    },
)
