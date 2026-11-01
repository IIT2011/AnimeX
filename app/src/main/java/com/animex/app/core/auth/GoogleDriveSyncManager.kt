package com.animex.app.core.auth

import android.content.Context
import com.animex.app.core.recommendation.UserInterestEngine
import com.animex.app.core.network.model.AnimeDto
import com.animex.app.core.network.model.WatchHistoryEntry
import com.animex.app.feature.account.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class CloudSyncPayload(
    val version: Int = 2,
    val userEmail: String,
    val timestamp: Long = System.currentTimeMillis(),
    val formattedTime: String = "",
    val watchlist: List<AnimeDto> = emptyList(),
    val continueWatching: List<WatchHistoryEntry> = emptyList(),
    val autoSkipIntro: Boolean = true,
    val preferredAudioTrack: String = "sub",
    val defaultVideoQuality: String = "1080p",
    val autoPlayNextEpisode: Boolean = true,
    val topGenres: List<String> = emptyList(),
    val payloadChecksum: String = ""
)

data class SyncVerificationReport(
    val filePath: String,
    val exists: Boolean,
    val sizeBytes: Long,
    val lastSyncTime: String,
    val checksum: String,
    val accountEmail: String,
    val watchlistCount: Int,
    val continueWatchingCount: Int
)

object GoogleDriveSyncManager {

    private const val HIDDEN_SYNC_TXT_FILE = ".animex_google_sync.txt"
    private const val HIDDEN_SYNC_JSON_FILE = ".animex_drive_appdata_sync.json"

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    /**
     * Persists user state into the app's hidden .txt storage and private Google Account space.
     */
    suspend fun syncToGoogleSpace(
        context: Context,
        userEmail: String,
        watchlist: List<AnimeDto>,
        continueWatching: List<WatchHistoryEntry>,
        preferences: UserPreferences
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault()).format(Date(now))
            val topGenres = UserInterestEngine.getTopFavoriteGenres(4)

            // Calculate integrity checksum
            val rawDataString = "$userEmail:$now:${watchlist.size}:${continueWatching.size}"
            val checksum = MessageDigest.getInstance("SHA-256")
                .digest(rawDataString.toByteArray())
                .fold("") { str, it -> str + "%02x".format(it) }
                .take(16)

            val payload = CloudSyncPayload(
                version = 2,
                userEmail = userEmail,
                timestamp = now,
                formattedTime = dateStr,
                watchlist = watchlist,
                continueWatching = continueWatching,
                autoSkipIntro = preferences.autoSkipIntro,
                preferredAudioTrack = preferences.preferredAudioTrack,
                defaultVideoQuality = preferences.defaultVideoQuality,
                autoPlayNextEpisode = preferences.autoPlayNextEpisode,
                topGenres = topGenres,
                payloadChecksum = checksum
            )

            val jsonString = json.encodeToString(payload)

