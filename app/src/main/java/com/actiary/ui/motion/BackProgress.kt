package com.actiary.ui.motion

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.actiary.Tab
import com.actiary.ui.settings.SettingsPage
import com.actiary.ui.settings.parent
import kotlinx.coroutines.CancellationException

internal sealed interface BackTarget {
    data object None : BackTarget
    data object CloseOverlay : BackTarget
    data object SettingsParent : BackTarget
    data object ToCalendar : BackTarget
}

internal fun backTarget(hasOverlay: Boolean, tab: Tab, settingsPage: SettingsPage): BackTarget = when {
    hasOverlay -> BackTarget.CloseOverlay
    tab == Tab.SETTINGS && settingsPage.parent() != null -> BackTarget.SettingsParent
    tab != Tab.CALENDAR -> BackTarget.ToCalendar
    else -> BackTarget.None
}

@Composable
internal fun PredictiveBack(
    enabled: Boolean,
    onProgress: (Float) -> Unit,
    onCommit: () -> Unit,
    onCancel: () -> Unit,
) {
    val active by rememberUpdatedState(enabled)
    if (Build.VERSION.SDK_INT >= 34) {
        PredictiveBackHandler(enabled = enabled) { events ->
            // AndroidX can still dispatch a gesture if enabled changed in the same frame.
            val started = active
            var accepted = started
            try {
                events.collect { event ->
                    accepted = accepted && active
                    if (accepted) onProgress(event.progress)
                }
            } catch (e: CancellationException) {
                if (started) onCancel()
                throw e
            }
            if (accepted && active) onCommit() else if (started) onCancel()
        }
    } else {
        BackHandler(enabled = enabled) {
            if (active) onCommit()
        }
    }
}
