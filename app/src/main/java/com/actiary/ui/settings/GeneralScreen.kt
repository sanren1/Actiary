package com.actiary.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.actiary.R
import com.actiary.data.appearance.WeekStart
import com.actiary.ui.common.SelectionMenuRow

@Composable
internal fun GeneralScreen(weekStart: WeekStart, onWeekStart: (WeekStart) -> Unit, onBack: () -> Unit) {
    SettingsPageLayout(stringResource(R.string.settings_general), onBack) {
        SelectionMenuRow(
            label = stringResource(R.string.settings_week_start),
            options = WeekStart.entries.map { it to weekStartName(it) },
            selected = weekStart,
            onSelect = onWeekStart,
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