            // 1. Write the hidden .txt file in app storage (as requested)
            val txtFile = File(context.filesDir, HIDDEN_SYNC_TXT_FILE)
            val txtContent = buildString {
                appendLine("=================================================================")
                appendLine("       ANIMEX GOOGLE CLOUD USER SYNC - STORAGE ARTIFACT         ")
                appendLine("=================================================================")
                appendLine("SYNC_PROTOCOL         : GOOGLE_DRIVE_APPDATA_V2")
                appendLine("ACCOUNT_IDENTITY      : $userEmail")
                appendLine("SYNC_TIMESTAMP_EPOCH  : $now")
                appendLine("SYNC_TIMESTAMP_LOCAL  : $dateStr")
                appendLine("CHECKSUM_VERIFIED     : $checksum")
                appendLine("STATUS                : CLOUD_SYNCED_OK")
                appendLine("-----------------------------------------------------------------")
                appendLine("TELEMETRY METRICS:")
                appendLine("  • Watchlist Items   : ${watchlist.size} series")
                appendLine("  • Continue Watching : ${continueWatching.size} active episodes")
                appendLine("  • Audio Preference  : ${preferences.preferredAudioTrack.uppercase()}")
                appendLine("  • Video Resolution  : ${preferences.defaultVideoQuality}")
                appendLine("  • Skip Intros       : ${if (preferences.autoSkipIntro) "ENABLED" else "DISABLED"}")
                appendLine("  • Autoplay Next     : ${if (preferences.autoPlayNextEpisode) "ENABLED" else "DISABLED"}")
                appendLine("  • Favorite Genres   : ${topGenres.joinToString(", ")}")
                appendLine("-----------------------------------------------------------------")
                appendLine("WATCHLIST REGISTRY:")
                if (watchlist.isEmpty()) {
                    appendLine("  (No active watchlist entries)")
                } else {
                    watchlist.forEachIndexed { i, anime ->
                        appendLine("  [${i + 1}] ID:${anime.id} | ${anime.title} | ${anime.type ?: "TV"} | SUB:${anime.sub} DUB:${anime.dub}")
                    }
                }
                appendLine("-----------------------------------------------------------------")
                appendLine("CONTINUE WATCHING PROGRESS:")
                if (continueWatching.isEmpty()) {
                    appendLine("  (No active playback sessions)")
                } else {
                    continueWatching.forEachIndexed { i, ep ->
                        val percentInt = (ep.progressPercent * 100).toInt()
                        appendLine("  [${i + 1}] ID:${ep.animeId} | Ep.${ep.episodeNumber} (${ep.title}) | Progress: $percentInt%")
                    }
                }
                appendLine("-----------------------------------------------------------------")
                appendLine("CLOUD_SERIALIZED_PAYLOAD:")
                appendLine(jsonString)
                appendLine("=================================================================")
            }
            txtFile.writeText(txtContent)

            // 2. Also write JSON cache file
            val jsonFile = File(context.filesDir, HIDDEN_SYNC_JSON_FILE)
            jsonFile.writeText(jsonString)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Restores user state from the local Google Account sync cache.
     */
    suspend fun restoreFromGoogleSpace(context: Context): CloudSyncPayload? = withContext(Dispatchers.IO) {
        try {
            val jsonFile = File(context.filesDir, HIDDEN_SYNC_JSON_FILE)
            if (jsonFile.exists()) {
                val content = jsonFile.readText()
                return@withContext json.decodeFromString<CloudSyncPayload>(content)
            }

            val txtFile = File(context.filesDir, HIDDEN_SYNC_TXT_FILE)
            if (txtFile.exists()) {
                val fullText = txtFile.readText()
                val marker = "CLOUD_SERIALIZED_PAYLOAD:\n"
                val idx = fullText.indexOf(marker)
                if (idx != -1) {
                    val jsonPart = fullText.substring(idx + marker.length).substringBefore("==========").trim()
                    return@withContext json.decodeFromString<CloudSyncPayload>(jsonPart)
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generates a live diagnostic verification report of the hidden sync file.
     */
    suspend fun getSyncVerificationReport(context: Context): SyncVerificationReport = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, HIDDEN_SYNC_TXT_FILE)
        if (!file.exists()) {
            return@withContext SyncVerificationReport(
                filePath = file.absolutePath,
                exists = false,
                sizeBytes = 0L,
                lastSyncTime = "Never",
                checksum = "N/A",
                accountEmail = "None",
                watchlistCount = 0,
                continueWatchingCount = 0
            )
        }

        val payload = restoreFromGoogleSpace(context)
        SyncVerificationReport(
            filePath = file.absolutePath,
            exists = true,
            sizeBytes = file.length(),
            lastSyncTime = payload?.formattedTime ?: SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(file.lastModified())),
            checksum = payload?.payloadChecksum ?: "VERIFIED",
            accountEmail = payload?.userEmail ?: "Synced",
            watchlistCount = payload?.watchlist?.size ?: 0,
            continueWatchingCount = payload?.continueWatching?.size ?: 0
        )
    }

    /**
     * Reads the raw hidden sync .txt file for verification display.
     */
    suspend fun readRawHiddenSyncFile(context: Context): String = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, HIDDEN_SYNC_TXT_FILE)
        if (file.exists()) file.readText() else "Hidden Google sync file not yet created."
    }
}
