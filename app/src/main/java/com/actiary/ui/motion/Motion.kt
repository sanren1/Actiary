package com.actiary.ui.motion

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleStartEffect
import com.actiary.glass.stiffnessFromResponse

object Motion {
    // Navigation parameters follow NEORUAA/Glasmio's docs/ios-navigation-motion.md (Apache-2.0).
    const val PUSH_DAMPING = 1f
    const val PUSH_RESPONSE = 0.3f
    const val RELEASE_DAMPING = 0.85f
    const val RELEASE_RESPONSE = 0.3f
    const val TRACK_DAMPING = 0.85f
    const val TRACK_RESPONSE = 0.08f
    const val PARALLAX = 0.3f
    const val DIM_ALPHA = 0.10f

    const val TAB_SHIFT = 0.08f
    const val DAY_DETAIL_SHIFT = 0.08f
    const val APPEAR_SCALE = 0.9f
    const val DIALOG_SCALE = 0.92f
    const val FADE_IN_MS = 200
    const val FADE_OUT_MS = 150
    const val REDUCED_MAX_MS = 120

    fun pushSpec(
        damping: Float = PUSH_DAMPING,
        response: Float = PUSH_RESPONSE,
    ): SpringSpec<Float> = spring(dampingRatio = damping, stiffness = stiffnessFromResponse(response))
}

fun parallaxOffsetPx(progress: Float, widthPx: Float): Float = -Motion.PARALLAX * widthPx * progress

fun dimAlpha(progress: Float): Float = Motion.DIM_ALPHA * progress

fun reducedMotion(animatorScale: Float, powerSave: Boolean): Boolean = animatorScale == 0f || powerSave

internal val LocalMotionReduced = compositionLocalOf { false }

@Composable
fun rememberMotionReduced(): Boolean = LocalMotionReduced.current

/** Observes system preferences once at the Activity root. */
@Composable
internal fun rememberSystemMotionReduced(): Boolean {
    val context = LocalContext.current
    val resolver = context.contentResolver
    val powerManager = remember(context) { context.getSystemService(PowerManager::class.java) }
    val setting = Settings.Global.ANIMATOR_DURATION_SCALE

    fun readPreferences(): Boolean =
        reducedMotion(Settings.Global.getFloat(resolver, setting, 1f), powerManager.isPowerSaveMode)

    var reduced by remember(context) { mutableStateOf(readPreferences()) }
    LifecycleStartEffect(context) {
        fun refresh() {
            reduced = readPreferences()
        }

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = refresh()
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) = refresh()
        }

        resolver.registerContentObserver(Settings.Global.getUriFor(setting), false, observer)
        val filter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        refresh()

        onStopOrDispose {
            resolver.unregisterContentObserver(observer)
            context.unregisterReceiver(receiver)
        }
    }
    return reduced
}
