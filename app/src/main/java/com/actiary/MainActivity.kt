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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.actiary.glass.GlassBottomBar
import com.actiary.glass.GlassFloatingAction
import com.actiary.glass.GlassHost
import com.actiary.glass.GlassIconButton
import com.actiary.glass.GlassMenu
import com.actiary.glass.GlassTab
import com.actiary.glass.GlassTopBar
import com.actiary.glass.rememberGlassDegraded
import com.actiary.glass.resolveGlass
import com.actiary.theme.AppTheme
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor
import com.actiary.theme.resolveTheme
import com.actiary.ui.calendar.HomeChrome
import com.actiary.ui.calendar.HomeScreen
import com.actiary.ui.common.FloatingInsets
import com.actiary.ui.common.LocalFloatingInsets
import com.actiary.ui.record.RecordSheet
import com.actiary.ui.review.ReviewPlaceholder
import com.actiary.ui.settings.AppearanceScreen
import com.actiary.ui.stats.StatsPlaceholder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

private enum class Tab { CALENDAR, REVIEW, STATS }

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
                var appearance by rememberSaveable { mutableStateOf(false) }
                val holder = rememberSaveableStateHolder()
                var menu by remember { mutableStateOf(false) }
                var chrome by remember { mutableStateOf(HomeChrome()) }
                BackHandler(tab != Tab.CALENDAR && !appearance) { tab = Tab.CALENDAR }
                BackHandler(appearance) { appearance = false }

                val navBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                // 64dp bar + 12dp gap above the navigation bar; the page without the bar only clears the navigation bar.
                val insets = FloatingInsets(if (appearance) navBar else navBar + 76.dp)
                val tabs = listOf(
                    GlassTab(Icons.Outlined.CalendarMonth, stringResource(R.string.tab_calendar)),
                    GlassTab(Icons.Outlined.PhotoLibrary, stringResource(R.string.tab_review)),
                    GlassTab(Icons.Outlined.BarChart, stringResource(R.string.tab_stats)),
                )
                val onHome = tab == Tab.CALENDAR && !appearance
                CompositionLocalProvider(LocalFloatingInsets provides insets) {
                    GlassHost(
                        tokens, profile,
                        overlay = {
                            if (!appearance) {
                                GlassBottomBar(
                                    tabs, tab.ordinal, { tab = Tab.entries[it]; menu = false },
                                    Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                                )
                            }
                            if (onHome) {
                                val more = stringResource(R.string.home_more)
                                if (chrome.pastTitle) {
                                    GlassTopBar(stringResource(R.string.home_title), true, Modifier.align(Alignment.TopCenter)) {
                                        Icon(
                                            Icons.Outlined.MoreHoriz, more,
                                            Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button) { menu = true }.padding(12.dp),
                                            tint = LocalThemeTokens.current.neutrals.textPrimary.asColor(),
                                        )
                                    }
                                } else {
                                    GlassIconButton(
                                        Icons.Outlined.MoreHoriz, more, { menu = true },
                                        Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 8.dp, end = 16.dp),
                                    )
                                }
                                chrome.fab?.let { label ->
                                    GlassFloatingAction(
                                        stringResource(label), Icons.Outlined.Edit, { editing = selected.toEpochDay() },
                                        Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = insets.bottom + 12.dp),
                                    )
                                }
                                GlassMenu(
                                    menu, { menu = false },
                                    listOf(stringResource(R.string.appearance_title) to { menu = false; appearance = true }),
                                    Modifier.statusBarsPadding().padding(top = 56.dp),
                                )
                            }
                            if (appearance) {
                                GlassIconButton(
                                    Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.nav_back), { appearance = false },
                                    Modifier.align(Alignment.TopStart).statusBarsPadding().padding(start = 12.dp, top = 8.dp),
                                )
                            }
                            editing?.let { day ->
                                RecordSheet(
                                    date = LocalDate.ofEpochDay(day),
                                    repository = container.recordRepository,
                                    photoStore = container.photoStore,
                                    onDismiss = { editing = null },
                                    onSaved = { selected = it },
                                )
                            }
                        },
                    ) {
                        val page = if (appearance) "appearance" else tab.name
                        holder.SaveableStateProvider(page) { when {
                            appearance -> AppearanceScreen(
                                settings = settings,
                                onThemeMode = { lifecycleScope.launch { repository.setThemeMode(it) } },
                                onPreset = { lifecycleScope.launch { repository.setAccentPreset(it) } },
                                onGlassLevel = { lifecycleScope.launch { repository.setGlassLevel(it) } },
                                onGlassTint = { lifecycleScope.launch { repository.setGlassTintStrength(it) } },
                                onAmbient = { lifecycleScope.launch { repository.setAmbientBackground(it) } },
                            )
                            tab == Tab.CALENDAR -> HomeScreen(
                                repository = container.recordRepository,
                                photoStore = container.photoStore,
                                selected = selected,
                                onSelect = { selected = it },
                                onChrome = { chrome = it },
                            )
                            tab == Tab.REVIEW -> ReviewPlaceholder()
                            else -> StatsPlaceholder()
                        } }
                    }
                }
            }
        }
    }
}
