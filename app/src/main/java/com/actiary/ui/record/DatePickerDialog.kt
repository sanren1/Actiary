package com.actiary.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.actiary.R
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
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
    val scope = rememberCoroutineScope()
    val month = state.firstVisibleMonth.yearMonth
    // Still an M3 Dialog window (opened from the M3 record sheet); becomes an in-tree glass overlay with the sheet in Task 5.
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().background(t.neutrals.surface.asColor(), RoundedCornerShape(24.dp)).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    month.format(DateTimeFormatter.ofPattern(stringResource(R.string.home_month_pattern), locale)),
                    Modifier.weight(1f),
                    style = TextStyle(t.neutrals.textPrimary.asColor(), 18.sp, FontWeight.SemiBold),
                )
                NavButton(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, stringResource(R.string.home_prev_month), month > state.startMonth) {
                    scope.launch { state.animateScrollToMonth(month.minusMonths(1)) }
                }
                NavButton(Icons.AutoMirrored.Outlined.KeyboardArrowRight, stringResource(R.string.home_next_month), month < state.endMonth) {
                    scope.launch { state.animateScrollToMonth(month.plusMonths(1)) }
                }
            }
            MonthCalendar(state, emptyMap(), selected, today, locale, onSelect) { !isFuture(it, today) }
        }
    }
}

@Composable
private fun NavButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    val t = LocalThemeTokens.current
    Box(
        Modifier.size(48.dp).clip(CircleShape).clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = t.neutrals.textPrimary.asColor().copy(alpha = if (enabled) 1f else 0.3f))
    }
}
