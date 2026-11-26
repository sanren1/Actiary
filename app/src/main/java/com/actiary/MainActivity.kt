package com.actiary

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.net.Uri
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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.actiary.data.appearance.AppLanguagePrefs
import com.actiary.data.appearance.resolve
import com.actiary.data.db.DayStatus
import com.actiary.data.db.StrengthLogItemEntity
import com.actiary.data.photo.recognizeWorkoutScreenshot
import com.actiary.data.update.AppUpdater
import com.actiary.data.update.ReleaseUpdate
import com.actiary.data.update.UpdatePrefs
import com.actiary.domain.CheckInCelebrationData
import com.actiary.domain.CheckInKind
import com.actiary.domain.DayDraft
import com.actiary.domain.WorkoutScreenshot
import com.actiary.domain.WorkoutTrackParser
import com.actiary.domain.computeCurrentStreak
import com.actiary.domain.computeWeekExercisedDays
import com.actiary.domain.resolveCheckInKind
import com.actiary.glass.GlassBottomBar
import com.actiary.glass.GlassBottomBarHeight
import com.actiary.glass.GlassDialog
import com.actiary.glass.LocalGlassProfile
import com.actiary.glass.GlassHost
import com.actiary.glass.GlassIconButton
import com.actiary.glass.GlassOverlayPanel
import com.actiary.glass.GlassSampling
import com.actiary.glass.GlassTab
import com.actiary.glass.overlayPresent
import com.actiary.glass.rememberOverlayVisibility
import com.actiary.glass.resolveGlass
import com.actiary.theme.AppTheme
import com.actiary.theme.asColor
import com.actiary.theme.rememberAnimatedThemeTokens
import com.actiary.theme.resolveTheme
import com.actiary.ui.calendar.HomeScreen
import com.actiary.ui.calendar.buildStrengthSummaryText
import com.actiary.ui.common.FloatingInsets
import com.actiary.ui.common.LocalFloatingInsets
import com.actiary.ui.common.CUSTOM_TYPE_ICONS
import com.actiary.ui.common.exerciseTypeLabel
import com.actiary.ui.common.labelRes
import com.actiary.ui.motion.BackTarget
import com.actiary.ui.motion.LocalMotionReduced
import com.actiary.ui.motion.Motion
import com.actiary.ui.motion.PredictiveBack
import com.actiary.ui.motion.PushHost
import com.actiary.ui.motion.backTarget
import com.actiary.ui.motion.inactiveNavigation
import com.actiary.ui.motion.rememberPushMotion
import com.actiary.ui.motion.rememberSystemMotionReduced
import com.actiary.ui.record.CheckInCelebrationDialog
import com.actiary.ui.record.DatePickerDialog
import com.actiary.ui.record.RecordDetailSheet
import com.actiary.ui.record.RecordSheet
import com.actiary.ui.records.RecordsScreen
import com.actiary.ui.settings.AddTypeActions
import com.actiary.ui.settings.AddTypeSheet
import com.actiary.ui.settings.BackupActions
import com.actiary.ui.settings.BackupOverlay
import com.actiary.ui.settings.BackupState
import com.actiary.ui.settings.ColorEditorSheet
import com.actiary.ui.settings.CustomTypeIconPicker
import com.actiary.ui.settings.CustomTypesScreen
import com.actiary.ui.settings.EditingTypeSaver
import com.actiary.ui.settings.SettingsPage
import com.actiary.ui.settings.SettingsScreen
import com.actiary.ui.settings.StorageDetailsSheet
import com.actiary.ui.settings.ThirdPartyNoticesSheet
import com.actiary.ui.settings.UpdateDialog
import com.actiary.ui.settings.WorkoutPlansOverlayState
import com.actiary.ui.settings.WorkoutPlansOverlays
import com.actiary.ui.settings.WorkoutPlansScreen
import com.actiary.ui.settings.parent
import com.actiary.ui.settings.showsBottomBar
import com.actiary.widget.TodayWidgetUpdater
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlin.random.Random

private val BarGap = 0.dp

internal enum class Tab { CALENDAR, RECORDS, SETTINGS }

private data class IncomingImport(val uri: String, val isImage: Boolean, val token: Int)

