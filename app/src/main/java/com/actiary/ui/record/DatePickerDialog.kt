package com.actiary.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor
import com.actiary.ui.calendar.MonthCalendar
import com.actiary.ui.calendar.isFuture
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun DatePickerDialog(selected: LocalDate, onSelect: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val t = LocalThemeTokens.current
    val locale = LocalConfiguration.current.locales[0]
    val today = LocalDate.now()
    val thisMonth = YearMonth.from(today)
    val state = rememberCalendarState(
        startMonth = thisMonth.minusYears(10),
        endMonth = thisMonth,
        firstVisibleMonth = YearMonth.from(selected),
        firstDayOfWeek = remember { firstDayOfWeekFromLocale() },
    )
    Dialog(onDismissRequest = onDismiss) {
        Box(Modifier.fillMaxWidth().background(t.neutrals.surface.asColor(), RoundedCornerShape(24.dp)).padding(16.dp)) {
            MonthCalendar(state, emptyMap(), selected, today, locale, onSelect) { !isFuture(it, today) }
        }
    }
}
