package com.actiary.ui.record

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import com.actiary.glass.GlassIconButton
import com.actiary.glass.GlassOverlayPanel
import com.actiary.glass.rememberOverlayVisibility
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.actiary.R
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor
import com.actiary.ui.calendar.MonthCalendar
import com.actiary.ui.calendar.isFuture
import com.kizitonwose.calendar.compose.rememberCalendarState
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun DatePickerDialog(
    visible: Boolean,
    selected: LocalDate,
    firstDayOfWeek: DayOfWeek,
    onSelect: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    title: String = "",
    visibility: MutableTransitionState<Boolean> = rememberOverlayVisibility(visible),
) {
    val t = LocalThemeTokens.current
    val locale = LocalConfiguration.current.locales[0]
    val today = LocalDate.now()
    val thisMonth = YearMonth.from(today)
    GlassOverlayPanel(visible, onDismiss, PaddingValues(16.dp), title, visibility) {
        val state = rememberCalendarState(
            startMonth = thisMonth.minusYears(10),
            endMonth = thisMonth,
            firstVisibleMonth = YearMonth.from(selected),
            firstDayOfWeek = remember { firstDayOfWeek },
        )
        SideEffect { state.firstDayOfWeek = firstDayOfWeek }
        val scope = rememberCoroutineScope()
        val month = state.firstVisibleMonth.yearMonth
        if (title.isNotEmpty()) {
            BasicText(title, Modifier.padding(bottom = 8.dp), style = TextStyle(t.neutrals.textPrimary.asColor(), 16.sp, FontWeight.Medium))
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                month.format(DateTimeFormatter.ofPattern(stringResource(R.string.home_month_pattern), locale)),
                Modifier.weight(1f),
                style = TextStyle(t.neutrals.textPrimary.asColor(), 18.sp, FontWeight.SemiBold),
            )
            GlassIconButton(
                Icons.AutoMirrored.Outlined.KeyboardArrowLeft, stringResource(R.string.home_prev_month),
                { scope.launch { state.animateScrollToMonth(month.minusMonths(1)) } }, enabled = month > state.startMonth, glass = false,
            )
            Spacer(Modifier.width(4.dp))
            GlassIconButton(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight, stringResource(R.string.home_next_month),
                { scope.launch { state.animateScrollToMonth(month.plusMonths(1)) } }, enabled = month < state.endMonth, glass = false,
            )
        }
        MonthCalendar(state, emptyMap(), selected, today, locale, onSelect) { !isFuture(it, today) }
    }
}