class MainActivity : ComponentActivity() {
    private var openTodayRequest by mutableIntStateOf(0)
    private var incomingImport by mutableStateOf<IncomingImport?>(null)
    private var incomingImportSeq = 0

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguagePrefs.wrapContext(newBase))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeIncomingIntent(intent)
    }

    private fun consumeIncomingIntent(target: Intent?) {
        if (target == null) return
        if (target.getBooleanExtra(EXTRA_OPEN_RECORD_TODAY, false)) {
            target.removeExtra(EXTRA_OPEN_RECORD_TODAY)
            openTodayRequest++
        }
        val uri: Uri? = when (target.action) {
            Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(target, Intent.EXTRA_STREAM, Uri::class.java)
            Intent.ACTION_VIEW -> target.data
            else -> null
        }
        if (uri != null) {
            val mime = (target.type ?: runCatching { contentResolver.getType(uri) }.getOrNull()).orEmpty().lowercase()
            val isImage = mime.startsWith("image/")
            incomingImport = IncomingImport(uri.toString(), isImage, ++incomingImportSeq)
            // Clear action so configuration changes do not replay the same share intent.
            target.action = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) consumeIncomingIntent(intent)
        val initialLanguage = AppLanguagePrefs.load(this)
        val container = (application as ActiaryApp).container
        val backup = ViewModelProvider(this)[BackupActions::class.java]
        val typeActions = ViewModelProvider(this)[AddTypeActions::class.java]
        val repository = container.appearanceRepository
        // Tiny local DataStore file: a blocking first read at startup avoids a light-theme flash.
        val initial = runBlocking { repository.settings.first() }
        val recordsView = runBlocking { container.recordsPrefs.view.first() }
        val initialWeekStart = runBlocking { container.generalPrefs.weekStart.first() }
        val systemDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val initialDynamicSeed = ContextCompat.getColor(this, android.R.color.system_accent1_500)
        window.setBackgroundDrawable(ColorDrawable(resolveTheme(initial, systemDark, initialDynamicSeed).neutrals.background))
        setContent {
            val settings by repository.settings.collectAsStateWithLifecycle(initial)
            val accentColors by repository.accentColors.collectAsStateWithLifecycle(emptyList())
            val weekStart by container.generalPrefs.weekStart.collectAsStateWithLifecycle(initialWeekStart)
            val firstDayOfWeek = weekStart.resolve()
            LocalConfiguration.current
            val dynamicSeed = ContextCompat.getColor(LocalContext.current, android.R.color.system_accent1_500)
            val targetTokens = resolveTheme(settings, isSystemInDarkTheme(), dynamicSeed)
            val reducedMotion = rememberSystemMotionReduced()
            val tokens = rememberAnimatedThemeTokens(targetTokens, reducedMotion)
            val profile = resolveGlass(settings.glassLevel, settings.glassTintStrength, tokens, reducedMotion)
            LaunchedEffect(targetTokens.neutrals.background) {
                window.setBackgroundDrawable(ColorDrawable(targetTokens.neutrals.background))
            }
            DisposableEffect(targetTokens.dark) {
                val t = AndroidColor.TRANSPARENT
                val bars = if (targetTokens.dark) SystemBarStyle.dark(t) else SystemBarStyle.light(t, t)
                enableEdgeToEdge(bars, bars)
                onDispose {}
            }
            AppTheme(tokens) {
                var selected by rememberSaveable { mutableStateOf(LocalDate.now()) }
                var editing by rememberSaveable { mutableStateOf<Long?>(null) }
                var viewing by rememberSaveable { mutableStateOf<Long?>(null) }
                var scanningUri by rememberSaveable { mutableStateOf<String?>(null) }
                var screenshot by rememberSaveable { mutableStateOf<WorkoutScreenshot?>(null) }
                var screenshotPhotoFile by rememberSaveable { mutableStateOf<String?>(null) }
                fun clearScreenshot() {
                    scanningUri = null
                    screenshot = null
                    screenshotPhotoFile?.let { container.photoStore.stagedFile(it).delete() }
                    screenshotPhotoFile = null
                }
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
                    val result = try {
                        val name = screenshotPhotoFile ?: container.recordRepository.stagePickedPhotos(listOf(uri)).single().fileName
                        val photo = container.photoStore.stagedFile(name)
                        if (scanningUri != uri) {
                            photo.delete()
                            return@LaunchedEffect
                        }
                        screenshotPhotoFile = name
                        val localUri = Uri.fromFile(photo).toString()
                        recognizeWorkoutScreenshot(this@MainActivity, localUri)?.let { WorkoutScreenshot(localUri, it) }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        null
                    }
                    if (scanningUri != uri) return@LaunchedEffect
                    if (result == null) {
                        val message = if (screenshotPhotoFile == null) R.string.record_photo_import_failed else R.string.home_import_failed
                        clearScreenshot()
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                    } else {
                        screenshot = result
                        val today = LocalDate.now()
                        val firstDay = YearMonth.from(today).minusYears(10).atDay(1)
                        editing = result.workout.date?.takeIf { it in firstDay..today }?.toEpochDay()
                        scanningUri = null
                    }
                }
                var tab by rememberSaveable { mutableStateOf(Tab.CALENDAR) }
                LaunchedEffect(incomingImport?.token) {
                    val req = incomingImport ?: return@LaunchedEffect
                    incomingImport = null
                    tab = Tab.CALENDAR
                    viewing = null
                    editing = null
                    clearScreenshot()
                    if (req.isImage) {
                        scanningUri = req.uri
                    } else {
                        val parsed = withContext(Dispatchers.IO) {
                            runCatching {
                                contentResolver.openInputStream(Uri.parse(req.uri))?.use { stream ->
                                    WorkoutTrackParser.parse(stream)
                                }
                            }.getOrNull()
                        }
                        if (parsed == null) {
                            Toast.makeText(this@MainActivity, R.string.home_import_failed, Toast.LENGTH_LONG).show()
                        } else {
                            screenshot = WorkoutScreenshot(uri = null, workout = parsed)
                            val today = LocalDate.now()
                            val firstDay = YearMonth.from(today).minusYears(10).atDay(1)
                            editing = parsed.date?.takeIf { it in firstDay..today }?.toEpochDay()
                        }
                    }
                }
                LaunchedEffect(openTodayRequest) {
                    if (openTodayRequest > 0) {
                        val today = LocalDate.now()
                        tab = Tab.CALENDAR
                        selected = today
                        viewing = null
                        editing = today.toEpochDay()
                    }
                }
                var settingsPage by rememberSaveable { mutableStateOf(SettingsPage.HOME) }
                var confirmAppearanceReset by rememberSaveable { mutableStateOf(false) }
                var accentEditorOpen by rememberSaveable { mutableStateOf(false) }
                var editingAccent by rememberSaveable { mutableStateOf<String?>(null) }
                var pendingAccentRemoval by rememberSaveable { mutableStateOf<String?>(null) }
                var pendingTypeRemoval by rememberSaveable { mutableStateOf<Long?>(null) }
                var editingTypeOpen by rememberSaveable { mutableStateOf(false) }
                var editingType by rememberSaveable(stateSaver = EditingTypeSaver) { mutableStateOf(null) }
                val defaultTypeIcon = CUSTOM_TYPE_ICONS.first().key
                var typeIcon by rememberSaveable { mutableStateOf<String?>(defaultTypeIcon) }
                var pickingTypeIcon by rememberSaveable { mutableStateOf(false) }
                val workoutPlansOverlay = remember { WorkoutPlansOverlayState() }
                var storageDetailsOpen by rememberSaveable { mutableStateOf(false) }
                var storageRefreshKey by rememberSaveable { mutableIntStateOf(0) }
                var thirdPartyNoticesOpen by rememberSaveable { mutableStateOf(false) }
                var availableUpdate by remember { mutableStateOf<ReleaseUpdate?>(null) }
                var checkingUpdate by remember { mutableStateOf(false) }
                var checkInCelebration by remember { mutableStateOf<CheckInCelebrationData?>(null) }
                val appVersion = remember {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
                }
                LaunchedEffect(Unit) {
                    val found = AppUpdater.checkForUpdate(appVersion).getOrNull() ?: return@LaunchedEffect
                    if (found.version != UpdatePrefs.getIgnoredVersion(this@MainActivity)) {
                        availableUpdate = found
                    }
                }
                fun triggerManualUpdateCheck() {
                    if (checkingUpdate) return
                    checkingUpdate = true
                    lifecycleScope.launch {
                        val result = AppUpdater.checkForUpdate(appVersion)
                        checkingUpdate = false
                        result.onSuccess { found ->
                            if (found != null) {
                                availableUpdate = found
                            } else {
                                Toast.makeText(this@MainActivity, R.string.update_up_to_date, Toast.LENGTH_SHORT).show()
                            }
                        }.onFailure {
                            Toast.makeText(this@MainActivity, R.string.update_check_failed, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                val holder = rememberSaveableStateHolder()
                val push = rememberPushMotion(settingsPage, reducedMotion)
                val tabTransition = updateTransition(tab, label = "tabs")
                val ltr = LocalLayoutDirection.current == LayoutDirection.Ltr

                val choosingWorkoutDate = screenshot != null && editing == null
                val backupModal = backup.state != BackupState.Idle
                val scanVisibility = rememberOverlayVisibility(scanningUri != null)
                val dateVisibility = rememberOverlayVisibility(choosingWorkoutDate)
                val resetVisibility = rememberOverlayVisibility(confirmAppearanceReset)
                val accentRemovalVisibility = rememberOverlayVisibility(pendingAccentRemoval != null)
                val removalVisibility = rememberOverlayVisibility(pendingTypeRemoval != null)
                val planDeleteVisibility = rememberOverlayVisibility(workoutPlansOverlay.pendingPlanDelete != null)
                val customExerciseVisibility = rememberOverlayVisibility(workoutPlansOverlay.customExerciseOpen)
                val iconVisibility = rememberOverlayVisibility(pickingTypeIcon)
                val updateVisibility = rememberOverlayVisibility(availableUpdate != null)
                val checkInVisibility = rememberOverlayVisibility(checkInCelebration != null)
                fun hasOverlay() = editing != null || viewing != null ||
                    accentEditorOpen || pendingAccentRemoval != null || accentRemovalVisibility.overlayPresent ||
                    pendingTypeRemoval != null || editingTypeOpen || pickingTypeIcon || scanningUri != null ||
                    workoutPlansOverlay.hasVisibleOverlay(planDeleteVisibility, customExerciseVisibility) ||
                    storageDetailsOpen || thirdPartyNoticesOpen || availableUpdate != null || updateVisibility.overlayPresent ||
                    checkInCelebration != null || checkInVisibility.overlayPresent ||
                    (screenshot != null && editing == null) || confirmAppearanceReset || backup.state != BackupState.Idle ||
                    scanVisibility.overlayPresent || dateVisibility.overlayPresent || resetVisibility.overlayPresent ||
                    removalVisibility.overlayPresent || iconVisibility.overlayPresent

                val target = backTarget(hasOverlay(), tab, settingsPage)
                val fromTab = tab
                val fromSettingsPage = settingsPage
                fun stillOwnsBack() = tab == fromTab && settingsPage == fromSettingsPage &&
                    backTarget(hasOverlay(), tab, settingsPage) == target
                fun goToSettings(page: SettingsPage) {
                    push.navigate(page, reducedMotion)
                    settingsPage = page
                }

                PredictiveBack(
                    enabled = target == BackTarget.SettingsParent || target == BackTarget.ToCalendar,
                    onProgress = {
                        if (!reducedMotion && target == BackTarget.SettingsParent && stillOwnsBack()) push.drag(it)
                    },
                    onCommit = {
                        if (stillOwnsBack()) {
                            when (target) {
                                BackTarget.SettingsParent -> settingsPage.parent()?.let(::goToSettings)
                                BackTarget.ToCalendar -> tab = Tab.CALENDAR
                                BackTarget.None, BackTarget.CloseOverlay -> Unit
                            }
                        } else {
                            push.cancelDrag()
                        }
                    },
                    onCancel = push::cancelDrag,
                )
                BackHandler(backup.state == BackupState.Picking) {}

                val showBottomBar = tab != Tab.SETTINGS || push.state.top.showsBottomBar()
                val navBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                val regularInsets = FloatingInsets((navBar - 3.dp).coerceAtLeast(10.dp) + GlassBottomBarHeight + 8.dp)
                val childInsets = FloatingInsets(navBar)
                val modalOpen = hasOverlay()
                val bottomBarProfile = if (editing != null || viewing != null ||
                    resetVisibility.overlayPresent || confirmAppearanceReset || backupModal
                ) {
                    profile.copy(degraded = true)
                } else {
                    profile
                }
                val hideBase = if (modalOpen) Modifier.inactiveNavigation() else Modifier
                val tabs = listOf(
                    GlassTab(Icons.Outlined.CalendarMonth, stringResource(R.string.tab_calendar)),
                    GlassTab(Icons.Outlined.BarChart, stringResource(R.string.tab_records)),
                    GlassTab(Icons.Outlined.Settings, stringResource(R.string.tab_settings)),
                )
                val onHome = tab == Tab.CALENDAR
                val controlsSpec = Motion.pushSpec(damping = Motion.RELEASE_DAMPING)
                val controlEnter = if (reducedMotion) fadeIn(tween(Motion.REDUCED_MAX_MS)) else
                    fadeIn(controlsSpec) + scaleIn(controlsSpec, initialScale = Motion.APPEAR_SCALE)
                val controlExit = if (reducedMotion) fadeOut(tween(Motion.REDUCED_MAX_MS)) else
                    fadeOut(controlsSpec) + scaleOut(controlsSpec, targetScale = Motion.APPEAR_SCALE)
                val barSlide = spring<IntOffset>(dampingRatio = Motion.RELEASE_DAMPING, stiffness = controlsSpec.stiffness)
                CompositionLocalProvider(LocalMotionReduced provides reducedMotion) {
                    GlassHost(
                        tokens, profile,
                        overlay = {
                            CompositionLocalProvider(LocalGlassProfile provides bottomBarProfile) {
                                Box(Modifier.fillMaxSize().then(hideBase)) {
                                    AnimatedVisibility(
                                        visible = showBottomBar,
                                        modifier = Modifier.align(Alignment.BottomCenter)
                                            .padding(start = 16.dp, end = 16.dp, bottom = (navBar - 3.dp).coerceAtLeast(10.dp)),
                                        enter = if (reducedMotion) controlEnter else
                                            controlEnter + slideInVertically(barSlide) { it / 6 },
                                        exit = if (reducedMotion) controlExit else
                                            controlExit + slideOutVertically(barSlide) { it / 6 },
                                    ) {
                                        // The bar stays mounted while root pages animate underneath it.
                                        GlassBottomBar(
                                            tabs,
                                            tab.ordinal,
                                            {
                                                if (!hasOverlay() && (tab != Tab.SETTINGS || push.state.top.showsBottomBar())) {
                                                    tab = Tab.entries[it]
                                                }
                                            },
                                            if (showBottomBar) Modifier else Modifier.inactiveNavigation(),
                                        )
                                    }
                                    AnimatedVisibility(
                                        visible = onHome,
                                        modifier = Modifier.align(Alignment.TopEnd)
                                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
                                        enter = controlEnter,
                                        exit = controlExit,
                                    ) {
                                        Box(Modifier.padding(top = 8.dp, end = 16.dp, start = 16.dp, bottom = 16.dp)) {
                                            GlassIconButton(
                                                Icons.Outlined.DocumentScanner,
                                                stringResource(R.string.home_import_screenshot),
                                                {
                                                    if (tab == Tab.CALENDAR && !hasOverlay()) {
                                                        screenshotPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                                    }
                                                },
                                                if (onHome) Modifier else Modifier.inactiveNavigation(),
                                                sampling = GlassSampling.Background,
                                                enabled = onHome && !modalOpen,
                                            )
                                        }
                                    }
                                }
                            }
                            val scanningLabel = stringResource(R.string.home_recognizing_screenshot)
                            GlassOverlayPanel(
                                visible = scanningUri != null,
                                onDismiss = { if (scanningUri != null) clearScreenshot() },
                                contentPadding = PaddingValues(20.dp),
                                title = scanningLabel,
                                visibility = scanVisibility,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    CircularProgressIndicator(color = tokens.accent.accentAction.asColor())
                                    Text(scanningLabel, color = tokens.neutrals.textPrimary.asColor())
                                }
                            }
                            DatePickerDialog(
                                visible = choosingWorkoutDate,
                                selected = selected.coerceAtMost(LocalDate.now()),
                                firstDayOfWeek = firstDayOfWeek,
                                onSelect = { if (screenshot != null && editing == null) editing = it.toEpochDay() },
                                onDismiss = { if (editing == null) clearScreenshot() },
                                title = stringResource(R.string.home_import_select_date),
                                visibility = dateVisibility,
                            )
                            viewing?.let { day ->
                                key(day) {
                                    RecordDetailSheet(
                                        date = LocalDate.ofEpochDay(day),
                                        repository = container.recordRepository,
                                        strengthRepository = container.strengthRepository,
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
                                key(day) {
                                    RecordSheet(
                                        date = LocalDate.ofEpochDay(day),
                                        firstDayOfWeek = firstDayOfWeek,
                                        repository = container.recordRepository,
                                        strengthRepository = container.strengthRepository,
                                        photoStore = container.photoStore,
                                        onDismiss = {
                                            clearScreenshot()
                                            editing = null
                                        },
                                        onManagePlans = {
                                            clearScreenshot()
                                            editing = null
                                            push.navigate(SettingsPage.WORKOUT_PLANS, reduced = true)
                                            settingsPage = SettingsPage.WORKOUT_PLANS
                                            tab = Tab.SETTINGS
                                        },
                                        onSaved = { selected = it },
                                        onSavedExercisedToday = { savedDraft ->
                                            lifecycleScope.launch {
                                                val today = LocalDate.now()
                                                if (savedDraft.date != today) return@launch
                                                if (container.generalPrefs.lastCelebratedEpochDay() == today.toEpochDay()) return@launch
                                                val data = buildCheckInCelebrationData(
                                                    container = container,
                                                    savedDraft = savedDraft,
                                                    today = today,
                                                    firstDayOfWeek = firstDayOfWeek,
                                                )
                                                container.generalPrefs.setLastCelebratedEpochDay(today.toEpochDay())
                                                checkInCelebration = data
                                            }
                                        },
                                        screenshot = screenshot,
                                    )
                                }
                            }
                            if (accentEditorOpen) {
                                ColorEditorSheet(
                                    settings = settings,
                                    initialHex = editingAccent,
                                    onApply = { hex ->
                                        val previous = editingAccent
                                        lifecycleScope.launch { repository.savePaletteAccent(hex, previous) }
                                    },
                                    onDismiss = {
                                        accentEditorOpen = false
                                        editingAccent = null
                                    },
                                )
                            }
                            GlassDialog(
                                visible = pendingAccentRemoval != null,
                                text = stringResource(R.string.accent_delete_confirm),
                                confirm = stringResource(R.string.accent_delete),
                                dismiss = stringResource(R.string.record_cancel),
                                onConfirm = {
                                    val hex = pendingAccentRemoval
                                    pendingAccentRemoval = null
                                    if (hex != null) lifecycleScope.launch { repository.removePaletteAccent(hex) }
                                },
                                onDismiss = { pendingAccentRemoval = null },
                                visibility = accentRemovalVisibility,
                            )
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
                                visibility = resetVisibility,
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
                                visibility = removalVisibility,
                            )
                            if (editingTypeOpen) {
                                val activeTypes by container.recordRepository.types.collectAsStateWithLifecycle(emptyList())
                                AddTypeSheet(
                                    actions = typeActions,
                                    editingType = editingType,
                                    existingTypes = activeTypes,
                                    iconKey = typeIcon,
                                    covered = iconVisibility.overlayPresent,
                                    onRequestIconPicker = { pickingTypeIcon = true },
                                    onDismiss = {
                                        editingTypeOpen = false
                                        editingType = null
                                        typeActions.reset()
                                    },
                                )
                            }
                            CustomTypeIconPicker(
                                visible = pickingTypeIcon,
                                iconKey = typeIcon,
                                defaultType = editingType,
                                onSelect = {
                                    typeIcon = it
                                    pickingTypeIcon = false
                                },
                                onDismiss = { pickingTypeIcon = false },
                                visibility = iconVisibility,
                            )
                            WorkoutPlansOverlays(
                                repository = container.strengthRepository,
                                state = workoutPlansOverlay,
                                deleteVisibility = planDeleteVisibility,
                                customExerciseVisibility = customExerciseVisibility,
                            )
                            if (storageDetailsOpen) {
                                StorageDetailsSheet(
                                    onCacheCleared = { storageRefreshKey++ },
                                    onDismiss = { storageDetailsOpen = false },
                                )
                            }
                            if (thirdPartyNoticesOpen) {
                                ThirdPartyNoticesSheet(
                                    onDismiss = { thirdPartyNoticesOpen = false },
                                )
                            }
                            UpdateDialog(
                                update = availableUpdate,
                                visibility = updateVisibility,
                                onDismiss = { availableUpdate = null },
                                onIgnore = { version ->
                                    UpdatePrefs.setIgnoredVersion(this@MainActivity, version)
                                    availableUpdate = null
                                },
                            )
                            BackupOverlay(backup)
                            CheckInCelebrationDialog(
                                data = checkInCelebration,
                                visibility = checkInVisibility,
                                onDismiss = { checkInCelebration = null },
                            )
                        },
                    ) {
                        CompositionLocalProvider(LocalGlassProfile provides bottomBarProfile) {
                            tabTransition.AnimatedContent(
                                modifier = Modifier.fillMaxSize().clipToBounds().then(hideBase),
                                transitionSpec = {
                                    val fadeInMs = if (reducedMotion) Motion.REDUCED_MAX_MS else Motion.FADE_IN_MS
                                    val fadeOutMs = if (reducedMotion) Motion.REDUCED_MAX_MS else Motion.FADE_OUT_MS
                                    val enter = fadeIn(tween(fadeInMs))
                                    val exit = fadeOut(tween(fadeOutMs))
                                    if (reducedMotion) {
                                        enter togetherWith exit
                                    } else {
                                        val forward = targetState.ordinal > initialState.ordinal
                                        val shift = if (forward == ltr) Motion.TAB_SHIFT else -Motion.TAB_SHIFT
                                        (enter + slideInHorizontally(tween(fadeInMs)) { (it * shift).roundToInt() }) togetherWith
                                            (exit + slideOutHorizontally(tween(fadeOutMs)) { (-it * shift).roundToInt() })
                                    }
                                },
                            ) { destination ->
                                val activeDestination = destination == tab
                                Box(Modifier.fillMaxSize().then(if (activeDestination) Modifier else Modifier.inactiveNavigation())) {
                                    CompositionLocalProvider(LocalFloatingInsets provides regularInsets) {
                                        when (destination) {
                                            Tab.CALENDAR -> holder.SaveableStateProvider(destination.name) {
                                                HomeScreen(
                                                    repository = container.recordRepository,
                                                    strengthRepository = container.strengthRepository,
                                                    photoStore = container.photoStore,
                                                    selected = selected,
                                                    firstDayOfWeek = firstDayOfWeek,
                                                    onSelect = { selected = it },
                                                    onEdit = { editing = it.toEpochDay() },
                                                )
                                            }
                                            Tab.RECORDS -> holder.SaveableStateProvider(destination.name) {
                                                RecordsScreen(
                                                    repository = container.recordRepository,
                                                    strengthRepository = container.strengthRepository,
                                                    photoStore = container.photoStore,
                                                    initialView = recordsView,
                                                    firstDayOfWeek = firstDayOfWeek,
                                                    chartPalette = settings.chartPalette,
                                                    onKind = { lifecycleScope.launch { container.recordsPrefs.setKind(it) } },
                                                    onMetric = { lifecycleScope.launch { container.recordsPrefs.setMetric(it) } },
                                                    onView = { viewing = it.toEpochDay() },
                                                )
                                            }
                                            Tab.SETTINGS -> PushHost(push) { page ->
                                                val renderPage: @Composable () -> Unit = {
                                                    val insets = if (page.showsBottomBar()) regularInsets else childInsets
                                                    CompositionLocalProvider(LocalFloatingInsets provides insets) {
                                                        SettingsScreen(
                                                            page = page,
                                                            onPage = {
                                                                if (tab == Tab.SETTINGS && page == settingsPage && !hasOverlay()) goToSettings(it)
                                                            },
                                                            settings = settings,
                                                            dynamicSeed = dynamicSeed,
                                                            appLanguage = initialLanguage,
                                                            onAppLanguage = { lang ->
                                                                if (lang != initialLanguage) {
                                                                    AppLanguagePrefs.save(this@MainActivity, lang)
                                                                    TodayWidgetUpdater.refresh(this@MainActivity)
                                                                    recreate()
                                                                }
                                                            },
                                                            weekStart = weekStart,
                                                            onWeekStart = { lifecycleScope.launch { container.generalPrefs.setWeekStart(it) } },
                                                            onThemeMode = { lifecycleScope.launch { repository.setThemeMode(it) } },
                                                            onAmoledBlack = { lifecycleScope.launch { repository.setAmoledBlack(it) } },
                                                            accentColors = accentColors,
                                                            onSelectDynamicAccent = {
                                                                lifecycleScope.launch { repository.selectDynamicAccent() }
                                                            },
                                                            onSelectAccent = { hex ->
                                                                lifecycleScope.launch { repository.selectPaletteAccent(hex) }
                                                            },
                                                            onRemoveAccent = { pendingAccentRemoval = it },
                                                            onGlassLevel = { lifecycleScope.launch { repository.setGlassLevel(it) } },
                                                            onAmbient = { lifecycleScope.launch { repository.setAmbientBackground(it) } },
                                                            onResetAppearance = { confirmAppearanceReset = true },
                                                            onCustomColor = { hex ->
                                                                if (tab == Tab.SETTINGS && page == settingsPage) {
                                                                    editingAccent = hex
                                                                    accentEditorOpen = true
                                                                }
                                                            },
                                                            onChartPalette = { lifecycleScope.launch { repository.setChartPalette(it) } },
                                                            typesContent = { onBack ->
                                                                CustomTypesScreen(
                                                                    repository = container.recordRepository,
                                                                    onBack = onBack,
                                                                    onRequestAdd = {
                                                                        if (!hasOverlay()) {
                                                                            editingType = null
                                                                            typeIcon = defaultTypeIcon
                                                                            editingTypeOpen = true
                                                                        }
                                                                    },
                                                                    onRequestEdit = { type ->
                                                                        if (!hasOverlay()) {
                                                                            editingType = type
                                                                            typeIcon = type.iconKey
                                                                            editingTypeOpen = true
                                                                        }
                                                                    },
                                                                    onRequestRemoval = { pendingTypeRemoval = it },
                                                                )
                                                            },
                                                            workoutPlansContent = { onBack ->
                                                                WorkoutPlansScreen(
                                                                    repository = container.strengthRepository,
                                                                    onBack = onBack,
                                                                    onRequestNewPlan = {
                                                                        if (!hasOverlay()) workoutPlansOverlay.openNewPlan()
                                                                    },
                                                                    onRequestEditPlan = { plan ->
                                                                        if (!hasOverlay()) workoutPlansOverlay.openEditPlan(plan)
                                                                    },
                                                                    onRequestDeletePlan = { plan ->
                                                                        if (!hasOverlay()) workoutPlansOverlay.requestDeletePlan(plan)
                                                                    },
                                                                    onRequestNewExercise = {
                                                                        if (!hasOverlay()) workoutPlansOverlay.openNewExercise()
                                                                    },
                                                                    onRequestEditExercise = { exercise ->
                                                                        if (!hasOverlay()) workoutPlansOverlay.openEditExercise(exercise)
                                                                    },
                                                                    onRequestExerciseHistory = { exercise ->
                                                                        if (!hasOverlay()) workoutPlansOverlay.openExerciseHistory(exercise)
                                                                    },
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
                                                            onStorageDetails = { if (!hasOverlay()) storageDetailsOpen = true },
                                                            storageRefreshKey = storageRefreshKey,
                                                            onOpenNotices = { if (!hasOverlay()) thirdPartyNoticesOpen = true },
                                                            checkingUpdate = checkingUpdate,
                                                            onCheckUpdate = { if (!hasOverlay()) triggerManualUpdateCheck() },
                                                        )
                                                    }
                                                }
                                                if (page == SettingsPage.HOME) {
                                                    holder.SaveableStateProvider(page.name) { renderPage() }
                                                } else {
                                                    renderPage()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun loadTodayStats(
        container: AppContainer,
        today: LocalDate,
        firstDayOfWeek: DayOfWeek,
        includeToday: Boolean,
    ): Triple<Int, Int, Int> {
        val history = container.recordRepository.observeRange(today.minusYears(10), today).first()
        val exercisedEpochDays = history
            .filter { it.record.status == DayStatus.EXERCISED }
            .mapTo(HashSet()) { it.record.epochDay }
        val restEpochDays = history
            .filter { it.record.status == DayStatus.REST }
            .mapTo(HashSet()) { it.record.epochDay }
        if (includeToday) {
            exercisedEpochDays.add(today.toEpochDay())
            restEpochDays.remove(today.toEpochDay())
        }
        val streakDays = computeCurrentStreak(exercisedEpochDays, today, restEpochDays)
        val weekDays = computeWeekExercisedDays(exercisedEpochDays, today, firstDayOfWeek)
        val totalDays = container.recordRepository.countExercisedDays()
        return Triple(streakDays, weekDays, totalDays)
    }

    private suspend fun buildCheckInCelebrationData(
        container: AppContainer,
        savedDraft: DayDraft,
        today: LocalDate,
        firstDayOfWeek: DayOfWeek,
    ): CheckInCelebrationData {
        val (streakDays, weekDays, rawTotalDays) = loadTodayStats(
            container, today, firstDayOfWeek, includeToday = true,
        )
        val totalDays = maxOf(rawTotalDays, 1)
        val kind = resolveCheckInKind(totalDays, streakDays)
        val quoteIndex = when (kind) {
            CheckInKind.Daily -> Random.nextInt(DAILY_QUOTE_COUNT)
            is CheckInKind.TotalMilestone,
            is CheckInKind.StreakMilestone,
            -> Random.nextInt(MILESTONE_QUOTE_COUNT)
        }
        val strengthItems = savedDraft.entries.flatMap { it.strengthItems }
        val workoutSummary = if (strengthItems.isNotEmpty()) {
            val allExercises = container.strengthRepository.allExercises.first().associateBy { it.id }
            val logs = strengthItems.mapIndexed { idx, item ->
                StrengthLogItemEntity(
                    id = 0,
                    entryId = 0,
                    exerciseId = item.exerciseId,
                    sets = item.sets,
                    reps = item.reps,
                    weightKg = item.weightKg,
                    sortOrder = idx,
                )
            }
            buildStrengthSummaryText(
                logs = logs,
                exercises = allExercises,
                muscleLabel = { getString(it.labelRes()) },
                countsLabel = { exCount, setCount ->
                    getString(R.string.strength_summary_counts, exCount, setCount)
                },
            )
        } else if (savedDraft.entries.isNotEmpty()) {
            val typesById = container.recordRepository.allTypes.first().associateBy { it.id }
            savedDraft.entries
                .mapNotNull { entry -> typesById[entry.typeId]?.let { exerciseTypeLabel(it, resources) } }
                .filter { it.isNotBlank() }
                .distinct()
                .take(3)
                .joinToString(" · ")
                .ifBlank { null }
        } else {
            null
        }
        return CheckInCelebrationData(
            kind = kind,
            streakDays = streakDays,
            weekDays = weekDays,
            totalDays = totalDays,
            quoteIndex = quoteIndex,
            workoutSummary = workoutSummary,
        )
    }

    companion object {
        private const val DAILY_QUOTE_COUNT = 12
        private const val MILESTONE_QUOTE_COUNT = 6
        const val EXTRA_OPEN_RECORD_TODAY = "com.actiary.EXTRA_OPEN_RECORD_TODAY"
    }
}
