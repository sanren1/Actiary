package com.actiary

import android.content.res.Configuration
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.actiary.data.appearance.resolve
import com.actiary.data.photo.recognizeWorkoutScreenshot
import com.actiary.domain.WorkoutScreenshot
import com.actiary.glass.GlassBottomBar
import com.actiary.glass.GlassBottomBarHeight
import com.actiary.glass.GlassDialog
import com.actiary.glass.LocalGlassProfile
import com.actiary.glass.GlassHost
import com.actiary.glass.GlassIconButton
import com.actiary.glass.GlassOverlayPanel
import com.actiary.glass.GlassTab
import com.actiary.glass.GlassTopBar
import com.actiary.glass.rememberGlassDegraded
import com.actiary.glass.resolveGlass
import com.actiary.theme.AppTheme
import com.actiary.theme.asColor
import com.actiary.theme.resolveTheme
import com.actiary.ui.calendar.HomeChrome
import com.actiary.ui.calendar.HomeScreen
import com.actiary.ui.common.FloatingInsets
import com.actiary.ui.common.LocalFloatingInsets
import com.actiary.ui.common.CUSTOM_TYPE_ICONS
import com.actiary.ui.record.DatePickerDialog
import com.actiary.ui.record.RecordDetailSheet
import com.actiary.ui.record.RecordSheet
import com.actiary.ui.records.RecordsScreen
import com.actiary.ui.settings.BackupActions
import com.actiary.ui.settings.BackupOverlay
import com.actiary.ui.settings.BackupState
import com.actiary.ui.settings.CustomTypeIconPicker
import com.actiary.ui.settings.CustomTypesScreen
import com.actiary.ui.settings.SettingsPage
import com.actiary.ui.settings.SettingsScreen
import com.actiary.ui.settings.parent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val BarGap = 6.dp

