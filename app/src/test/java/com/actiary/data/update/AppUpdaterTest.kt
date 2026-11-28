package com.actiary.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdaterTest {
    @Test
    fun versionComparisonHandlesSemVerAndPrefix() {
        assertTrue(AppUpdater.isNewerVersion("v1.0.2", "1.0.1"))
        assertTrue(AppUpdater.isNewerVersion("1.1.0", "1.0.9"))
        assertTrue(AppUpdater.isNewerVersion("1.10.0", "1.2.9"))
        assertTrue(AppUpdater.isNewerVersion("2.0.0", "1.9.9"))
        assertFalse(AppUpdater.isNewerVersion("v1.0.1", "1.0.1"))
        assertFalse(AppUpdater.isNewerVersion("1.0.0", "1.0.1"))
    }

    @Test
    fun parseLatestReleaseSelectsArm64ApkAsset() {
        val json = """
            {
              "tag_name": "v1.0.2",
              "draft": false,
              "prerelease": false,
              "html_url": "https://github.com/sanren1/Actiary/releases/tag/v1.0.2",
              "body": "Bug fixes and improvements",
              "assets": [
                {
                  "name": "Actiary-1.0.2-universal.apk",
                  "size": 25000000,
                  "browser_download_url": "https://example.com/universal.apk"
                },
                {
                  "name": "Actiary-1.0.2-arm64-v8a.apk",
                  "size": 17800000,
                  "browser_download_url": "https://example.com/arm64.apk"
                }
              ]
            }
        """.trimIndent()
        val parsed = AppUpdater.parseLatestReleaseJson(json)
        assertNotNull(parsed)
        assertEquals("1.0.2", parsed!!.version)
        assertEquals("https://example.com/arm64.apk", parsed.apkUrl)
        assertEquals(17800000L, parsed.apkSizeBytes)
        assertEquals("Bug fixes and improvements", parsed.notes)
    }

    @Test
    fun parseLatestReleaseSkipsDrafts() {
        val json = """{"tag_name": "v1.0.2", "draft": true, "prerelease": false}"""
        assertNull(AppUpdater.parseLatestReleaseJson(json))
    }

    @Test
    fun safeApkFileNameSanitizesUnsafeCharacters() {
        assertEquals("Actiary-1.5.1.apk", AppUpdater.safeApkFileName("1.5.1"))
        assertEquals("Actiary-1.5.1_beta_1.apk", AppUpdater.safeApkFileName("1.5.1/beta 1"))
        assertEquals("Actiary-.._.._evil.apk", AppUpdater.safeApkFileName("../../evil"))
        assertEquals("Actiary-v2.0-rc_1.apk", AppUpdater.safeApkFileName("v2.0-rc+1"))
    }

    @Test
    fun updateDownloadManagerStateTransitions() {
        UpdateDownloadManager.updateState(UpdateDownloadState.Idle)
        assertEquals(UpdateDownloadState.Idle, UpdateDownloadManager.state.value)

        UpdateDownloadManager.updateState(UpdateDownloadState.Downloading("1.5.1", 0.42f))
        assertEquals(
            UpdateDownloadState.Downloading("1.5.1", 0.42f),
            UpdateDownloadManager.state.value,
        )
        // clearIfTerminal must not clear an active download
        UpdateDownloadManager.clearIfTerminal()
        assertEquals(
            UpdateDownloadState.Downloading("1.5.1", 0.42f),
            UpdateDownloadManager.state.value,
        )

        UpdateDownloadManager.updateState(
            UpdateDownloadState.ReadyToInstall("1.5.1", "/cache/updates/Actiary-1.5.1.apk"),
        )
        assertEquals(
            UpdateDownloadState.ReadyToInstall("1.5.1", "/cache/updates/Actiary-1.5.1.apk"),
            UpdateDownloadManager.state.value,
        )
        UpdateDownloadManager.clearIfTerminal()
        assertEquals(UpdateDownloadState.Idle, UpdateDownloadManager.state.value)

        UpdateDownloadManager.updateState(
            UpdateDownloadState.Failed("1.5.1", "https://github.com/sanren1/Actiary/releases"),
        )
        assertEquals(
            UpdateDownloadState.Failed("1.5.1", "https://github.com/sanren1/Actiary/releases"),
            UpdateDownloadManager.state.value,
        )
        UpdateDownloadManager.clearIfTerminal()
        assertEquals(UpdateDownloadState.Idle, UpdateDownloadManager.state.value)
    }

    @Test
    fun shouldThrottleNotificationRespectsPercentAndInterval() {
        // 0% and 100% are never throttled when percent changes
        assertFalse(shouldThrottleNotification(lastPercent = -1, newPercent = 0, lastUpdateMs = 1000L, nowMs = 1010L))
        assertFalse(shouldThrottleNotification(lastPercent = 99, newPercent = 100, lastUpdateMs = 1000L, nowMs = 1010L))

        // Duplicate percent is always throttled, even for 0% or 100% or after interval
        assertTrue(shouldThrottleNotification(lastPercent = 0, newPercent = 0, lastUpdateMs = 1000L, nowMs = 5000L))
        assertTrue(shouldThrottleNotification(lastPercent = 50, newPercent = 50, lastUpdateMs = 1000L, nowMs = 5000L))
        assertTrue(shouldThrottleNotification(lastPercent = 100, newPercent = 100, lastUpdateMs = 1000L, nowMs = 5000L))

        // Different intermediate percent within 250ms is throttled
        assertTrue(shouldThrottleNotification(lastPercent = 10, newPercent = 11, lastUpdateMs = 1000L, nowMs = 1249L))

        // Different intermediate percent at or after 250ms is not throttled
        assertFalse(shouldThrottleNotification(lastPercent = 10, newPercent = 11, lastUpdateMs = 1000L, nowMs = 1250L))
        assertFalse(shouldThrottleNotification(lastPercent = 10, newPercent = 15, lastUpdateMs = 1000L, nowMs = 1400L))
    }
}
