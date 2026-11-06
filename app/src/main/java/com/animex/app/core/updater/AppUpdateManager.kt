package com.animex.app.core.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

@Serializable
data class GitHubReleaseAsset(
    val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
    val size: Long = 0L
)

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val body: String? = null,
    @SerialName("published_at") val publishedAt: String? = null,
    val assets: List<GitHubReleaseAsset> = emptyList()
)

data class UpdateCheckResult(
    val hasUpdate: Boolean,
    val currentVersion: String,
    val latestVersion: String,
    val downloadUrl: String? = null,
    val fileName: String? = null,
    val releaseTitle: String? = null,
    val changelog: String? = null
)

object AppUpdateManager {
    private const val TAG = "AppUpdateManager"
    private const val RELEASES_API_URL = "https://api.github.com/repos/IIT2011/AnimeX/releases"
    const val CURRENT_VERSION = "1.2.0"

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun getInstalledVersion(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.2.0"
        } catch (_: Exception) {
            "1.2.0"
        }
    }

    suspend fun checkForUpdate(context: Context, isTv: Boolean = false): UpdateCheckResult = withContext(Dispatchers.IO) {
        val currentVer = getInstalledVersion(context)
        try {
            val req = Request.Builder()
                .url(RELEASES_API_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "AnimeX-App")
                .build()

            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) {
                Log.w(TAG, "GitHub API update check returned HTTP ${resp.code}")
                return@withContext UpdateCheckResult(false, currentVer, currentVer)
            }

            val body = resp.body?.string() ?: return@withContext UpdateCheckResult(false, currentVer, currentVer)
            val releases = json.decodeFromString<List<GitHubRelease>>(body)
            if (releases.isEmpty()) {
                return@withContext UpdateCheckResult(false, currentVer, currentVer)
            }

            val mobileRegex = Regex("""(?i)AnimeX[-_]V?(\d+(?:\.\d+)*)\.apk$""")
            val tvRegex = Regex("""(?i)AnimeX[-_]tv[-_]v?(\d+(?:\.\d+)*)\.apk$""")

            for (rel in releases) {
                for (asset in rel.assets) {
                    val assetName = asset.name
                    if (isTv) {
                        val match = tvRegex.find(assetName)
                        if (match != null) {
                            val verStr = match.groupValues[1]
                            if (isNewerVersion(verStr, currentVer)) {
                                return@withContext UpdateCheckResult(
                                    hasUpdate = true,
                                    currentVersion = currentVer,
                                    latestVersion = verStr,
                                    downloadUrl = asset.browserDownloadUrl,
                                    fileName = asset.name,
                                    releaseTitle = rel.name ?: "AnimeX TV v$verStr",
                                    changelog = rel.body
                                )
                            }
                        }
                    } else {
                        if (!assetName.contains("tv", ignoreCase = true)) {
                            val match = mobileRegex.find(assetName)
                            if (match != null) {
                                val verStr = match.groupValues[1]
                                if (isNewerVersion(verStr, currentVer)) {
                                    return@withContext UpdateCheckResult(
                                        hasUpdate = true,
                                        currentVersion = currentVer,
                                        latestVersion = verStr,
                                        downloadUrl = asset.browserDownloadUrl,
                                        fileName = asset.name,
                                        releaseTitle = rel.name ?: "AnimeX v$verStr",
                                        changelog = rel.body
                                    )
                                }
                            }
                        }
                    }
                }
            }
            UpdateCheckResult(false, currentVer, currentVer)
        } catch (e: Exception) {
            Log.e(TAG, "Update check failed", e)
            UpdateCheckResult(false, currentVer, currentVer)
        }
    }

    fun isNewerVersion(remote: String, current: String): Boolean {
        val r = remote.split('.').mapNotNull { it.toIntOrNull() }
        val c = current.split('.').mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(r.size, c.size)
        for (i in 0 until maxLen) {
            val rVal = r.getOrElse(i) { 0 }
            val cVal = c.getOrElse(i) { 0 }
            if (rVal > cVal) return true
            if (rVal < cVal) return false
        }
        return false
    }

    suspend fun downloadAndInstallApk(
        context: Context,
        downloadUrl: String,
        fileName: String,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(downloadUrl).build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext false

            val body = resp.body ?: return@withContext false
            val totalBytes = body.contentLength().coerceAtLeast(1L)
            val targetDir = context.externalCacheDir ?: context.cacheDir
            val targetFile = File(targetDir, fileName)

            var downloadedBytes = 0L
            FileOutputStream(targetFile).use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        val progress = (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                        onProgress(progress)
                    }
                    output.flush()
                }
            }

            installApk(context, targetFile)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading and installing APK", e)
            false
        }
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(settingsIntent)
                    Toast.makeText(
                        context,
                        "Please allow 'Install unknown apps' for AnimeX to install this update.",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch installer intent", e)
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(apkFile.absolutePath)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(browserIntent)
            } catch (_: Exception) {}
        }
    }
}
