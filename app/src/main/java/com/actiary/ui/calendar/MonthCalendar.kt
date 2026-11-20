package com.actiary.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
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
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as NameStyle
import java.util.Locale

internal val CELL_HEIGHT = 48.dp

@Composable
fun MonthCalendar(
    state: CalendarState,
    states: Map<LocalDate, DayState>,
    selected: LocalDate,
    today: LocalDate,
    locale: Locale,
    onSelect: (LocalDate) -> Unit,
    collapse: Float = 0f,
    userScrollEnabled: Boolean = true,
    isEnabled: (LocalDate) -> Boolean = { true },
) {
    val t = LocalThemeTokens.current
    val cell = with(LocalDensity.current) { CELL_HEIGHT.toPx() }
    val month = state.firstVisibleMonth.yearMonth
    val rows = keptRowIndex(month, month.atEndOfMonth(), state.firstDayOfWeek) + 1
    val kept = keptRowIndex(month, selected, state.firstDayOfWeek)
    Column {
        Row(Modifier.fillMaxWidth()) {
            for (d in daysOfWeek(firstDayOfWeek = state.firstDayOfWeek)) {
                BasicText(
                    d.getDisplayName(NameStyle.SHORT, locale),
                    Modifier.weight(1f).padding(vertical = 6.dp),
                    style = TextStyle(t.neutrals.textSecondary.asColor(), 12.sp, textAlign = TextAlign.Center),
                )
            }
        }
        // The grid slides inside a clipped box so the kept week row ends up at the top.
        Box(Modifier.fillMaxWidth().layout { m, c ->
            val pl = m.measure(c.copy(maxHeight = Constraints.Infinity))
            // Expanded: let the pager wrap to its tallest visible page; only force the height while collapsing.
            val h = if (collapse > 0f) collapsedHeight(rows * cell, cell, collapse).roundToInt() else pl.height
            layout(pl.width, h) { pl.place(0, 0) }
        }.clipToBounds()) {
            HorizontalCalendar(
                modifier = Modifier.wrapContentHeight(Alignment.Top, unbounded = true)
                    .graphicsLayer { translationY = if (collapse > 0f) rowOffset(kept, cell, collapse) else 0f },
                state = state,
                userScrollEnabled = userScrollEnabled,
                dayContent = { day ->
                    if (day.position == DayPosition.MonthDate) {
                        // Fully collapsed: rows clipped away must not stay reachable for TalkBack.
                        val hidden = collapse >= 1f && keptRowIndex(YearMonth.from(day.date), day.date, state.firstDayOfWeek) != kept
                        DayCell(day.date, states[day.date] ?: DayState.UNRECORDED, day.date == selected, day.date == today, onSelect, isEnabled(day.date), hidden)
                    } else Box(Modifier.height(CELL_HEIGHT))
                },
            )
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, state: DayState, selected: Boolean, today: Boolean, onSelect: (LocalDate) -> Unit, enabled: Boolean, hidden: Boolean) {
    val t = LocalThemeTokens.current
    val locale = LocalConfiguration.current.locales[0]
    val stateText = stringResource(when (state) {
        DayState.EXERCISED -> R.string.day_state_logged
        DayState.REST -> R.string.day_rest
        DayState.UNRECORDED -> R.string.day_unrecorded
    })
    val description = listOfNotNull(
        date.format(DateTimeFormatter.ofPattern(stringResource(R.string.day_accessibility_pattern), locale)),
        stateText,
        if (today) stringResource(R.string.day_today) else null,
    ).joinToString(", ")
    val mark = if (selected) t.accent.onAccentAction.asColor() else t.accent.accentAction.asColor()
    Column(
        Modifier.fillMaxWidth().height(CELL_HEIGHT).alpha(if (enabled) 1f else 0.35f)
            .clickable(enabled = enabled, role = Role.Button) { onSelect(date) }
            .semantics(mergeDescendants = true) {
                contentDescription = description; this.selected = selected
                if (hidden) hideFromAccessibility()
            },
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