private enum class Tab { CALENDAR, RECORDS, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as ActiaryApp).container
        val backup = ViewModelProvider(this)[BackupActions::class.java]
        val repository = container.appearanceRepository
        // Tiny local DataStore file: a blocking first read at startup avoids a light-theme flash.
        val initial = runBlocking { repository.settings.first() }
        val recordsView = runBlocking { container.recordsPrefs.view.first() }
        val initialWeekStart = runBlocking { container.generalPrefs.weekStart.first() }
        val systemDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        window.setBackgroundDrawable(ColorDrawable(resolveTheme(initial, systemDark).neutrals.background))
        setContent {
            val settings by repository.settings.collectAsStateWithLifecycle(initial)
            val recent by repository.recentColors.collectAsStateWithLifecycle(emptyList())
            val weekStart by container.generalPrefs.weekStart.collectAsStateWithLifecycle(initialWeekStart)
            val firstDayOfWeek = weekStart.resolve()
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
                var viewing by rememberSaveable { mutableStateOf<Long?>(null) }
                var scanningUri by rememberSaveable { mutableStateOf<String?>(null) }
                var screenshot by rememberSaveable { mutableStateOf<WorkoutScreenshot?>(null) }
                val screenshotPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                    scanningUri = uri?.toString()
                }
                val exportPicker = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument("application/zip"), backup::exportTo,
                )
                val importPicker = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument(), backup::loadFrom,
                )
                LaunchedEffect(scanningUri) {
                    val uri = scanningUri ?: return@LaunchedEffect
                    val workout = try {
                        recognizeWorkoutScreenshot(this@MainActivity, uri)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        null
                    }
                    if (workout == null) {
                        Toast.makeText(this@MainActivity, R.string.home_import_failed, Toast.LENGTH_LONG).show()
                    } else {
                        screenshot = WorkoutScreenshot(uri, workout)
                        val today = LocalDate.now()
                        val firstDay = YearMonth.from(today).minusYears(10).atDay(1)
                        editing = workout.date?.takeIf { it in firstDay..today }?.toEpochDay()
                    }
                    scanningUri = null
                }
                var tab by rememberSaveable { mutableStateOf(Tab.CALENDAR) }
                var settingsPage by rememberSaveable { mutableStateOf(SettingsPage.HOME) }
                var confirmAppearanceReset by rememberSaveable { mutableStateOf(false) }
                var pendingTypeRemoval by rememberSaveable { mutableStateOf<Long?>(null) }
                val defaultTypeIcon = CUSTOM_TYPE_ICONS.first().key
                var typeIcon by rememberSaveable { mutableStateOf(defaultTypeIcon) }
                var pickingTypeIcon by rememberSaveable { mutableStateOf(false) }
                val holder = rememberSaveableStateHolder()
                var chrome by remember { mutableStateOf(HomeChrome()) }
                BackHandler(tab != Tab.CALENDAR) { tab = Tab.CALENDAR }
                // Overlays register later; back dismisses them before returning to settings home.
                BackHandler(tab == Tab.SETTINGS && settingsPage != SettingsPage.HOME) {
                    val parent = settingsPage.parent()
                    if (parent != null) settingsPage = parent
                }
                BackHandler(backup.state == BackupState.Picking) {}

                val navBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                // Include the bar, its gap, and the navigation inset in the scroll clearance.
                val insets = FloatingInsets(navBar + GlassBottomBarHeight + BarGap)
                // Behind the sheet: page and base chrome are inert for TalkBack; the base also goes flat (spec 4.1 glass cap).
                val choosingWorkoutDate = screenshot != null && editing == null
                val backupModal = backup.state != BackupState.Idle
                val modalOpen = editing != null || viewing != null || pendingTypeRemoval != null || pickingTypeIcon ||
                    scanningUri != null || choosingWorkoutDate || confirmAppearanceReset || backupModal
                val baseProfile = if (editing != null || viewing != null || confirmAppearanceReset || backupModal) {
                    profile.copy(degraded = true)
                } else {
                    profile
                }
                val hideBase = if (modalOpen) Modifier.clearAndSetSemantics { hideFromAccessibility() } else Modifier
                val tabs = listOf(
                    GlassTab(Icons.Outlined.CalendarMonth, stringResource(R.string.tab_calendar)),
                    GlassTab(Icons.Outlined.BarChart, stringResource(R.string.tab_records)),
                    GlassTab(Icons.Outlined.Settings, stringResource(R.string.tab_settings)),
                )
                val onHome = tab == Tab.CALENDAR
                val hideBottomBar = tab == Tab.SETTINGS &&
                    (settingsPage == SettingsPage.TYPES || settingsPage == SettingsPage.CUSTOM_COLOR)
                CompositionLocalProvider(LocalFloatingInsets provides insets) {
                    GlassHost(
                        tokens, profile,
                        overlay = {
                            CompositionLocalProvider(LocalGlassProfile provides baseProfile) { Box(Modifier.fillMaxSize().then(hideBase)) {
                            if (!hideBottomBar) {
                                GlassBottomBar(
                                    tabs, tab.ordinal, { if (!backupModal) tab = Tab.entries[it] },
                                    Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = BarGap),
                                )
                                if (onHome) {
                                    if (chrome.pastTitle) {
                                        GlassTopBar(stringResource(R.string.home_title), true, Modifier.align(Alignment.TopCenter))
                                    }
                                    GlassIconButton(
                                        Icons.Outlined.DocumentScanner,
                                        stringResource(R.string.home_import_screenshot),
                                        { screenshotPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                        Modifier.align(Alignment.TopEnd)
                                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                                            .padding(top = 8.dp, end = 16.dp),
                                    )
                                }
                            }
                            } }
                            val scanningLabel = stringResource(R.string.home_recognizing_screenshot)
                            GlassOverlayPanel(
                                visible = scanningUri != null,
                                onDismiss = { scanningUri = null },
                                contentPadding = PaddingValues(20.dp),
                                title = scanningLabel,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    CircularProgressIndicator(color = tokens.accent.accentAction.asColor())
                                    Text(scanningLabel, color = tokens.neutrals.textPrimary.asColor())
                                }
                            }
                            if (choosingWorkoutDate) {
                                DatePickerDialog(
                                    selected = selected.coerceAtMost(LocalDate.now()),
                                    firstDayOfWeek = firstDayOfWeek,
                                    onSelect = { editing = it.toEpochDay() },
                                    onDismiss = { screenshot = null },
                                    title = stringResource(R.string.home_import_select_date),
                                )
                            }
                            viewing?.let { day ->
                                key(day) {
                                    RecordDetailSheet(
                                        date = LocalDate.ofEpochDay(day),
                                        repository = container.recordRepository,
                                        photoStore = container.photoStore,
                                        onDismiss = { viewing = null },
                                        onEdit = {
                                            editing = day
                                            viewing = null
                                        },
                                    )
                                }
                            }
                            editing?.let { day ->
                                RecordSheet(
                                    date = LocalDate.ofEpochDay(day),
                                    firstDayOfWeek = firstDayOfWeek,
                                    repository = container.recordRepository,
                                    photoStore = container.photoStore,
                                    onDismiss = {
                                        screenshot = null
                                        editing = null
                                    },
                                    onSaved = { selected = it },
                                    screenshot = screenshot,
                                )
                            }
                            GlassDialog(
                                visible = confirmAppearanceReset,
                                text = stringResource(R.string.settings_reset_appearance_message),
                                confirm = stringResource(R.string.settings_reset),
                                dismiss = stringResource(R.string.record_cancel),
                                onConfirm = {
                                    confirmAppearanceReset = false
                                    lifecycleScope.launch { repository.resetAppearance() }
                                },
                                onDismiss = { confirmAppearanceReset = false },
                            )
                            GlassDialog(
                                visible = pendingTypeRemoval != null,
                                text = stringResource(R.string.custom_type_delete_confirm),
                                confirm = stringResource(R.string.custom_type_delete),
                                dismiss = stringResource(R.string.record_cancel),
                                onConfirm = {
                                    val id = pendingTypeRemoval
                                    pendingTypeRemoval = null
                                    if (id != null) lifecycleScope.launch { container.recordRepository.removeType(id) }
                                },
                                onDismiss = { pendingTypeRemoval = null },
                            )
                            CustomTypeIconPicker(
                                visible = pickingTypeIcon,
                                iconKey = typeIcon,
                                onSelect = {
                                    typeIcon = it
                                    pickingTypeIcon = false
                                },
                                onDismiss = { pickingTypeIcon = false },
                            )
                            BackupOverlay(backup)
                        },
                    ) {
                        CompositionLocalProvider(LocalGlassProfile provides baseProfile) { Box(Modifier.fillMaxSize().then(hideBase)) {
                        val pageKey = if (tab == Tab.SETTINGS) settingsPage.name else tab.name
                        holder.SaveableStateProvider(pageKey) { when (tab) {
                            Tab.CALENDAR -> HomeScreen(
                                repository = container.recordRepository,
                                photoStore = container.photoStore,
                                selected = selected,
                                firstDayOfWeek = firstDayOfWeek,
                                onSelect = { selected = it },
                                onChrome = { chrome = it },
                                onEdit = { editing = it.toEpochDay() },
                            )
                            Tab.RECORDS -> RecordsScreen(
                                repository = container.recordRepository,
                                photoStore = container.photoStore,
                                initialView = recordsView,
                                firstDayOfWeek = firstDayOfWeek,
                                chartPalette = settings.chartPalette,
                                onKind = { lifecycleScope.launch { container.recordsPrefs.setKind(it) } },
                                onMetric = { lifecycleScope.launch { container.recordsPrefs.setMetric(it) } },
                                onView = { viewing = it.toEpochDay() },
                            )
                            Tab.SETTINGS -> SettingsScreen(
                                page = settingsPage,
                                onPage = { if (!backupModal) settingsPage = it },
                                settings = settings,
                                weekStart = weekStart,
                                onWeekStart = { lifecycleScope.launch { container.generalPrefs.setWeekStart(it) } },
                                onThemeMode = { lifecycleScope.launch { repository.setThemeMode(it) } },
                                onPreset = { lifecycleScope.launch { repository.setAccentPreset(it) } },
                                onGlassLevel = { lifecycleScope.launch { repository.setGlassLevel(it) } },
                                onGlassTint = { lifecycleScope.launch { repository.setGlassTintStrength(it) } },
                                onAmbient = { lifecycleScope.launch { repository.setAmbientBackground(it) } },
                                onResetAppearance = { confirmAppearanceReset = true },
                                onCustomColor = { settingsPage = SettingsPage.CUSTOM_COLOR },
                                onChartPalette = { lifecycleScope.launch { repository.setChartPalette(it) } },
                                recent = recent,
                                onApplyCustomAccent = { hex -> lifecycleScope.launch { repository.applyCustomAccent(hex) } },
                                typesContent = { onBack ->
                                    CustomTypesScreen(
                                        repository = container.recordRepository,
                                        onBack = onBack,
                                        iconKey = typeIcon,
                                        onRequestIconPicker = { pickingTypeIcon = true },
                                        onResetIcon = { typeIcon = defaultTypeIcon },
                                        onRequestRemoval = { pendingTypeRemoval = it },
                                    )
                                },
                                onExportBackup = {
                                    backup.pickDocument {
                                        val date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                                        exportPicker.launch("actiary-$date.zip")
                                    }
                                },
                                onImportBackup = {
                                    backup.pickDocument { importPicker.launch(arrayOf("application/zip", "application/x-zip-compressed")) }
                                },
                                onCleanOrphanPhotos = backup::checkOrphanPhotos,
                            )
                        } }
                        } }
                    }
                }
            }
        }
    }
}
