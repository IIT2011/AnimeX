package com.animex.app.feature.account

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.animex.app.core.auth.GoogleAuthManager
import com.animex.app.core.network.model.AnimeDto
import com.animex.app.core.network.model.UserGoogleAccount
import com.animex.app.core.network.model.WatchHistoryEntry
import com.animex.app.core.session.UserSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
    val autoSkipIntro: Boolean = true,
    val preferredAudioTrack: String = "sub", // "sub" or "dub"
    val defaultVideoQuality: String = "1080p",
    val autoPlayNextEpisode: Boolean = true
)

data class AccountUiState(
    val isLoggedIn: Boolean = false,
    val googleAccount: UserGoogleAccount? = null,
    val isSyncing: Boolean = false,
    val lastSyncTime: String = "Ready",
    val detectedDeviceAccounts: List<String> = emptyList(),
    val watchlist: List<AnimeDto> = emptyList(),
    val continueWatching: List<WatchHistoryEntry> = emptyList(),
    val preferences: UserPreferences = UserPreferences()
)

class AccountViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext

    private val _deviceAccounts = MutableStateFlow<List<String>>(emptyList())

    private data class SessionBundle(
        val account: UserGoogleAccount?,
        val watchlist: List<AnimeDto>,
        val continueWatching: List<WatchHistoryEntry>,
        val prefs: UserPreferences,
        val status: String
    )

    val uiState: StateFlow<AccountUiState> = combine(
        combine(
            UserSessionRepository.googleAccount,
            UserSessionRepository.watchlist,
            UserSessionRepository.continueWatching,
            UserSessionRepository.preferences,
            UserSessionRepository.syncStatus
        ) { account, watchlist, continueWatching, prefs, status ->
            SessionBundle(account, watchlist, continueWatching, prefs, status)
        },
        _deviceAccounts
    ) { session, devAccounts ->
        AccountUiState(
            isLoggedIn = session.account != null,
            googleAccount = session.account,
            isSyncing = session.status == "Syncing...",
            lastSyncTime = session.status,
            detectedDeviceAccounts = devAccounts,
            watchlist = session.watchlist,
            continueWatching = session.continueWatching,
            preferences = session.prefs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AccountUiState()
    )

    init {
        refreshAccounts()
    }

    fun refreshAccounts() {
        viewModelScope.launch {
            _deviceAccounts.value = GoogleAuthManager.getAvailableDeviceGoogleAccounts(context)
        }
    }

    fun onGoogleAccountSelected(account: UserGoogleAccount) {
        UserSessionRepository.setGoogleAccount(context, account)
        refreshAccounts()
    }

    fun onDirectEmailSignIn(email: String) {
        val account = GoogleAuthManager.signInWithEmail(context, email)
        UserSessionRepository.setGoogleAccount(context, account)
        refreshAccounts()
    }

    fun logout() {
        UserSessionRepository.signOut(context)
        refreshAccounts()
    }

    fun triggerCloudSync() {
        viewModelScope.launch {
            UserSessionRepository.forceSyncNow(context)
        }
    }

    fun updatePreferences(
        autoSkip: Boolean = uiState.value.preferences.autoSkipIntro,
        audioTrack: String = uiState.value.preferences.preferredAudioTrack,
        quality: String = uiState.value.preferences.defaultVideoQuality,
        autoPlayNext: Boolean = uiState.value.preferences.autoPlayNextEpisode
    ) {
        val updated = UserPreferences(
            autoSkipIntro = autoSkip,
            preferredAudioTrack = audioTrack,
            defaultVideoQuality = quality,
            autoPlayNextEpisode = autoPlayNext
        )
        UserSessionRepository.updatePreferences(context, updated)
    }

    fun removeFromWatchlist(animeId: Long) {
        UserSessionRepository.removeFromWatchlist(context, animeId)
    }
}
