package com.actiary.ui.records

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.actiary.R
import com.actiary.data.RecordRepository
import com.actiary.data.db.DayStatus
import com.actiary.data.db.DayWithDetails
import com.actiary.data.db.ExerciseEntryEntity
import com.actiary.data.db.ExerciseTypeEntity
import com.actiary.data.photo.PhotoStore
import com.actiary.data.appearance.RecordsView
import com.actiary.domain.Bucket
import com.actiary.domain.ChartMetric
import com.actiary.domain.DateRange
import com.actiary.domain.RangeKind
import com.actiary.domain.RangeStats
import com.actiary.domain.computeStats
import com.actiary.domain.rangeOf
import com.actiary.domain.shiftRange
import com.actiary.glass.GlassIconButton
import com.actiary.glass.GlassPanel
import com.actiary.glass.GlassSegmentedPicker
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor
import com.actiary.ui.common.LocalFloatingInsets
import com.actiary.ui.common.exerciseTypeIcon
import com.actiary.ui.common.exerciseTypeLabel
import com.actiary.ui.record.formatDistance
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val BOTTOM_CLEARANCE = 24.dp
private val KINDS = RangeKind.entries

@get:StringRes
private val ChartMetric.label: Int get() = when (this) {
    ChartMetric.DURATION -> R.string.records_stat_duration
    ChartMetric.CALORIES -> R.string.records_stat_calories
    ChartMetric.COUNT -> R.string.records_stat_count
}

/** Null (no bar) when the bucket has no value for the metric or it is 0. */
private fun Bucket.valueOf(m: ChartMetric): Int? = when (m) {
    ChartMetric.DURATION -> minutes
    ChartMetric.CALORIES -> calories
    ChartMetric.COUNT -> count
}?.takeIf { it > 0 }

