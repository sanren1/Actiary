package com.actiary.data.update

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.actiary.MainActivity
import com.actiary.R
import com.actiary.data.appearance.AppLanguagePrefs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

sealed interface UpdateDownloadState {
    data object Idle : UpdateDownloadState
    data class Downloading(val version: String, val progress: Float?) : UpdateDownloadState
    data class ReadyToInstall(val version: String, val apkPath: String) : UpdateDownloadState
    data class Failed(val version: String, val htmlUrl: String) : UpdateDownloadState
}

object UpdateDownloadManager {
    private val _state = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    val state: StateFlow<UpdateDownloadState> = _state.asStateFlow()

    fun start(context: Context, update: ReleaseUpdate) {
        val current = _state.value
        if (current is UpdateDownloadState.Downloading && current.version == update.version) {
            return
        }
        val cached = AppUpdater.cachedApkFile(context, update.version)
        if (cached != null) {
            _state.value = UpdateDownloadState.ReadyToInstall(update.version, cached.absolutePath)
            return
        }
        val apkUrl = update.apkUrl ?: return
        _state.value = UpdateDownloadState.Downloading(update.version, 0f)
        val appContext = context.applicationContext
        val intent = Intent(appContext, UpdateDownloadService::class.java).apply {
            action = UpdateDownloadService.ACTION_START
            putExtra(UpdateDownloadService.EXTRA_VERSION, update.version)
            putExtra(UpdateDownloadService.EXTRA_APK_URL, apkUrl)
            putExtra(UpdateDownloadService.EXTRA_HTML_URL, update.htmlUrl)
        }
        ContextCompat.startForegroundService(appContext, intent)
    }

    fun cancel(context: Context) {
        if (_state.value is UpdateDownloadState.Downloading) {
            val appContext = context.applicationContext
            val intent = Intent(appContext, UpdateDownloadService::class.java).apply {
                action = UpdateDownloadService.ACTION_CANCEL
            }
            val delivered = runCatching { appContext.startService(intent) }.isSuccess
            if (!delivered) {
                _state.value = UpdateDownloadState.Idle
            }
        } else {
            _state.value = UpdateDownloadState.Idle
        }
    }

    fun clearIfTerminal() {
        val current = _state.value
        if (current is UpdateDownloadState.ReadyToInstall || current is UpdateDownloadState.Failed) {
            _state.value = UpdateDownloadState.Idle
        }
    }

    internal fun updateState(newState: UpdateDownloadState) {
        _state.value = newState
    }
}

internal fun shouldThrottleNotification(
    lastPercent: Int,
    newPercent: Int,
    lastUpdateMs: Long,
    nowMs: Long,
    minIntervalMs: Long = 250L,
): Boolean {
    if (newPercent == lastPercent) return true
    if (newPercent == 0 || newPercent == 100) return false
    return (nowMs - lastUpdateMs) < minIntervalMs
}

