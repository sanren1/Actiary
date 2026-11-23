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
}