@Composable
fun RecordsScreen(
    repository: RecordRepository,
    photoStore: PhotoStore,
    initialView: RecordsView,
    onKind: (RangeKind) -> Unit,
    onMetric: (ChartMetric) -> Unit,
    onEdit: (LocalDate) -> Unit,
) {
    val t = LocalThemeTokens.current
    val locale = LocalConfiguration.current.locales[0]
    var today by remember { mutableStateOf(LocalDate.now()) }
    LifecycleResumeEffect(Unit) {
        today = LocalDate.now()
        onPauseOrDispose {}
    }
    val firstDay = remember { firstDayOfWeekFromLocale() }
    var kindIx by rememberSaveable { mutableIntStateOf(KINDS.indexOf(initialView.kind)) }
    var anchor by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var metricIx by rememberSaveable { mutableIntStateOf(initialView.metric.ordinal) }
    var filterId by rememberSaveable { mutableStateOf<Long?>(null) }
    val range = rangeOf(LocalDate.ofEpochDay(anchor), KINDS[kindIx], firstDay)
    val prev = shiftRange(range, -1, today, firstDay)?.takeIf { !it.from.isBefore(today.minusYears(10)) }
    val next = shiftRange(range, 1, today, firstDay)

    val loaded by remember(range) { repository.observeRange(range.from, range.to).map { range to it } }
        .collectAsStateWithLifecycle(null)
    val days = loaded?.takeIf { it.first == range }?.second
    val types by repository.allTypes.collectAsStateWithLifecycle(emptyList())
    val typeById = remember(types) { types.associateBy { it.id } }

    val primary = t.neutrals.textPrimary.asColor()
    val secondary = t.neutrals.textSecondary.asColor()
    val dayFmt = DateTimeFormatter.ofPattern(stringResource(R.string.records_day_pattern), locale)
    val rangeText = when (range.kind) {
        RangeKind.WEEK -> stringResource(R.string.records_week_range, range.from.format(dayFmt), range.to.format(dayFmt))
        RangeKind.MONTH -> range.from.format(DateTimeFormatter.ofPattern(stringResource(R.string.home_month_pattern), locale))
        RangeKind.YEAR -> range.from.format(DateTimeFormatter.ofPattern(stringResource(R.string.records_year_pattern), locale))
    }
    val monthFmt = DateTimeFormatter.ofPattern(stringResource(R.string.home_month_pattern), locale)
    val cardDateFmt = DateTimeFormatter.ofPattern(stringResource(R.string.day_title_pattern), locale)

    LazyColumn(
        Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = LocalFloatingInsets.current.bottom + BOTTOM_CLEARANCE),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            BasicText(
                stringResource(R.string.tab_records),
                Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = 48.dp).semantics { heading() },
                style = TextStyle(primary, 28.sp, FontWeight.Bold),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        item {
            GlassSegmentedPicker(
                listOf(R.string.records_week, R.string.records_month, R.string.records_year).map { stringResource(it) },
                kindIx, { kindIx = it; onKind(KINDS[it]) },
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    rangeText, Modifier.weight(1f).padding(start = 8.dp),
                    style = TextStyle(primary, 18.sp, FontWeight.SemiBold),
                )
                GlassIconButton(
                    Icons.AutoMirrored.Outlined.KeyboardArrowLeft, stringResource(R.string.records_prev),
                    { prev?.let { anchor = it.from.toEpochDay() } }, enabled = prev != null, glass = false, iconSize = 28.dp,
                )
                GlassIconButton(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight, stringResource(R.string.records_next),
                    { next?.let { anchor = it.from.toEpochDay() } }, enabled = next != null, glass = false, iconSize = 28.dp,
                )
            }
        }
        if (days != null && days.isEmpty()) {
            item {
                BasicText(
                    stringResource(R.string.records_none),
                    Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    style = TextStyle(secondary, 16.sp),
                )
            }
        } else if (days != null) {
            val present = computeStats(days, range, null).byType.map { it.first }.toSet()
            val filter = filterId?.takeIf { it in present }
            val stats = computeStats(days, range, filter)
            item { SummaryCard(stats, range.kind, ChartMetric.entries[metricIx], { metricIx = it.ordinal; onMetric(it) }, typeById) }
            item {
                FilterBar(types.filter { it.id in present }, filter) { filterId = it }
            }
            val timeline = timelineItems(days, range, filter)
            items(timeline, key = { if (it is TimelineItem.Day) it.day.record.epochDay else "m${(it as TimelineItem.Month).month}" }) { item ->
                when (item) {
                    is TimelineItem.Month -> BasicText(
                        item.month.format(monthFmt),
                        Modifier.fillMaxWidth().padding(top = 8.dp).semantics { heading() },
                        style = TextStyle(secondary, 15.sp, FontWeight.SemiBold),
                    )
                    is TimelineItem.Day -> DayCard(item.day, typeById, photoStore, cardDateFmt) {
                        onEdit(LocalDate.ofEpochDay(item.day.record.epochDay))
                    }
                }
            }
        }
    }
}

@Composable
private fun durationText(minutes: Int): String {
    val (h, m) = splitMinutes(minutes)
    return when {
        h == 0 -> stringResource(R.string.records_min, m)
        m == 0 -> stringResource(R.string.records_h, h)
        else -> stringResource(R.string.records_hm, h, m)
    }
}