class UpdateDownloadService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var downloadJob: Job? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguagePrefs.wrapContext(newBase))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL -> {
                cancelAndStop()
            }
            ACTION_START -> {
                val version = intent.getStringExtra(EXTRA_VERSION).orEmpty()
                val apkUrl = intent.getStringExtra(EXTRA_APK_URL).orEmpty()
                val htmlUrl = intent.getStringExtra(EXTRA_HTML_URL).orEmpty()
                if (version.isBlank() || apkUrl.isBlank()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                ensureNotificationChannel()
                NotificationManagerCompat.from(this).cancel(NOTIF_ID_RESULT)
                ServiceCompat.startForeground(
                    this,
                    NOTIF_ID_PROGRESS,
                    buildProgressNotification(version, 0f),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                )
                startDownload(version, apkUrl, htmlUrl)
            }
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        cancelAndStop()
    }

    override fun onDestroy() {
        downloadJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startDownload(version: String, apkUrl: String, htmlUrl: String) {
        downloadJob?.cancel()
        UpdateDownloadManager.updateState(UpdateDownloadState.Downloading(version, 0f))
        downloadJob = serviceScope.launch {
            var lastPercent = 0
            var lastUpdateMs = SystemClock.elapsedRealtime()
            try {
                val result = AppUpdater.downloadApk(applicationContext, apkUrl, version) { progress ->
                    UpdateDownloadManager.updateState(UpdateDownloadState.Downloading(version, progress))
                    val newPercent = if (progress != null) {
                        (progress * 100f).roundToInt().coerceIn(0, 100)
                    } else {
                        -1
                    }
                    val now = SystemClock.elapsedRealtime()
                    if (!shouldThrottleNotification(lastPercent, newPercent, lastUpdateMs, now)) {
                        lastPercent = newPercent
                        lastUpdateMs = now
                        notifySafely(NOTIF_ID_PROGRESS, buildProgressNotification(version, progress))
                    }
                }
                result.onSuccess { file ->
                    UpdateDownloadManager.updateState(
                        UpdateDownloadState.ReadyToInstall(version, file.absolutePath),
                    )
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    notifySafely(NOTIF_ID_RESULT, buildReadyNotification(version, file))
                    stopSelf()
                }.onFailure {
                    UpdateDownloadManager.updateState(UpdateDownloadState.Failed(version, htmlUrl))
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    notifySafely(NOTIF_ID_RESULT, buildFailedNotification())
                    stopSelf()
                }
            } catch (e: CancellationException) {
                UpdateDownloadManager.updateState(UpdateDownloadState.Idle)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                throw e
            }
        }
    }

    private fun cancelAndStop() {
        downloadJob?.cancel()
        downloadJob = null
        UpdateDownloadManager.updateState(UpdateDownloadState.Idle)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun ensureNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.update_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
    }

    private fun buildProgressNotification(version: String, progress: Float?): Notification {
        val percent = progress?.let { (it * 100f).roundToInt().coerceIn(0, 100) }
        val text = if (percent != null) {
            getString(R.string.update_downloading_percent, percent)
        } else {
            getString(R.string.update_downloading)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(getString(R.string.update_notification_title, version))
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppPendingIntent())
            .apply {
                if (percent != null) {
                    setProgress(100, percent, false)
                } else {
                    setProgress(0, 0, true)
                }
            }
            .addAction(0, getString(R.string.record_cancel), cancelPendingIntent())
            .build()
    }

    private fun buildReadyNotification(version: String, apkFile: File): Notification {
        val installIntent = AppUpdater.createInstallIntent(this, apkFile)
        val contentIntent = if (installIntent != null) {
            PendingIntent.getActivity(
                this,
                REQUEST_CODE_INSTALL,
                installIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        } else {
            openAppPendingIntent()
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(getString(R.string.update_notification_ready_title, version))
            .setContentText(getString(R.string.update_notification_ready_text))
            .setAutoCancel(true)
            .setOngoing(false)
            .setContentIntent(contentIntent)
            .build()
    }

    private fun buildFailedNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(getString(R.string.update_notification_failed_title))
            .setContentText(getString(R.string.update_notification_failed_text))
            .setAutoCancel(true)
            .setOngoing(false)
            .setContentIntent(openAppPendingIntent())
            .build()
    }

    private fun openAppPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            REQUEST_CODE_OPEN_APP,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun cancelPendingIntent(): PendingIntent {
        val intent = Intent(this, UpdateDownloadService::class.java).apply {
            action = ACTION_CANCEL
        }
        return PendingIntent.getService(
            this,
            REQUEST_CODE_CANCEL,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun notifySafely(id: Int, notification: Notification) {
        if (!canPostNotifications()) return
        try {
            NotificationManagerCompat.from(this).notify(id, notification)
        } catch (_: SecurityException) {
            // Ignore safely if notification permission is revoked.
        }
    }

    companion object {
        const val ACTION_START = "com.actiary.action.UPDATE_DOWNLOAD_START"
        const val ACTION_CANCEL = "com.actiary.action.UPDATE_DOWNLOAD_CANCEL"
        const val EXTRA_VERSION = "extra_version"
        const val EXTRA_APK_URL = "extra_apk_url"
        const val EXTRA_HTML_URL = "extra_html_url"

        private const val CHANNEL_ID = "actiary_updates"
        private const val NOTIF_ID_PROGRESS = 2001
        private const val NOTIF_ID_RESULT = 2002
        private const val REQUEST_CODE_OPEN_APP = 2010
        private const val REQUEST_CODE_CANCEL = 2011
        private const val REQUEST_CODE_INSTALL = 2012
    }
}
