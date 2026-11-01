package com.animex.app.core.session

import android.content.Context
import com.animex.app.core.recommendation.UserInterestEngine
import com.animex.app.core.auth.GoogleAuthManager
import com.animex.app.core.auth.GoogleDriveSyncManager
import com.animex.app.core.network.model.AnimeDto
import com.animex.app.core.network.model.UserGoogleAccount
import com.animex.app.core.network.model.WatchHistoryEntry
import com.animex.app.feature.account.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object UserSessionRepository {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _watchlist = MutableStateFlow<List<AnimeDto>>(emptyList())
    val watchlist: StateFlow<List<AnimeDto>> = _watchlist.asStateFlow()

    private val _continueWatching = MutableStateFlow<List<WatchHistoryEntry>>(emptyList())
    val continueWatching: StateFlow<List<WatchHistoryEntry>> = _continueWatching.asStateFlow()

    private val _preferences = MutableStateFlow(UserPreferences())
    val preferences: StateFlow<UserPreferences> = _preferences.asStateFlow()

    private val _googleAccount = MutableStateFlow<UserGoogleAccount?>(null)
    val googleAccount: StateFlow<UserGoogleAccount?> = _googleAccount.asStateFlow()

    private val _syncStatus = MutableStateFlow("Ready")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        isInitialized = true

        // Initialize interest engine
        UserInterestEngine.initialize(context)

        scope.launch {
            // Restore Google Account
            val account = GoogleAuthManager.loadSession(context)
            _googleAccount.value = account

            // Restore from Google AppData space & hidden .txt sync cache
            val cloudData = GoogleDriveSyncManager.restoreFromGoogleSpace(context)
            if (cloudData != null) {
                _watchlist.value = cloudData.watchlist
                _continueWatching.value = cloudData.continueWatching
                _preferences.value = UserPreferences(
                    autoSkipIntro = cloudData.autoSkipIntro,
                    preferredAudioTrack = cloudData.preferredAudioTrack,
                    defaultVideoQuality = cloudData.defaultVideoQuality,
                    autoPlayNextEpisode = cloudData.autoPlayNextEpisode
                )
                _syncStatus.value = "Synced with Google Cloud"
            } else {
                _syncStatus.value = if (account != null) "Cloud Storage Connected" else "Guest Mode"
            }
        }
    }

    fun isAnimeInWatchlist(animeId: Long): Boolean {
        return _watchlist.value.any { it.id == animeId }
    }

    fun toggleWatchlist(context: Context, anime: AnimeDto) {
        val currentList = _watchlist.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == anime.id }
        if (index >= 0) {
            currentList.removeAt(index)
        } else {
            currentList.add(0, anime)
            // Record user interest signal
            UserInterestEngine.recordWatchlistAdd(context, anime)
        }
        _watchlist.value = currentList
        triggerAutoSync(context)
    }

    fun removeFromWatchlist(context: Context, animeId: Long) {
        _watchlist.value = _watchlist.value.filter { it.id != animeId }
        triggerAutoSync(context)
    }

    fun recordPlaybackProgress(
        context: Context,
        animeId: Long,
        title: String,
        poster: String?,
        episodeId: Long,
        episodeNumber: Int,
        currentPosMs: Long,
        durationMs: Long
    ) {
        if (durationMs <= 0) return
        val percent = (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0.01f, 1f)

        val list = _continueWatching.value.toMutableList()
        list.removeAll { it.animeId == animeId }
        list.add(
            0,
            WatchHistoryEntry(
                animeId = animeId,
                title = title,
                poster = poster,
                episodeId = episodeId,
                episodeNumber = episodeNumber,
                progressPercent = percent
            )
        )
        _continueWatching.value = list

        // Record playback watch progress for recommendations
        UserInterestEngine.recordPlayback(
            context = context,
            animeId = animeId,
            title = title,
            genres = null,
            progressPercent = percent
        )

        triggerAutoSync(context)
    }

    fun setGoogleAccount(context: Context, account: UserGoogleAccount) {
        _googleAccount.value = account
        GoogleAuthManager.saveSession(context, account)
        triggerAutoSync(context)
    }

    fun signOut(context: Context) {
        _googleAccount.value = null
        GoogleAuthManager.clearSession(context)
        _syncStatus.value = "Signed Out"
    }

    fun updatePreferences(context: Context, prefs: UserPreferences) {
        _preferences.value = prefs
        triggerAutoSync(context)
    }

    suspend fun forceSyncNow(context: Context): Boolean = withContext(Dispatchers.IO) {
        _syncStatus.value = "Syncing..."
        val email = _googleAccount.value?.email ?: "local_guest_storage"
        val success = GoogleDriveSyncManager.syncToGoogleSpace(
            context = context,
            userEmail = email,
            watchlist = _watchlist.value,
            continueWatching = _continueWatching.value,
            preferences = _preferences.value
        )
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _syncStatus.value = if (success) "Synced at $time" else "Sync Error"
        success
    }

    private fun triggerAutoSync(context: Context) {
        scope.launch {
            val email = _googleAccount.value?.email ?: "local_guest_storage"
            _syncStatus.value = "Syncing..."
            val success = GoogleDriveSyncManager.syncToGoogleSpace(
                context = context,
                userEmail = email,
                watchlist = _watchlist.value,
                continueWatching = _continueWatching.value,
                preferences = _preferences.value
            )
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            _syncStatus.value = if (success) "Auto-synced at $time" else "Sync error"
        }
    }
}