@Composable
private fun SummaryCard(stats: RangeStats, kind: RangeKind, metric: ChartMetric, onMetric: (ChartMetric) -> Unit, typeById: Map<Long, ExerciseTypeEntity>) {
    val t = LocalThemeTokens.current
    val secondary = t.neutrals.textSecondary.asColor()
    GlassPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Stat(Modifier.weight(1f), stringResource(R.string.records_stat_days), stats.exerciseDays.toString())
                Stat(Modifier.weight(1f), stringResource(R.string.records_stat_count), stats.entryCount.toString())
            }
            Row(Modifier.fillMaxWidth()) {
                Stat(Modifier.weight(1f), stringResource(R.string.records_stat_duration), stats.totalMinutes?.let { durationText(it) })
                Stat(Modifier.weight(1f), stringResource(R.string.records_stat_calories), stats.totalCalories?.let { stringResource(R.string.records_kcal, it) })
            }
            val top = stats.byType.firstOrNull()?.second ?: 0
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                stats.byType.forEach { (id, count) ->
                    val type = typeById[id] ?: return@forEach
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(exerciseTypeIcon(type), null, Modifier.size(18.dp), tint = secondary)
                        BasicText(
                            exerciseTypeLabel(type), Modifier.width(88.dp),
                            style = TextStyle(t.neutrals.textPrimary.asColor(), 14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(t.neutrals.surfaceMuted.asColor())) {
                            Box(
                                Modifier.fillMaxHeight().fillMaxWidth(count.toFloat() / top)
                                    .clip(RoundedCornerShape(5.dp)).background(t.accent.accentChart.asColor()),
                            )
                        }
                        BasicText(count.toString(), Modifier.width(28.dp), style = TextStyle(t.neutrals.textPrimary.asColor(), 14.sp, FontWeight.Medium))
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChartMetric.entries.forEach { Chip(stringResource(it.label), null, it == metric) { onMetric(it) } }
                }
                BarChart(stats, kind, metric)
            }
        }
    }
}

@Composable
private fun Stat(modifier: Modifier, label: String, value: String?) {
    val t = LocalThemeTokens.current
    Column(modifier) {
        if (value == null) {
            BasicText(stringResource(R.string.records_none), style = TextStyle(t.neutrals.textSecondary.asColor(), 16.sp))
        } else {
            BasicText(value, style = TextStyle(t.neutrals.textPrimary.asColor(), 22.sp, FontWeight.Bold))
        }
        BasicText(label, style = TextStyle(t.neutrals.textSecondary.asColor(), 13.sp))
    }
}

@Composable
private fun metricText(metric: ChartMetric, v: Int): String = when (metric) {
    ChartMetric.DURATION -> durationText(v)
    ChartMetric.CALORIES -> stringResource(R.string.records_kcal, v)
    ChartMetric.COUNT -> v.toString()
}

@Composable
private fun BarChart(stats: RangeStats, kind: RangeKind, metric: ChartMetric) {
    val t = LocalThemeTokens.current
    val buckets = stats.buckets
    val values = buckets.map { it.valueOf(metric) }
    val max = values.filterNotNull().maxOrNull()
    val secondary = t.neutrals.textSecondary.asColor()
    if (max == null) {
        BasicText(stringResource(R.string.records_none), Modifier.fillMaxWidth().padding(vertical = 16.dp), style = TextStyle(secondary, 16.sp))
        return
    }
    val maxText = metricText(metric, max)
    val desc = stringResource(R.string.records_chart_desc, stringResource(metric.label), maxText)
    val bar = t.accent.accentChart.asColor()
    val slot = t.neutrals.divider.asColor()
    fun label(i: Int) = buckets[i].start.let { if (kind == RangeKind.YEAR) it.monthValue else it.dayOfMonth }.toString()
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(t.neutrals.surface.asColor())
            .padding(12.dp).semantics(mergeDescendants = true) { contentDescription = desc },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BasicText(maxText, style = TextStyle(secondary, 12.sp))
        Canvas(Modifier.fillMaxWidth().height(96.dp)) {
            val slotW = size.width / buckets.size
            val barW = slotW * 0.6f
            val minH = 2.dp.toPx()
            values.forEachIndexed { i, v ->
                val x = i * slotW + (slotW - barW) / 2
                val h = if (v == null) minH else (size.height * v / max).coerceAtLeast(4.dp.toPx())
                drawRoundRect(
                    if (v == null) slot else bar,
                    Offset(x, size.height - h), Size(barW, h), CornerRadius(barW.coerceAtMost(4.dp.toPx())),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            BasicText(label(0), style = TextStyle(secondary, 12.sp))
            BasicText(label(buckets.size - 1), style = TextStyle(secondary, 12.sp))
        }
    }
}

@Composable
private fun FilterBar(present: List<ExerciseTypeEntity>, selected: Long?, onSelect: (Long?) -> Unit) {
    GlassPanel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Chip(stringResource(R.string.records_all), null, selected == null) { onSelect(null) }
            present.forEach { type ->
                Chip(exerciseTypeLabel(type), exerciseTypeIcon(type), selected == type.id) { onSelect(type.id) }
            }
        }
    }
}

