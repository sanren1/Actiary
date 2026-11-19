package com.actiary.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.actiary.R
import com.actiary.domain.DayState
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor
import com.kizitonwose.calendar.compose.CalendarState
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as NameStyle
import java.util.Locale

private val CELL_HEIGHT = 48.dp

@Composable
fun MonthCalendar(
    state: CalendarState,
    states: Map<LocalDate, DayState>,
    selected: LocalDate,
    today: LocalDate,
    locale: Locale,
    onSelect: (LocalDate) -> Unit,
) {
    val t = LocalThemeTokens.current
    val scope = rememberCoroutineScope()
    val month = state.firstVisibleMonth.yearMonth
    fun go(target: YearMonth) = scope.launch { state.animateScrollToMonth(target) }
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                month.format(DateTimeFormatter.ofPattern(stringResource(R.string.home_month_pattern), locale)),
                Modifier.weight(1f),
                style = TextStyle(t.neutrals.textPrimary.asColor(), 18.sp, FontWeight.SemiBold),
            )
            NavButton(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, stringResource(R.string.home_prev_month),
                month > state.startMonth) { go(month.minusMonths(1)) }
            NavButton(Icons.AutoMirrored.Outlined.KeyboardArrowRight, stringResource(R.string.home_next_month),
                month < state.endMonth) { go(month.plusMonths(1)) }
        }
        HorizontalCalendar(
            state = state,
            dayContent = { day ->
                if (day.position == DayPosition.MonthDate) {
                    DayCell(day.date, states[day.date] ?: DayState.UNRECORDED, day.date == selected, day.date == today, onSelect)
                } else Box(Modifier.height(CELL_HEIGHT))
            },
            monthHeader = {
                Row(Modifier.fillMaxWidth()) {
                    for (d in daysOfWeek(firstDayOfWeek = state.firstDayOfWeek)) {
                        BasicText(
                            d.getDisplayName(NameStyle.SHORT, locale),
                            Modifier.weight(1f).padding(vertical = 6.dp),
                            style = TextStyle(t.neutrals.textSecondary.asColor(), 12.sp, textAlign = TextAlign.Center),
                        )
                    }
                }
            },
        )
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

@Composable
private fun DayCell(date: LocalDate, state: DayState, selected: Boolean, today: Boolean, onSelect: (LocalDate) -> Unit) {
    val t = LocalThemeTokens.current
    val locale = LocalConfiguration.current.locales[0]
    val stateText = stringResource(when (state) {
        DayState.EXERCISED -> R.string.day_state_logged
        DayState.REST -> R.string.day_rest
        DayState.UNRECORDED -> R.string.day_unrecorded
    })
    val description = listOfNotNull(
        date.format(DateTimeFormatter.ofPattern(stringResource(R.string.day_title_pattern), locale)),
        stateText,
        if (today) stringResource(R.string.day_today) else null,
    ).joinToString(", ")
    val mark = if (selected) t.accent.onAccentAction.asColor() else t.accent.accentAction.asColor()
    Column(
        Modifier.fillMaxWidth().height(CELL_HEIGHT).clickable(role = Role.Button) { onSelect(date) }
            .semantics(mergeDescendants = true) { contentDescription = description; this.selected = selected },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val ring = when {
            selected -> Modifier.background(t.accent.accentAction.asColor(), CircleShape)
            today -> Modifier.border(1.5.dp, t.accent.accentStroke.asColor(), CircleShape)
            else -> Modifier
        }
        Box(Modifier.size(32.dp).then(ring), contentAlignment = Alignment.Center) {
            BasicText(
                date.dayOfMonth.toString(),
                style = TextStyle(
                    if (selected) t.accent.onAccentAction.asColor() else t.neutrals.textPrimary.asColor(),
                    15.sp, if (today || selected) FontWeight.SemiBold else FontWeight.Normal,
                ),
            )
        }
        Box(Modifier.height(8.dp), contentAlignment = Alignment.Center) {
            when (state) {
                DayState.EXERCISED -> Box(Modifier.size(4.dp).background(mark, CircleShape))
                DayState.REST -> Box(
                    Modifier.width(8.dp).height(2.dp)
                        .background(if (selected) t.accent.onAccentAction.asColor() else t.neutrals.textSecondary.asColor()),
                )
                DayState.UNRECORDED -> {}
            }
        }
    }
}
