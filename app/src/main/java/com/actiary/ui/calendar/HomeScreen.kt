package com.actiary.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.actiary.R
import com.actiary.data.RecordRepository
import com.actiary.data.photo.PhotoStore
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    repository: RecordRepository,
    photoStore: PhotoStore,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    onOpenAppearance: () -> Unit,
    onEdit: (LocalDate) -> Unit,
) {
    val t = LocalThemeTokens.current
    val locale = LocalConfiguration.current.locales[0]
    var today by remember { mutableStateOf(LocalDate.now()) }
    LifecycleResumeEffect(Unit) {
        today = LocalDate.now()
        onPauseOrDispose {}
    }
    val thisMonth = YearMonth.from(today)
    val calendar = rememberCalendarState(
        startMonth = thisMonth.minusYears(10),
        endMonth = thisMonth.plusYears(1),
        firstVisibleMonth = YearMonth.from(selected),
        firstDayOfWeek = remember { firstDayOfWeekFromLocale() },
    )
    val visible = calendar.firstVisibleMonth.yearMonth
    // Keyed with their month/date so a stale value from the previous key never reads as "loaded".
    val month by remember(visible) { repository.observeMonth(visible).map { visible to it } }.collectAsStateWithLifecycle(null)
    val monthStates = month?.takeIf { it.first == visible }?.second.orEmpty()
    val day by remember(selected) { repository.observeDay(selected).map { selected to it } }.collectAsStateWithLifecycle(null)
    val loading = day?.first != selected
    val detail = day?.second
    val types by repository.types.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    val canRecord = !isFuture(selected, today)

    Box(Modifier.fillMaxSize().background(t.neutrals.background.asColor()).safeDrawingPadding()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    stringResource(R.string.home_title),
                    Modifier.weight(1f),
                    style = TextStyle(t.neutrals.textPrimary.asColor(), 28.sp, FontWeight.Bold),
                )
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(t.neutrals.surfaceMuted.asColor())
                        .clickable(role = Role.Button, onClick = onOpenAppearance),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.MoreHoriz, stringResource(R.string.appearance_title), tint = t.neutrals.textPrimary.asColor())
                }
            }
            MonthCalendar(calendar, monthStates, selected, today, locale, onSelect)
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 16.dp, bottom = 96.dp)) {
                DayDetail(
                    title = selected.format(DateTimeFormatter.ofPattern(stringResource(R.string.day_title_pattern), locale)),
                    detail = detail,
                    types = types.associateBy { it.id },
                    photoStore = photoStore,
                    loading = loading,
                )
                if (canRecord && !loading && detail == null) {
                    BasicText(
                        stringResource(R.string.home_quick_check_in),
                        Modifier.heightIn(min = 48.dp).clickable(role = Role.Button) {
                            scope.launch { repository.quickCheckIn(selected) }
                        }.padding(top = 12.dp),
                        style = TextStyle(t.accent.accentStroke.asColor(), 15.sp, FontWeight.Medium),
                    )
                }
            }
        }
        if (canRecord && !loading) {
            Row(
                Modifier.align(Alignment.BottomEnd).padding(16.dp).heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(50)).background(t.accent.accentAction.asColor())
                    .clickable(role = Role.Button) { onEdit(selected) }
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Outlined.Edit, null, Modifier.size(18.dp), tint = t.accent.onAccentAction.asColor())
                BasicText(
                    stringResource(if (detail == null) R.string.home_log else R.string.home_edit),
                    style = TextStyle(t.accent.onAccentAction.asColor(), 15.sp, FontWeight.SemiBold),
                )
            }
        }
    }
}
