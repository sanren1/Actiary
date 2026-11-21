package com.actiary.ui.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
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
import com.actiary.glass.GlassIconButton
import com.actiary.glass.GlassPanel
import com.actiary.glass.GlassSampling
import com.actiary.glass.rememberAnimationsOff
import com.actiary.glass.rememberGlassLayer
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor
import com.actiary.ui.common.LocalFloatingInsets
import com.actiary.ui.common.RootTitle
import com.kizitonwose.calendar.compose.rememberCalendarState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val BOTTOM_CLEARANCE = 24.dp

@Composable
fun HomeScreen(
    repository: RecordRepository,
    photoStore: PhotoStore,
    selected: LocalDate,
    firstDayOfWeek: DayOfWeek,
    onSelect: (LocalDate) -> Unit,
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
        firstVisibleMonth = remember { YearMonth.from(selected) },
        firstDayOfWeek = remember { firstDayOfWeek },
    )
    // Update the saved pager in place so changing the week layout retains the visible month.
    SideEffect { calendar.firstDayOfWeek = firstDayOfWeek }
    var alignedSelection by rememberSaveable { mutableStateOf(selected) }
    LaunchedEffect(selected) {
        if (selected != alignedSelection) {
            val m = YearMonth.from(selected).coerceIn(calendar.startMonth, calendar.endMonth)
            if (m != calendar.firstVisibleMonth.yearMonth) calendar.animateScrollToMonth(m)
            alignedSelection = selected
        }
    }
    val visible = calendar.firstVisibleMonth.yearMonth
    // Keyed with their month/date so a stale value from the previous key never reads as "loaded".
    val month by remember(visible) { repository.observeMonth(visible).map { visible to it } }.collectAsStateWithLifecycle(null)
    val monthStates = month?.takeIf { it.first == visible }?.second.orEmpty()
    val day by remember(selected) { repository.observeDay(selected).map { selected to it } }.collectAsStateWithLifecycle(null)
    val loading = day?.first != selected
    val detail = day?.second
    val types by repository.allTypes.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    val canRecord = !isFuture(selected, today)

    // Collapse progress of the calendar: 0 = full month, 1 = only the selected week.
    var p by rememberSaveable { mutableStateOf(0f) }
    val animationsOff = rememberAnimationsOff()
    // One settle animation at a time; any new drag or settle cancels the previous one.
    var settling by remember { mutableStateOf<Job?>(null) }
    val settle by rememberUpdatedState<(Float) -> Unit> { target ->
        settling?.cancel()
        if (animationsOff) p = target
        else settling = scope.launch { animate(p, target, animationSpec = tween(150)) { v, _ -> p = v } }
    }
    val lastRow = keptRowIndex(visible, visible.atEndOfMonth(), calendar.firstDayOfWeek)
    val rangePx = with(LocalDensity.current) { (CELL_HEIGHT * lastRow).toPx() }
    val range by rememberUpdatedState(rangePx)
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                settling?.cancel()
                if (available.y >= 0f || p >= 1f || range <= 0f) return Offset.Zero
                val next = (p - available.y / range).coerceAtMost(1f)
                val used = -(next - p) * range
                p = next
                return Offset(0f, used)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                settling?.cancel()
                if (available.y <= 0f || p <= 0f || range <= 0f) return Offset.Zero
                val next = (p - available.y / range).coerceAtLeast(0f)
                val used = -(next - p) * range
                p = next
                return Offset(0f, used)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (p > 0f && p < 1f) settle(snapTarget(p))
                return Velocity.Zero
            }
        }
    }
    val detailScroll = rememberScrollState()

    val calendarLayer = rememberGlassLayer()
    val primary = t.neutrals.textPrimary.asColor()
    fun go(target: YearMonth) = scope.launch {
        if (p > 0f) settle(0f)
        calendar.animateScrollToMonth(target)
    }
    Column(
        Modifier.fillMaxSize().nestedScroll(connection)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RootTitle(stringResource(R.string.tab_calendar), Modifier.padding(end = 56.dp))
        GlassPanel(Modifier.fillMaxWidth(), exported = calendarLayer) {
            Column(Modifier.padding(12.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicText(
                        visible.format(DateTimeFormatter.ofPattern(stringResource(R.string.home_month_pattern), locale)),
                        Modifier.weight(1f).padding(start = 8.dp),
                        style = TextStyle(primary, 18.sp, FontWeight.SemiBold),
                    )
                    val hasPrev = visible > calendar.startMonth
                    val hasNext = visible < calendar.endMonth
                    GlassIconButton(
                        Icons.AutoMirrored.Outlined.KeyboardArrowLeft, stringResource(R.string.home_prev_month),
                        { go(visible.minusMonths(1)) }, Modifier,
                        GlassSampling.Background, calendarLayer, hasPrev, iconSize = 28.dp,
                    )
                    GlassIconButton(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight, stringResource(R.string.home_next_month),
                        { go(visible.plusMonths(1)) }, Modifier,
                        GlassSampling.Background, calendarLayer, hasNext, iconSize = 28.dp,
                    )
                }
                MonthCalendar(calendar, monthStates, selected, today, locale, onSelect, collapse = p, userScrollEnabled = p < 1f)
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(detailScroll)
                .padding(bottom = LocalFloatingInsets.current.bottom + BOTTOM_CLEARANCE),
        ) {
        GlassPanel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                val editable = canRecord && !loading
                Box(Modifier.then(if (editable) Modifier.clickable(role = Role.Button) { onEdit(selected) } else Modifier)) {
                    DayDetail(
                        title = selected.format(DateTimeFormatter.ofPattern(stringResource(R.string.day_title_pattern), locale)),
                        detail = detail,
                        types = types.associateBy { it.id },
                        photoStore = photoStore,
                        loading = loading,
                    )
                }
            }
        }
        }
    }
}
