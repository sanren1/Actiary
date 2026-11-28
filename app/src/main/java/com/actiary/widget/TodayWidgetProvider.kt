package com.actiary.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.Bundle
import android.util.SizeF
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.actiary.ActiaryApp
import com.actiary.MainActivity
import com.actiary.R
import com.actiary.data.appearance.AppLanguagePrefs
import com.actiary.data.db.DayStatus
import com.actiary.domain.DayDraft
import com.actiary.domain.DayState
import com.actiary.domain.computeCurrentStreak
import com.actiary.domain.recommendNextPlanId
import com.actiary.domain.toDayState
import com.actiary.theme.ThemeTokens
import com.actiary.theme.resolveTheme
import com.actiary.ui.calendar.buildStrengthSummaryText
import com.actiary.ui.common.exerciseTypeLabel
import com.actiary.ui.common.labelRes
import com.actiary.ui.common.workoutPlanLabel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TodayWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return
        val pending = goAsync()
        TodayWidgetUpdater.scope.launch {
            try {
                TodayWidgetUpdater.updateWidgets(context.applicationContext, appWidgetManager, appWidgetIds)
            } finally {
                pending.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        val pending = goAsync()
        TodayWidgetUpdater.scope.launch {
            try {
                TodayWidgetUpdater.updateWidgets(context.applicationContext, appWidgetManager, intArrayOf(appWidgetId))
            } finally {
                pending.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_TOGGLE_REST_TODAY) return
        val pending = goAsync()
        val appContext = context.applicationContext
        TodayWidgetUpdater.scope.launch {
            try {
                val container = (appContext as ActiaryApp).container
                val today = LocalDate.now()
                val detail = container.recordRepository.observeDay(today).first()
                when (decideWidgetRestAction(detail)) {
                    WidgetRestAction.MARK_REST -> {
                        container.recordRepository.save(
                            DayDraft(today, DayStatus.REST, emptyList(), "", emptyList()),
                        )
                    }
                    WidgetRestAction.CLEAR_REST -> {
                        container.recordRepository.clearDay(today)
                    }
                    WidgetRestAction.OPEN_EDITOR -> {
                        appContext.startActivity(openRecordTodayIntent(appContext))
                    }
                }
                TodayWidgetUpdater.refreshNow(appContext)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE_REST_TODAY = "com.actiary.widget.ACTION_TOGGLE_REST_TODAY"
    }
}

internal object TodayWidgetUpdater {
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val dotIds = intArrayOf(
        R.id.widget_dot_0,
        R.id.widget_dot_1,
        R.id.widget_dot_2,
        R.id.widget_dot_3,
        R.id.widget_dot_4,
        R.id.widget_dot_5,
        R.id.widget_dot_6,
    )

    private val dayLabelIds = intArrayOf(
        R.id.widget_day_label_0,
        R.id.widget_day_label_1,
        R.id.widget_day_label_2,
        R.id.widget_day_label_3,
        R.id.widget_day_label_4,
        R.id.widget_day_label_5,
        R.id.widget_day_label_6,
    )

    fun refresh(context: Context) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        val ids = manager.getAppWidgetIds(ComponentName(appContext, TodayWidgetProvider::class.java))
        if (ids.isEmpty()) return
        scope.launch {
            updateWidgets(appContext, manager, ids)
        }
    }

    internal suspend fun refreshNow(context: Context) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        val ids = manager.getAppWidgetIds(ComponentName(appContext, TodayWidgetProvider::class.java))
        if (ids.isEmpty()) return
        updateWidgets(appContext, manager, ids)
    }

    internal suspend fun updateWidgets(
        appContext: Context,
        manager: AppWidgetManager,
        widgetIds: IntArray,
    ) {
        val localized = AppLanguagePrefs.wrapContext(appContext)
        val container = (appContext as ActiaryApp).container
        val today = LocalDate.now()
        val history = container.recordRepository.observeRange(today.minusDays(365), today).first()
        val exercisedEpochDays = history
            .filter { it.record.status == DayStatus.EXERCISED }
            .mapTo(HashSet()) { it.record.epochDay }
        val restEpochDays = history
            .filter { it.record.status == DayStatus.REST }
            .mapTo(HashSet()) { it.record.epochDay }
        val streakDays = computeCurrentStreak(exercisedEpochDays, today, restEpochDays)
        val recentCutoff = today.minusDays(6).toEpochDay()
        val recent = history.filter { it.record.epochDay >= recentCutoff }
        val todayDetail = recent.firstOrNull { it.record.epochDay == today.toEpochDay() }
        val todayState = todayDetail?.record?.toDayState() ?: DayState.UNRECORDED
        val types = container.recordRepository.allTypes.first().associateBy { it.id }
        val allExercises = container.strengthRepository.allExercises.first().associateBy { it.id }
        val todayLogs = todayDetail?.entriesWithLogs
            .orEmpty()
            .sortedBy { it.entry.sortOrder }
            .flatMap { it.strengthLogs }
        val strengthSummary = if (todayState == DayState.EXERCISED && todayLogs.isNotEmpty()) {
            buildStrengthSummaryText(
                logs = todayLogs,
                exercises = allExercises,
                muscleLabel = { localized.getString(it.labelRes()) },
                countsLabel = { exCount, setCount ->
                    localized.getString(R.string.strength_summary_counts, exCount, setCount)
                },
            )
        } else {
            null
        }
        val recommendedPlanLabel = if (todayState == DayState.UNRECORDED) {
            val plans = container.strengthRepository.plans.first().sortedBy { it.plan.sortOrder }
            val lastPlanId = container.strengthRepository.latestFreeformPlanId(today)
            val recommendedId = recommendNextPlanId(plans.map { it.plan.id }, lastPlanId)
            plans.firstOrNull { it.plan.id == recommendedId }
                ?.plan
                ?.let { workoutPlanLabel(it, localized.resources) }
                ?.takeIf { it.isNotBlank() }
        } else {
            null
        }
        val settings = container.appearanceRepository.settings.first()
        val systemDark = (localized.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val dynamicSeed = ContextCompat.getColor(appContext, android.R.color.system_accent1_500)
        val tokens = resolveTheme(settings, systemDark, dynamicSeed)
        val visualStyle = resolveWidgetVisualStyle(settings, tokens)

        val snapshot = buildWidgetSnapshot(
            today = today,
            recentDays = recent,
            typesById = types,
            streakDays = streakDays,
            strengthSummary = strengthSummary,
            recommendedPlanLabel = recommendedPlanLabel,
        ) { type ->
            exerciseTypeLabel(type, localized.resources)
        }
        val strip = buildStripViews(appContext, localized, snapshot, tokens, visualStyle)
        val compact = buildRemoteViews(
            appContext,
            localized,
            R.layout.widget_today_compact,
            snapshot,
            tokens,
            visualStyle,
            compact = true,
        )
        val medium = buildRemoteViews(
            appContext,
            localized,
            R.layout.widget_today_medium,
            snapshot,
            tokens,
            visualStyle,
            compact = false,
        )
        val responsive = RemoteViews(
            mapOf(
                SizeF(110f, 40f) to strip,
                SizeF(110f, 110f) to compact,
                SizeF(240f, 110f) to medium,
            ),
        )
        for (id in widgetIds) {
            manager.updateAppWidget(id, responsive)
        }
    }

    private fun buildStripViews(
        appContext: Context,
        localized: Context,
        snapshot: WidgetSnapshot,
        tokens: ThemeTokens,
        visualStyle: WidgetVisualStyle,
    ): RemoteViews {
        val views = RemoteViews(appContext.packageName, R.layout.widget_today_strip)
        val n = tokens.neutrals
        val a = tokens.accent
        val isP5 = visualStyle.isPersona5
        val locale = localized.resources.configuration.locales[0]
        val dateText = snapshot.today.format(
            DateTimeFormatter.ofPattern(localized.getString(R.string.widget_today_date_short_pattern), locale),
        )
        views.setImageViewBitmap(
            R.id.widget_bg_image,
            if (isP5) createP5WidgetBackdrop(260, 110, visualStyle) else createFrostedBackdrop(260, 110, tokens),
        )

        views.setTextViewText(
            R.id.widget_strip_info,
            localized.getString(R.string.widget_strip_meta, dateText, snapshot.recentExercisedDays),
        )
        views.setTextColor(R.id.widget_strip_info, if (isP5) visualStyle.textPrimary else n.textPrimary)

        if (isP5) {
            views.setImageViewBitmap(
                R.id.widget_dot_today,
                createP5WeekDotBitmap(snapshot.todayState, isToday = true, style = visualStyle),
            )
            views.setInt(R.id.widget_dot_today, "setColorFilter", 0)
        } else {
            when (snapshot.todayState) {
                DayState.EXERCISED -> {
                    views.setImageViewResource(R.id.widget_dot_today, R.drawable.widget_dot_solid)
                    views.setInt(R.id.widget_dot_today, "setColorFilter", a.accentAction)
                }
                DayState.REST -> {
                    views.setImageViewResource(R.id.widget_dot_today, R.drawable.widget_dot_solid)
                    views.setInt(R.id.widget_dot_today, "setColorFilter", withAlpha(n.textSecondary, 0xCC))
                }
                DayState.UNRECORDED -> {
                    views.setImageViewResource(R.id.widget_dot_today, R.drawable.widget_dot_ring_today)
                    views.setInt(R.id.widget_dot_today, "setColorFilter", a.accentAction)
                }
            }
        }

        views.setTextViewText(R.id.widget_btn_record, localized.getString(R.string.widget_action_record_short))
        views.setColorStateList(
            R.id.widget_btn_record,
            "setBackgroundTintList",
            ColorStateList.valueOf(if (isP5) visualStyle.accentFill else a.accentAction),
        )
        views.setTextColor(R.id.widget_btn_record, if (isP5) visualStyle.onAccentFill else a.onAccentAction)

        val isRest = snapshot.todayState == DayState.REST
        views.setTextViewText(
            R.id.widget_btn_rest,
            localized.getString(if (isRest) R.string.widget_action_rest_active else R.string.widget_action_rest_short),
        )
        if (isP5) {
            val restBg = when {
                isRest -> P5_INK_BLACK
                visualStyle.isLightSurface -> P5_PAPER_MUTED
                else -> P5_SURFACE_MUTED
            }
            val restFg = when {
                isRest && visualStyle.isLightSurface -> P5_PAPER_WHITE
                isRest -> a.accentAction
                else -> visualStyle.textPrimary
            }
            views.setColorStateList(
                R.id.widget_btn_rest,
                "setBackgroundTintList",
                ColorStateList.valueOf(restBg),
            )
            views.setTextColor(R.id.widget_btn_rest, restFg)
        } else {
            views.setColorStateList(
                R.id.widget_btn_rest,
                "setBackgroundTintList",
                ColorStateList.valueOf(if (isRest) a.accentSubtle else n.surfaceMuted),
            )
            views.setTextColor(R.id.widget_btn_rest, if (isRest) a.accentAction else n.textPrimary)
        }

        bindWidgetClicks(appContext, views)
        return views
    }

    private fun buildRemoteViews(
        appContext: Context,
        localized: Context,
        layoutId: Int,
        snapshot: WidgetSnapshot,
        tokens: ThemeTokens,
        visualStyle: WidgetVisualStyle,
        compact: Boolean,
    ): RemoteViews {
        val views = RemoteViews(appContext.packageName, layoutId)
        val n = tokens.neutrals
        val a = tokens.accent
        val isP5 = visualStyle.isPersona5
        val locale = localized.resources.configuration.locales[0]
        val datePattern = localized.getString(
            if (compact) R.string.widget_today_date_short_pattern else R.string.widget_today_date_pattern,
        )
        val dateText = snapshot.today.format(DateTimeFormatter.ofPattern(datePattern, locale))

        val bgWidth = if (compact) 220 else 360
        views.setImageViewBitmap(
            R.id.widget_bg_image,
            if (isP5) createP5WidgetBackdrop(bgWidth, 200, visualStyle) else createFrostedBackdrop(bgWidth, 200, tokens),
        )

        views.setTextViewText(R.id.widget_date, dateText)
        views.setTextColor(R.id.widget_date, if (isP5) visualStyle.textPrimary else n.textPrimary)

        val weekText = localized.getString(R.string.widget_week_streak, snapshot.recentExercisedDays)
        val streakText = if (snapshot.streakDays > 0) {
            "$weekText · ${localized.getString(R.string.widget_streak_short, snapshot.streakDays)}"
        } else {
            weekText
        }
        views.setTextViewText(R.id.widget_streak, streakText)
        if (compact) {
            views.setTextViewTextSize(
                R.id.widget_streak,
                TypedValue.COMPLEX_UNIT_SP,
                if (snapshot.streakDays > 0) 13f else 15f,
            )
        }
        views.setTextColor(
            R.id.widget_streak,
            if (isP5) {
                if (compact) a.accentAction else visualStyle.textSecondary
            } else {
                if (compact) n.textPrimary else n.textSecondary
            },
        )

        val (badgeText, badgeBg, badgeFg) = if (isP5) {
            when (snapshot.todayState) {
                DayState.EXERCISED -> Triple(
                    localized.getString(R.string.day_checked_in),
                    visualStyle.accentFill,
                    visualStyle.onAccentFill,
                )
                DayState.REST -> Triple(
                    localized.getString(R.string.day_rest),
                    if (visualStyle.isLightSurface) P5_PAPER_MUTED else P5_SURFACE_MUTED,
                    visualStyle.textPrimary,
                )
                DayState.UNRECORDED -> Triple(
                    localized.getString(R.string.day_unrecorded),
                    if (visualStyle.isLightSurface) P5_PAPER_MUTED else P5_INK_BLACK,
                    visualStyle.textSecondary,
                )
            }
        } else {
            when (snapshot.todayState) {
                DayState.EXERCISED -> Triple(
                    localized.getString(R.string.day_checked_in),
                    a.accentSubtle,
                    a.accentAction,
                )
                DayState.REST -> Triple(
                    localized.getString(R.string.day_rest),
                    n.surfaceMuted,
                    n.textPrimary,
                )
                DayState.UNRECORDED -> Triple(
                    localized.getString(R.string.day_unrecorded),
                    n.surfaceMuted,
                    n.textSecondary,
                )
            }
        }
        views.setTextViewText(R.id.widget_status_badge, badgeText)
        views.setColorStateList(R.id.widget_status_badge, "setBackgroundTintList", ColorStateList.valueOf(badgeBg))
        views.setTextColor(R.id.widget_status_badge, badgeFg)

        val summary = resolveWidgetSummary(
            snapshot = snapshot,
            checkedInFallback = localized.getString(R.string.day_checked_in),
            restSummary = localized.getString(R.string.widget_today_rest_summary),
            emptySummary = localized.getString(R.string.widget_today_empty_summary),
            recommendedPlanText = { plan -> localized.getString(R.string.widget_recommended_plan, plan) },
        )
        views.setTextViewText(R.id.widget_summary, summary)
        views.setTextColor(
            R.id.widget_summary,
            if (isP5) {
                when {
                    snapshot.todayState == DayState.EXERCISED -> visualStyle.textPrimary
                    snapshot.todayState == DayState.UNRECORDED && snapshot.recommendedPlanLabel != null -> a.accentAction
                    else -> visualStyle.textSecondary
                }
            } else {
                when {
                    snapshot.todayState == DayState.EXERCISED -> n.textPrimary
                    snapshot.todayState == DayState.UNRECORDED && snapshot.recommendedPlanLabel != null -> a.accentAction
                    else -> n.textSecondary
                }
            },
        )

        val dayFmt = DateTimeFormatter.ofPattern("EEEEE", locale)
        val mutedRingColor = withAlpha(n.textSecondary, if (tokens.dark) 0x66 else 0x55)
        snapshot.weekDots.forEachIndexed { index, dot ->
            val id = dotIds[index]
            if (isP5) {
                views.setImageViewBitmap(id, createP5WeekDotBitmap(dot.state, dot.isToday, visualStyle))
                views.setInt(id, "setColorFilter", 0)
                if (dot.isToday) {
                    views.setViewVisibility(R.id.widget_dot_6_ring, View.GONE)
                }
            } else {
                when (dot.state) {
                    DayState.EXERCISED -> {
                        views.setImageViewResource(id, R.drawable.widget_dot_solid)
                        views.setInt(id, "setColorFilter", a.accentAction)
                    }
                    DayState.REST -> {
                        views.setImageViewResource(id, R.drawable.widget_dot_solid)
                        views.setInt(id, "setColorFilter", withAlpha(n.textSecondary, 0x99))
                    }
                    DayState.UNRECORDED -> {
                        views.setImageViewResource(
                            id,
                            if (dot.isToday) R.drawable.widget_dot_ring_today else R.drawable.widget_dot_ring,
                        )
                        views.setInt(id, "setColorFilter", if (dot.isToday) a.accentAction else mutedRingColor)
                    }
                }
                if (dot.isToday) {
                    when (dot.state) {
                        DayState.EXERCISED -> {
                            views.setViewVisibility(R.id.widget_dot_6_ring, View.VISIBLE)
                            views.setInt(
                                R.id.widget_dot_6_ring,
                                "setColorFilter",
                                withAlpha(a.accentAction, if (tokens.dark) 0x85 else 0x66),
                            )
                        }
                        DayState.REST -> {
                            views.setViewVisibility(R.id.widget_dot_6_ring, View.VISIBLE)
                            views.setInt(
                                R.id.widget_dot_6_ring,
                                "setColorFilter",
                                withAlpha(n.textSecondary, 0x66),
                            )
                        }
                        DayState.UNRECORDED -> {
                            views.setViewVisibility(R.id.widget_dot_6_ring, View.GONE)
                        }
                    }
                }
            }
            if (!compact) {
                val labelId = dayLabelIds[index]
                views.setTextViewText(labelId, dot.date.format(dayFmt))
                views.setTextColor(
                    labelId,
                    if (isP5) {
                        if (dot.isToday) a.accentAction else visualStyle.textSecondary
                    } else {
                        if (dot.isToday) n.textPrimary else n.textSecondary
                    },
                )
            }
        }

        val recordLabel = localized.getString(
            if (compact) R.string.widget_action_record_short else R.string.widget_action_record,
        )
        views.setTextViewText(R.id.widget_btn_record, recordLabel)
        views.setColorStateList(
            R.id.widget_btn_record,
            "setBackgroundTintList",
            ColorStateList.valueOf(if (isP5) visualStyle.accentFill else a.accentAction),
        )
        views.setTextColor(R.id.widget_btn_record, if (isP5) visualStyle.onAccentFill else a.onAccentAction)

        val isRest = snapshot.todayState == DayState.REST
        val restLabel = localized.getString(
            when {
                isRest -> R.string.widget_action_rest_active
                compact -> R.string.widget_action_rest_short
                else -> R.string.widget_action_rest
            },
        )
        views.setTextViewText(R.id.widget_btn_rest, restLabel)
        if (isP5) {
            val restBg = when {
                isRest -> P5_INK_BLACK
                visualStyle.isLightSurface -> P5_PAPER_MUTED
                else -> P5_SURFACE_MUTED
            }
            val restFg = when {
                isRest && visualStyle.isLightSurface -> P5_PAPER_WHITE
                isRest -> a.accentAction
                else -> visualStyle.textPrimary
            }
            views.setColorStateList(
                R.id.widget_btn_rest,
                "setBackgroundTintList",
                ColorStateList.valueOf(restBg),
            )
            views.setTextColor(R.id.widget_btn_rest, restFg)
        } else {
            views.setColorStateList(
                R.id.widget_btn_rest,
                "setBackgroundTintList",
                ColorStateList.valueOf(if (isRest) a.accentSubtle else n.surfaceMuted),
            )
            views.setTextColor(R.id.widget_btn_rest, if (isRest) a.accentAction else n.textPrimary)
        }

        bindWidgetClicks(appContext, views)
        return views
    }

    private fun bindWidgetClicks(appContext: Context, views: RemoteViews) {
        val openAppPending = PendingIntent.getActivity(
            appContext,
            REQ_OPEN_APP,
            Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val openRecordPending = PendingIntent.getActivity(
            appContext,
            REQ_OPEN_RECORD,
            openRecordTodayIntent(appContext),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val toggleRestPending = PendingIntent.getBroadcast(
            appContext,
            REQ_TOGGLE_REST,
            Intent(appContext, TodayWidgetProvider::class.java).apply {
                action = TodayWidgetProvider.ACTION_TOGGLE_REST_TODAY
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        views.setOnClickPendingIntent(R.id.widget_root, openAppPending)
        views.setOnClickPendingIntent(R.id.widget_btn_record, openRecordPending)
        views.setOnClickPendingIntent(R.id.widget_btn_rest, toggleRestPending)
    }

    private fun createFrostedBackdrop(width: Int, height: Int, tokens: ThemeTokens): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val w = width.toFloat()
        val h = height.toFloat()
        val rect = RectF(0f, 0f, w, h)
        val radius = 24f * (width / 220f).coerceIn(1f, 1.4f)
        val isAmoled = tokens.dark && tokens.neutrals.background == 0xFF000000.toInt()

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val topLeft = when {
            isAmoled -> 0xD215181D.toInt()
            tokens.dark -> 0xC4252930.toInt()
            else -> 0xD6FAFBFB.toInt()
        }
        val bottomRight = when {
            isAmoled -> 0xE4090B0E.toInt()
            tokens.dark -> 0xD615181C.toInt()
            else -> 0xC6ECEEF1.toInt()
        }
        paint.shader = LinearGradient(0f, 0f, w, h, topLeft, bottomRight, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(rect, radius, radius, paint)

        // Top-edge glass sheen + gentle top-left specular glow for depth.
        val topEdgeSheen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, h * 0.36f,
                when {
                    isAmoled -> 0x14FFFFFF
                    tokens.dark -> 0x18FFFFFF
                    else -> 0x36FFFFFF
                },
                0x00000000,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(rect, radius, radius, topEdgeSheen)

        val sheen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.18f, h * 0.12f, maxOf(w, h) * 0.78f,
                when {
                    isAmoled -> 0x12FFFFFF
                    tokens.dark -> 0x1AFFFFFF
                    else -> 0x30FFFFFF
                },
                0x00000000,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(rect, radius, radius, sheen)

        // Subtle deterministic matte frosted grain.
        val grainLight = Paint().apply { color = if (tokens.dark) 0x0EFFFFFF else 0x16FFFFFF }
        val grainDark = Paint().apply { color = if (tokens.dark) 0x10000000 else 0x09000000 }
        var seed = 0x51A7C3E1
        val step = 2
        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                seed = seed * 1664525 + 1013904223
                val v = (seed ushr 24) and 0xFF
                if (v > 180) canvas.drawPoint(x.toFloat(), y.toFloat(), grainLight)
                else if (v < 70) canvas.drawPoint(x.toFloat(), y.toFloat(), grainDark)
                x += step
            }
            y += step
        }

        // Crisp outer hairline in light mode so card edges stay distinct on bright wallpapers.
        if (!tokens.dark) {
            val outerHairline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1f
                color = 0x1C000000
            }
            val outerInset = 0.5f
            canvas.drawRoundRect(
                RectF(outerInset, outerInset, w - outerInset, h - outerInset),
                radius,
                radius,
                outerHairline,
            )
        }

        // Specular glass rim.
        val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.4f
            shader = LinearGradient(
                0f, 0f, w, h,
                when {
                    isAmoled -> intArrayOf(0x38FFFFFF, 0x12FFFFFF, 0x20FFFFFF)
                    tokens.dark -> intArrayOf(0x46FFFFFF, 0x16FFFFFF, 0x2AFFFFFF)
                    else -> intArrayOf(0xA8FFFFFF.toInt(), 0x32FFFFFF, 0x6EFFFFFF)
                },
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        val inset = if (tokens.dark) 0.9f else 1.4f
        canvas.drawRoundRect(RectF(inset, inset, w - inset, h - inset), radius, radius, rimPaint)
        return bitmap
    }

    private fun createP5WidgetBackdrop(width: Int, height: Int, style: WidgetVisualStyle): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val w = width.toFloat()
        val h = height.toFloat()
        val shortSide = minOf(w, h)
        val isStrip = h < 140f

        // 0. Full-bleed outer base matching the theme polarity so launcher rounded corners never show seams.
        canvas.drawColor(if (style.isLightSurface) style.backgroundColor else P5_INK_BLACK)

        // Safe inner geometry: all 4 corners are chamfered inside Android 12+ / OriginOS 16-24dp clip radii.
        val insetX = (shortSide * 0.045f).coerceIn(4.5f, 8f)
        val insetY = (shortSide * 0.045f).coerceIn(4.5f, 8f)
        val shadowDx = (shortSide * 0.032f).coerceIn(3.5f, 6f)
        val shadowDy = (shortSide * 0.032f).coerceIn(3.5f, 6f)
        val chamferPrimary = (shortSide * 0.165f).coerceIn(17f, 28f)
        val chamferSecondary = (shortSide * 0.135f).coerceIn(14f, 23f)

        val left = insetX
        val top = insetY
        val right = w - insetX - shadowDx
        val bottom = h - insetY - shadowDy

        fun buildSafeChamferedCardPath(dx: Float, dy: Float): Path = Path().apply {
            moveTo(left + chamferPrimary + dx, top + dy)
            lineTo(right - chamferSecondary + dx, top + dy)
            lineTo(right + dx, top + chamferSecondary + dy)
            lineTo(right + dx, bottom - chamferPrimary + dy)
            lineTo(right - chamferPrimary + dx, bottom + dy)
            lineTo(left + chamferSecondary + dx, bottom + dy)
            lineTo(left + dx, bottom - chamferSecondary + dy)
            lineTo(left + dx, top + chamferPrimary + dy)
            close()
        }

        // 1. Bottom-right hard-offset comic shadow polygon (strictly inside rounded-corner safe area).
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.FILL
            color = if (style.isLightSurface) P5_INK_BLACK else style.accentFill
        }
        canvas.drawPath(buildSafeChamferedCardPath(shadowDx, shadowDy), shadowPaint)

        // 2. Main safe-zone chamfered card polygon.
        val cardPath = buildSafeChamferedCardPath(0f, 0f)
        val cardFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.FILL
            color = style.backgroundColor
        }
        canvas.drawPath(cardPath, cardFillPaint)

        // 3. Internal P5 diagonal graphic accents clipped to the card polygon, keeping text zones clean.
        canvas.save()
        canvas.clipPath(cardPath)

        // Subtle low-alpha diagonal comic band across the lower-right interior (preserves > 95% text contrast).
        val bandAlpha = when {
            style.isLightSurface && isStrip -> 0x0C
            style.isLightSurface -> 0x10
            isStrip -> 0x12
            else -> 0x18
        }
        val washPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.FILL
            color = withAlpha(style.accentFill, bandAlpha)
        }
        val diagonalWash = Path().apply {
            moveTo(w * 0.62f, h)
            lineTo(w * 0.86f, 0f)
            lineTo(w, 0f)
            lineTo(w * 0.80f, h)
            close()
        }
        canvas.drawPath(diagonalWash, washPaint)

        // Crisp accent corner slashes tucked exclusively into the chamfer margins (outside text/button padding).
        val accentSlashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.FILL
            color = style.accentFill
        }
        val slashThickness = if (isStrip) 5.5f else 8.5f
        val topLeftSlash = Path().apply {
            moveTo(left + chamferPrimary, top)
            lineTo(left + chamferPrimary + slashThickness, top)
            lineTo(left, top + chamferPrimary + slashThickness)
            lineTo(left, top + chamferPrimary)
            close()
        }
        canvas.drawPath(topLeftSlash, accentSlashPaint)

        val bottomRightSlash = Path().apply {
            moveTo(right, bottom - chamferPrimary - slashThickness)
            lineTo(right, bottom - chamferPrimary)
            lineTo(right - chamferPrimary, bottom)
            lineTo(right - chamferPrimary - slashThickness, bottom)
            close()
        }
        canvas.drawPath(bottomRightSlash, accentSlashPaint)

        // Fine high-contrast speed-line parallel to the top-left chamfer.
        val highlightLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
            strokeWidth = 1.8f
            color = style.highlightColor
        }
        val lineOffset = slashThickness + 3f
        canvas.drawLine(
            left + chamferPrimary + lineOffset,
            top,
            left,
            top + chamferPrimary + lineOffset,
            highlightLinePaint,
        )
        canvas.restore()

        // 4. Crisp comic border around the safe-zone polygon.
        if (style.isLightSurface) {
            val inkFrame = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.style = Paint.Style.STROKE
                strokeWidth = 3.2f
                strokeJoin = Paint.Join.MITER
                color = style.strokeColor
            }
            canvas.drawPath(cardPath, inkFrame)
        } else {
            val outerInkStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.style = Paint.Style.STROKE
                strokeWidth = 4.2f
                strokeJoin = Paint.Join.MITER
                color = P5_INK_BLACK
            }
            canvas.drawPath(cardPath, outerInkStroke)

            val innerLightStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.style = Paint.Style.STROKE
                strokeWidth = 2.2f
                strokeJoin = Paint.Join.MITER
                color = style.strokeColor
            }
            canvas.drawPath(cardPath, innerLightStroke)
        }

        return bitmap
    }

    private fun createP5WeekDotBitmap(
        state: DayState,
        isToday: Boolean,
        style: WidgetVisualStyle,
    ): Bitmap {
        val size = 48
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = size / 2f
        val cy = size / 2f

        fun obliqueDiamond(rx: Float, ry: Float, skewX: Float): Path = Path().apply {
            moveTo(cx + skewX, cy - ry)
            lineTo(cx + rx, cy - skewX * 0.35f)
            lineTo(cx - skewX, cy + ry)
            lineTo(cx - rx, cy + skewX * 0.35f)
            close()
        }

        // Today emphasis: outer high-contrast highlight diamond frame.
        if (isToday) {
            val todayFrame = obliqueDiamond(rx = 20.5f, ry = 20.5f, skewX = 3.2f)
            val todayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.style = Paint.Style.STROKE
                strokeWidth = 3.2f
                strokeJoin = Paint.Join.MITER
                color = style.highlightColor
            }
            canvas.drawPath(todayFrame, todayPaint)
        }

        val innerR = if (isToday) 13.5f else 16.5f
        val innerSkew = if (isToday) 2.2f else 2.8f
        val diamond = obliqueDiamond(rx = innerR, ry = innerR, skewX = innerSkew)

        when (state) {
            DayState.EXERCISED -> {
                val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.FILL
                    color = style.accentFill
                }
                val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.STROKE
                    strokeWidth = 2.6f
                    strokeJoin = Paint.Join.MITER
                    color = style.strokeColor
                }
                canvas.drawPath(diamond, fill)
                canvas.drawPath(diamond, stroke)
            }
            DayState.REST -> {
                val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.FILL
                    color = if (style.isLightSurface) P5_PAPER_REST_FILL else P5_REST_FILL
                }
                canvas.drawPath(diamond, fill)
                canvas.save()
                canvas.clipPath(diamond)
                val hatchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.STROKE
                    strokeWidth = 2.2f
                    color = withAlpha(style.strokeColor, if (style.isLightSurface) 0xCC else 0xB3)
                }
                var offset = -size.toFloat()
                while (offset <= size.toFloat()) {
                    canvas.drawLine(offset, size.toFloat(), offset + size.toFloat(), 0f, hatchPaint)
                    offset += 7.5f
                }
                canvas.restore()
                val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.STROKE
                    strokeWidth = 2.2f
                    strokeJoin = Paint.Join.MITER
                    color = withAlpha(style.strokeColor, if (style.isLightSurface) 0xE6 else 0xCC)
                }
                canvas.drawPath(diamond, stroke)
            }
            DayState.UNRECORDED -> {
                val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.STROKE
                    strokeWidth = 2.4f
                    strokeJoin = Paint.Join.MITER
                    color = if (isToday) {
                        style.strokeColor
                    } else {
                        withAlpha(style.strokeColor, if (style.isLightSurface) 0xCC else 0xA8)
                    }
                }
                canvas.drawPath(diamond, stroke)
            }
        }
        return bitmap
    }

    private fun withAlpha(argb: Int, alpha: Int): Int =
        (argb and 0x00FFFFFF) or ((alpha and 0xFF) shl 24)

    private const val P5_INK_BLACK = 0xFF0D0D0D.toInt()
    private const val P5_SURFACE_MUTED = 0xFF22242C.toInt()
    private const val P5_REST_FILL = 0xFF2D303A.toInt()
    private const val P5_PAPER_WHITE = 0xFFF7F6F2.toInt()
    private const val P5_PAPER_MUTED = 0xFFE6E4DC.toInt()
    private const val P5_PAPER_REST_FILL = 0xFFE5E2DA.toInt()
    private const val REQ_OPEN_APP = 1001
    private const val REQ_OPEN_RECORD = 1002
    private const val REQ_TOGGLE_REST = 1003
}

internal fun openRecordTodayIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java).apply {
        putExtra(MainActivity.EXTRA_OPEN_RECORD_TODAY, true)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
