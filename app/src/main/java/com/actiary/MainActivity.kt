package com.actiary

import android.content.res.Configuration
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.actiary.glass.GlassBottomBar
import com.actiary.glass.GlassBottomBarHeight
import com.actiary.glass.GlassDialog
import com.actiary.glass.LocalGlassProfile
import com.actiary.glass.GlassFloatingAction
import com.actiary.glass.GlassHost
import com.actiary.glass.GlassTab
import com.actiary.glass.GlassTopBar
import com.actiary.glass.rememberGlassDegraded
import com.actiary.glass.resolveGlass
import com.actiary.theme.AppTheme
import com.actiary.theme.resolveTheme
import com.actiary.ui.calendar.HomeChrome
import com.actiary.ui.calendar.HomeScreen
import com.actiary.ui.common.FloatingInsets
import com.actiary.ui.common.LocalFloatingInsets
import com.actiary.ui.record.RecordSheet
import com.actiary.ui.review.ReviewPlaceholder
import com.actiary.ui.settings.CustomTypesScreen
import com.actiary.ui.settings.SettingsScreen
import com.actiary.ui.stats.StatsPlaceholder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

private val BarGap = 12.dp

private enum class Tab { CALENDAR, REVIEW, STATS, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as ActiaryApp).container
        val repository = container.appearanceRepository
        // Tiny local DataStore file: a blocking first read at startup avoids a light-theme flash.
        val initial = runBlocking { repository.settings.first() }
        val systemDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        window.setBackgroundDrawable(ColorDrawable(resolveTheme(initial, systemDark).neutrals.background))
        setContent {
            val settings by repository.settings.collectAsStateWithLifecycle(initial)
            val tokens = resolveTheme(settings, isSystemInDarkTheme())
            val profile = resolveGlass(settings.glassLevel, settings.glassTintStrength, tokens, rememberGlassDegraded())
            DisposableEffect(tokens.dark) {
                val t = AndroidColor.TRANSPARENT
                val bars = if (tokens.dark) SystemBarStyle.dark(t) else SystemBarStyle.light(t, t)
                enableEdgeToEdge(bars, bars)
                onDispose {}
            }
            AppTheme(tokens) {
                var selected by rememberSaveable { mutableStateOf(LocalDate.now()) }
                var editing by rememberSaveable { mutableStateOf<Long?>(null) }
                var tab by rememberSaveable { mutableStateOf(Tab.CALENDAR) }
                var customTypes by rememberSaveable { mutableStateOf(false) }
                var pendingArchive by rememberSaveable { mutableStateOf<Long?>(null) }
                val holder = rememberSaveableStateHolder()
                var chrome by remember { mutableStateOf(HomeChrome()) }
                BackHandler(tab != Tab.CALENDAR) { tab = Tab.CALENDAR }
                // Registered after the tab handler (later handler wins): back first exits the
                // custom-types page, only then resets the tab to CALENDAR.
                BackHandler(customTypes) { customTypes = false }

                val navBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                // Bar + gap above the navigation bar; the page only clears the navigation bar when the bar is absent.
                val insets = FloatingInsets(navBar + GlassBottomBarHeight + BarGap)
                // Behind the sheet: page and base chrome are inert for TalkBack; the base also goes flat (spec 4.1 glass cap).
                val modalOpen = editing != null
                val baseProfile = if (editing != null) profile.copy(degraded = true) else profile
                val hideBase = Modifier.semantics { if (modalOpen) hideFromAccessibility() }
                val tabs = listOf(
                    GlassTab(Icons.Outlined.CalendarMonth, stringResource(R.string.tab_calendar)),
                    GlassTab(Icons.Outlined.PhotoLibrary, stringResource(R.string.tab_review)),
                    GlassTab(Icons.Outlined.BarChart, stringResource(R.string.tab_stats)),
                    GlassTab(Icons.Outlined.Settings, stringResource(R.string.tab_settings)),
                )
                val onHome = tab == Tab.CALENDAR && !customTypes
                CompositionLocalProvider(LocalFloatingInsets provides insets) {
                    GlassHost(
                        tokens, profile,
                        overlay = {
                            CompositionLocalProvider(LocalGlassProfile provides baseProfile) { Box(Modifier.fillMaxSize().then(hideBase)) {
                            if (!customTypes) {
                                GlassBottomBar(
                                    tabs, tab.ordinal, { tab = Tab.entries[it] },
                                    Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = BarGap),
                                )
                                if (onHome) {
                                    if (chrome.pastTitle) {
                                        GlassTopBar(stringResource(R.string.home_title), true, Modifier.align(Alignment.TopCenter))
                                    }
                                    chrome.fab?.let { label ->
                                        GlassFloatingAction(
                                            stringResource(label), Icons.Outlined.Edit, { editing = selected.toEpochDay() },
                                            Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = insets.bottom + BarGap),
                                        )
                                    }
                                }
                            }
                            } }
                            editing?.let { day ->
                                RecordSheet(
                                    date = LocalDate.ofEpochDay(day),
                                    repository = container.recordRepository,
                                    photoStore = container.photoStore,
                                    onDismiss = { editing = null },
                                    onSaved = { selected = it },
                                )
                            }
                            GlassDialog(
                                visible = pendingArchive != null,
                                text = stringResource(R.string.custom_type_archive_confirm),
                                confirm = stringResource(R.string.custom_type_archive),
                                dismiss = stringResource(R.string.record_cancel),
                                onConfirm = {
                                    val id = pendingArchive
                                    pendingArchive = null
                                    if (id != null) lifecycleScope.launch { container.recordRepository.archiveType(id) }
                                },
                                onDismiss = { pendingArchive = null },
                            )
                        },
                    ) {
                        CompositionLocalProvider(LocalGlassProfile provides baseProfile) { Box(Modifier.fillMaxSize().then(hideBase)) {
                        if (customTypes) {
                            holder.SaveableStateProvider("custom_types") {
                                CustomTypesScreen(
                                    repository = container.recordRepository,
                                    onBack = { customTypes = false },
                                    onRequestArchive = { pendingArchive = it.id },
                                )
                            }
                        } else holder.SaveableStateProvider(tab.name) { when (tab) {
                            Tab.CALENDAR -> HomeScreen(
                                repository = container.recordRepository,
                                photoStore = container.photoStore,
                                selected = selected,
                                onSelect = { selected = it },
                                onChrome = { chrome = it },
                                onEdit = { editing = it.toEpochDay() },
                            )
                            Tab.REVIEW -> ReviewPlaceholder()
                            Tab.STATS -> StatsPlaceholder()
                            Tab.SETTINGS -> SettingsScreen(
                                settings = settings,
                                onThemeMode = { lifecycleScope.launch { repository.setThemeMode(it) } },
                                onPreset = { lifecycleScope.launch { repository.setAccentPreset(it) } },
                                onGlassLevel = { lifecycleScope.launch { repository.setGlassLevel(it) } },
                                onGlassTint = { lifecycleScope.launch { repository.setGlassTintStrength(it) } },
                                onAmbient = { lifecycleScope.launch { repository.setAmbientBackground(it) } },
                                onCustomTypes = { customTypes = true },
                            )
                        } }
                        } }
                    }
                }
            }
        }
    }
}
