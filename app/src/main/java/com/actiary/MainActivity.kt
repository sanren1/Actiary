package com.actiary

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.actiary.data.appearance.AppearanceRepository
import com.actiary.data.appearance.AppearanceSettings
import com.actiary.data.appearance.appearanceDataStore
import com.actiary.theme.AppTheme
import com.actiary.theme.resolveTheme
import com.actiary.ui.settings.AppearanceScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = AppearanceRepository(applicationContext.appearanceDataStore)
        setContent {
            val settings by repository.settings.collectAsStateWithLifecycle(AppearanceSettings())
            val tokens = resolveTheme(settings, isSystemInDarkTheme())
            DisposableEffect(tokens.dark) {
                val t = AndroidColor.TRANSPARENT
                val bars = if (tokens.dark) SystemBarStyle.dark(t) else SystemBarStyle.light(t, t)
                enableEdgeToEdge(bars, bars)
                onDispose {}
            }
            AppTheme(tokens) {
                AppearanceScreen(
                    settings = settings,
                    onThemeMode = { lifecycleScope.launch { repository.setThemeMode(it) } },
                    onPreset = { lifecycleScope.launch { repository.setAccentPreset(it) } },
                )
            }
        }
    }
}
