package com.animex.tv.core.session

import android.content.Context
import com.animex.tv.core.model.AnimeDto
import com.animex.tv.core.model.WatchHistoryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object UserSessionRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private const val PREFS_NAME = "animex_tv_prefs"
    private const val KEY_WATCHLIST = "watchlist"
    private const val KEY_HISTORY = "history"
    private const val KEY_USERNAME = "username"

    private val _watchlist = MutableStateFlow<List<AnimeDto>>(emptyList())
    val watchlist: StateFlow<List<AnimeDto>> = _watchlist.asStateFlow()

    private val _continueWatching = MutableStateFlow<List<WatchHistoryEntry>>(emptyList())
    val continueWatching: StateFlow<List<WatchHistoryEntry>> = _continueWatching.asStateFlow()

    private val _username = MutableStateFlow("CYBER_OPERATIVE")
    val username: StateFlow<String> = _username.asStateFlow()

    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _username.value = prefs.getString(KEY_USERNAME, "CYBER_OPERATIVE") ?: "CYBER_OPERATIVE"

        prefs.getString(KEY_WATCHLIST, null)?.let { raw ->
            try {
                _watchlist.value = json.decodeFromString(raw)
            } catch (_: Exception) {}
        }

        prefs.getString(KEY_HISTORY, null)?.let { raw ->
            try {
                _continueWatching.value = json.decodeFromString(raw)
            } catch (_: Exception) {}
        }
    }

    fun isAnimeInWatchlist(animeId: Long): Boolean {
        return _watchlist.value.any { it.id == animeId }
    }

    fun toggleWatchlist(context: Context, anime: AnimeDto) {
        val current = _watchlist.value.toMutableList()
        val index = current.indexOfFirst { it.id == anime.id }
        if (index >= 0) {
            current.removeAt(index)
        } else {
            current.add(0, anime)
        }
        _watchlist.value = current
        saveWatchlist(context)
    }

    fun recordHistory(context: Context, entry: WatchHistoryEntry) {
        val current = _continueWatching.value.toMutableList()
        current.removeAll { it.animeId == entry.animeId }
        current.add(0, entry)
        _continueWatching.value = current.take(30)
        saveHistory(context)
    }

    private fun saveWatchlist(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            val raw = json.encodeToString(_watchlist.value)
            prefs.edit().putString(KEY_WATCHLIST, raw).apply()
        } catch (_: Exception) {}
    }

    private fun saveHistory(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            val raw = json.encodeToString(_continueWatching.value)
            prefs.edit().putString(KEY_HISTORY, raw).apply()
        } catch (_: Exception) {}
    }
}