@Composable
private fun Chip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector?, on: Boolean, onClick: () -> Unit) {
    val t = LocalThemeTokens.current
    val shape = RoundedCornerShape(50)
    val color = (if (on) t.accent.accentAction else t.neutrals.textPrimary).asColor()
    Row(
        Modifier.heightIn(min = 48.dp).clip(shape)
            .then(if (on) Modifier.background(t.accent.accentSubtle.asColor()).border(2.dp, t.accent.accentStroke.asColor(), shape) else Modifier)
            .semantics { selected = on }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (on) Icon(Icons.Outlined.Check, null, Modifier.size(18.dp), tint = color)
        else if (icon != null) Icon(icon, null, Modifier.size(18.dp), tint = color)
        BasicText(label, style = TextStyle(color, 14.sp, FontWeight.Medium), maxLines = 1)
    }
}

@Composable
private fun entryMetrics(e: ExerciseEntryEntity, type: ExerciseTypeEntity): String = buildList {
    e.durationMinutes?.let { add(stringResource(R.string.records_min, it)) }
    e.calories?.let { add(stringResource(R.string.records_kcal, it)) }
    val d = formatDistance(e.distanceMeters, type.builtinKey, type.hasDistance)
    if (d.isNotEmpty()) add(stringResource(if (type.builtinKey == "swimming") R.string.records_m else R.string.records_km, d))
}.joinToString(" · ")

@Composable
private fun DayCard(
    day: DayWithDetails,
    typeById: Map<Long, ExerciseTypeEntity>,
    photoStore: PhotoStore,
    dateFmt: DateTimeFormatter,
    onClick: () -> Unit,
) {
    val t = LocalThemeTokens.current
    val primary = t.neutrals.textPrimary.asColor()
    val secondary = t.neutrals.textSecondary.asColor()
    val photo = day.photos.firstOrNull()
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(t.neutrals.surface.asColor())
            .clickable(role = Role.Button, onClick = onClick).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BasicText(LocalDate.ofEpochDay(day.record.epochDay).format(dateFmt), style = TextStyle(primary, 15.sp, FontWeight.SemiBold))
            if (day.record.status == DayStatus.REST) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.Bedtime, null, Modifier.size(18.dp), tint = secondary)
                    BasicText(stringResource(R.string.day_rest), style = TextStyle(secondary, 14.sp))
                }
            } else if (day.entries.isEmpty()) {
                BasicText(stringResource(R.string.day_checked_in), style = TextStyle(secondary, 14.sp))
            } else {
                day.entries.forEach { e ->
                    val type = typeById[e.typeId] ?: return@forEach
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(exerciseTypeIcon(type), null, Modifier.size(18.dp), tint = primary)
                        BasicText(exerciseTypeLabel(type), style = TextStyle(primary, 14.sp))
                        val metrics = entryMetrics(e, type)
                        if (metrics.isNotEmpty()) {
                            BasicText(
                                metrics, Modifier.weight(1f), style = TextStyle(secondary, 13.sp),
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            if (day.record.note.isNotBlank()) {
                BasicText(day.record.note, style = TextStyle(secondary, 13.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (photo != null) {
            AsyncImage(
                photoStore.file(photo.fileName), stringResource(R.string.day_photo, 1),
                Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(t.neutrals.surfaceMuted.asColor()),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
