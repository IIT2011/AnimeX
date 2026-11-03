package com.animex.app.core.updater

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
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
    const val CURRENT_VERSION = "1.2.0" // Mobile v1.2

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdate(context: Context, isTv: Boolean = false): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(RELEASES_API_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "AnimeX-App")
                .build()

            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) {
                return@withContext UpdateCheckResult(false, CURRENT_VERSION, CURRENT_VERSION)
            }

            val body = resp.body?.string() ?: return@withContext UpdateCheckResult(false, CURRENT_VERSION, CURRENT_VERSION)
            val releases = json.decodeFromString<List<GitHubRelease>>(body)
            if (releases.isEmpty()) {
                return@withContext UpdateCheckResult(false, CURRENT_VERSION, CURRENT_VERSION)
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
                            if (isNewerVersion(verStr, CURRENT_VERSION)) {
                                return@withContext UpdateCheckResult(
                                    hasUpdate = true,
                                    currentVersion = CURRENT_VERSION,
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
                                if (isNewerVersion(verStr, CURRENT_VERSION)) {
                                    return@withContext UpdateCheckResult(
                                        hasUpdate = true,
                                        currentVersion = CURRENT_VERSION,
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
            UpdateCheckResult(false, CURRENT_VERSION, CURRENT_VERSION)
        } catch (e: Exception) {
            Log.e(TAG, "Update check failed", e)
            UpdateCheckResult(false, CURRENT_VERSION, CURRENT_VERSION)
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

    fun startDownload(context: Context, downloadUrl: String, fileName: String) {
        try {
            val req = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle("Downloading $fileName")
                setDescription("AnimeX system update in progress...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setMimeType("application/vnd.android.package-archive")
            }
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            dm?.enqueue(req)
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}
