package com.actiary.data.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseUpdate(
    val version: String,
    val tagName: String,
    val notes: String,
    val htmlUrl: String,
    val apkUrl: String?,
    val apkSizeBytes: Long,
)

object UpdatePrefs {
    private const val PREFS_NAME = "actiary_update_prefs"
    private const val KEY_IGNORED_VERSION = "ignored_version"

    fun getIgnoredVersion(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_IGNORED_VERSION, null)

    fun setIgnoredVersion(context: Context, version: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .apply {
                if (version == null) remove(KEY_IGNORED_VERSION) else putString(KEY_IGNORED_VERSION, version)
            }
            .apply()
    }
}

object AppUpdater {
    private const val LATEST_RELEASE_API = "https://api.github.com/repos/sanren1/Actiary/releases/latest"
    private const val FILE_PROVIDER_AUTHORITY = "com.actiary.fileprovider"
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 15_000
    private const val DOWNLOAD_READ_TIMEOUT_MS = 30_000

    internal fun normalizeVersion(raw: String): String =
        raw.trim().removePrefix("v").removePrefix("V").substringBefore('-').substringBefore('+')

    internal fun isNewerVersion(candidate: String, current: String): Boolean {
        val candParts = normalizeVersion(candidate).split('.').mapNotNull { it.toIntOrNull() }
        val currParts = normalizeVersion(current).split('.').mapNotNull { it.toIntOrNull() }
        if (candParts.isEmpty() || currParts.isEmpty()) return false
        val maxLen = maxOf(candParts.size, currParts.size)
        for (i in 0 until maxLen) {
            val c = candParts.getOrElse(i) { 0 }
            val u = currParts.getOrElse(i) { 0 }
            if (c != u) return c > u
        }
        return false
    }

    internal fun parseLatestReleaseJson(jsonText: String): ReleaseUpdate? {
        val root = JSONObject(jsonText)
        if (root.optBoolean("draft", false) || root.optBoolean("prerelease", false)) return null
        val tagName = root.optString("tag_name", "").trim()
        val version = normalizeVersion(tagName)
        if (version.isEmpty()) return null
        val htmlUrl = root.optString("html_url", "https://github.com/sanren1/Actiary/releases").trim()
        val notes = root.optString("body", "").trim()
        val assets = root.optJSONArray("assets")
        var bestUrl: String? = null
        var bestSize = 0L
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val name = asset.optString("name", "")
                if (!name.endsWith(".apk", ignoreCase = true)) continue
                val url = asset.optString("browser_download_url", "").takeIf { it.isNotBlank() } ?: continue
                val size = asset.optLong("size", 0L)
                if (bestUrl == null || name.contains("arm64-v8a", ignoreCase = true)) {
                    bestUrl = url
                    bestSize = size
                    if (name.contains("arm64-v8a", ignoreCase = true)) break
                }
            }
        }
        return ReleaseUpdate(
            version = version,
            tagName = tagName,
            notes = notes,
            htmlUrl = htmlUrl,
            apkUrl = bestUrl,
            apkSizeBytes = bestSize,
        )
    }

    suspend fun checkForUpdate(currentVersion: String): Result<ReleaseUpdate?> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Actiary-Android/$currentVersion")
            }
            try {
                val code = conn.responseCode
                if (code !in 200..299) {
                    throw IOException("GitHub API HTTP $code")
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val latest = parseLatestReleaseJson(body) ?: return@runCatching null
                if (isNewerVersion(latest.version, currentVersion)) latest else null
            } finally {
                conn.disconnect()
            }
        }
    }

    internal fun safeApkFileName(version: String): String {
        val safeVersion = version.replace(Regex("[^0-9A-Za-z._-]"), "_")
        return "Actiary-$safeVersion.apk"
    }

    fun cachedApkFile(context: Context, version: String): File? {
        val file = File(File(context.cacheDir, "updates"), safeApkFileName(version))
        return file.takeIf { it.isFile && it.length() > 0L }
    }

    suspend fun downloadApk(
        context: Context,
        apkUrl: String,
        version: String,
        onProgress: (Float?) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val fileName = safeApkFileName(version)
        val partFile = File(dir, "$fileName.part")
        val finalFile = File(dir, fileName)
        try {
            currentCoroutineContext().ensureActive()
            dir.listFiles()?.forEach { file ->
                if (file.name != partFile.name && file.name != finalFile.name) {
                    file.delete()
                }
            }
            if (partFile.exists()) partFile.delete()

            var urlStr = apkUrl
            var redirects = 0
            var conn: HttpURLConnection
            while (true) {
                currentCoroutineContext().ensureActive()
                conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = DOWNLOAD_READ_TIMEOUT_MS
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "Actiary-Android")
                }
                val code = conn.responseCode
                if (code in 300..399 && redirects < 6) {
                    val location = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (location.isNullOrBlank()) throw IOException("Redirect without Location")
                    urlStr = location
                    redirects++
                } else {
                    break
                }
            }
            try {
                val code = conn.responseCode
                if (code !in 200..299) throw IOException("Download HTTP $code")
                val total = conn.contentLengthLong
                onProgress(if (total > 0L) 0f else null)
                conn.inputStream.use { input ->
                    partFile.outputStream().use { output ->
                        val buffer = ByteArray(16 * 1024)
                        var copied = 0L
                        var lastReported = -1
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            copied += read
                            if (total > 0L) {
                                val pct = ((copied * 100L) / total).toInt().coerceIn(0, 100)
                                if (pct != lastReported) {
                                    lastReported = pct
                                    onProgress(pct / 100f)
                                }
                            }
                        }
                    }
                }
                currentCoroutineContext().ensureActive()
                if (finalFile.exists()) finalFile.delete()
                if (!partFile.renameTo(finalFile)) {
                    partFile.copyTo(finalFile, overwrite = true)
                    partFile.delete()
                }
                onProgress(1f)
                Result.success(finalFile)
            } finally {
                conn.disconnect()
            }
        } catch (e: CancellationException) {
            partFile.delete()
            throw e
        } catch (t: Throwable) {
            partFile.delete()
            Result.failure(t)
        }
    }

    fun createInstallIntent(context: Context, apkFile: File): Intent? = runCatching {
        val uri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, apkFile)
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }.getOrNull()

    fun installApk(context: Context, apkFile: File): Boolean = runCatching {
        val intent = createInstallIntent(context, apkFile) ?: return@runCatching false
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}
