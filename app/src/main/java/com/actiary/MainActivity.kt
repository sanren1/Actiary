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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.actiary.theme.AppTheme
import com.actiary.theme.resolveTheme
import com.actiary.ui.calendar.HomeScreen
import com.actiary.ui.record.RecordSheet
import com.actiary.ui.settings.AppearanceScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

private enum class Screen { HOME, APPEARANCE }

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
            DisposableEffect(tokens.dark) {
                val t = AndroidColor.TRANSPARENT
                val bars = if (tokens.dark) SystemBarStyle.dark(t) else SystemBarStyle.light(t, t)
                enableEdgeToEdge(bars, bars)
                onDispose {}
            }
            AppTheme(tokens) {
                var selected by rememberSaveable { mutableStateOf(LocalDate.now()) }
                var editing by rememberSaveable { mutableStateOf<Long?>(null) }
                var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
                BackHandler(enabled = screen == Screen.APPEARANCE) { screen = Screen.HOME }
                when (screen) {
                    Screen.HOME -> HomeScreen(
                        repository = container.recordRepository,
                        photoStore = container.photoStore,
                        selected = selected,
                        onSelect = { selected = it },
                        onOpenAppearance = { screen = Screen.APPEARANCE },
                        onEdit = { editing = it.toEpochDay() },
                    )
                    Screen.APPEARANCE -> AppearanceScreen(
                        settings = settings,
                        onThemeMode = { lifecycleScope.launch { repository.setThemeMode(it) } },
                        onPreset = { lifecycleScope.launch { repository.setAccentPreset(it) } },
                        onGlassLevel = { lifecycleScope.launch { repository.setGlassLevel(it) } },
                        onGlassTint = { lifecycleScope.launch { repository.setGlassTintStrength(it) } },
                        onAmbient = { lifecycleScope.launch { repository.setAmbientBackground(it) } },
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
            }
        }
    }
}
