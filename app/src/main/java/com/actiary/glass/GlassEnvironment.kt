package com.actiary.glass

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.ThemeTokens
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop

private fun Context.animatorScaleIsZero() =
    Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

/** System "remove animations" state, refreshed on resume. */
@Composable
internal fun rememberAnimationsOff(): Boolean {
    val context = LocalContext.current
    var off by remember { mutableStateOf(context.animatorScaleIsZero()) }
    LifecycleResumeEffect(Unit) {
        off = context.animatorScaleIsZero()
        onPauseOrDispose {}
    }
    return off
}

@Composable
private fun rememberPowerSave(): Boolean {
    val context = LocalContext.current
    val pm = remember { context.getSystemService(PowerManager::class.java) }
    var saving by remember { mutableStateOf(pm.isPowerSaveMode) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) { saving = pm.isPowerSaveMode }
        }
        val filter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        if (Build.VERSION.SDK_INT >= 33) context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else context.registerReceiver(receiver, filter)
        saving = pm.isPowerSaveMode
        onDispose { context.unregisterReceiver(receiver) }
    }
    return saving
}

@Composable
fun rememberGlassDegraded(): Boolean {
    val powerSave = rememberPowerSave()
    val animationsOff = rememberAnimationsOff()
    return powerSave || animationsOff
}

/**
 * Two backdrop layers: ambient background and page content. Floating overlays (sheet, dialog, menu,
 * bars) go in [overlay], outside the content layer, so they sample the content below them.
 */
@Composable
fun GlassHost(
    tokens: ThemeTokens,
    profile: GlassProfile,
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable () -> Unit,
) {
    val bg = rememberGlassLayer()
    val contentLayer = rememberGlassLayer()
    // Overlays sample ambient + content, so transparent page areas still show the background.
    val combined = rememberCombinedBackdrop(bg, contentLayer)
    val layers = remember(bg, combined) { GlassLayers(background = bg, content = combined) }
    CompositionLocalProvider(
        LocalThemeTokens provides tokens,
        LocalGlassProfile provides profile,
        LocalGlassLayers provides layers,
    ) {
        Box(Modifier.fillMaxSize()) {
            AmbientBackground(tokens, Modifier.fillMaxSize().layerBackdrop(bg))
            Box(Modifier.fillMaxSize().layerBackdrop(contentLayer)) { content() }
            overlay()
        }
    }
}
