package com.animex.app.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animex.app.core.network.NetworkClient
import com.animex.app.core.network.model.PlayRequest
import com.animex.app.core.network.model.PlayResponseDto
import com.animex.app.core.network.model.PlaybackSourceDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PlayerUiState {
    data object Loading : PlayerUiState
    data class Ready(
        val playData: PlayResponseDto,
        val activeSource: PlaybackSourceDto,
        val currentTrack: String = "sub",
        val initialSeekMs: Long = 0L
    ) : PlayerUiState
    data class Error(val message: String) : PlayerUiState
}

class PlayerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var activeEpisodeId: Long = 0L
    private var activeTrack: String = "sub"

    fun loadEpisodeStream(episodeId: Long, track: String = "sub", initialSeekMs: Long = 0L) {
        activeEpisodeId = episodeId
        activeTrack = track
        viewModelScope.launch {
            _uiState.value = PlayerUiState.Loading
            try {
                val response = NetworkClient.api.playEpisode(episodeId, PlayRequest(track = track))
                val hlsSource = response.sources.firstOrNull { it.kind == "hls" }
                    ?: response.sources.firstOrNull()

                if (hlsSource?.url != null) {
                    _uiState.value = PlayerUiState.Ready(
                        playData = response,
                        activeSource = hlsSource,
                        currentTrack = track,
                        initialSeekMs = initialSeekMs
                    )
                } else {
                    _uiState.value = PlayerUiState.Error("No valid HLS video stream found")
                }
            } catch (e: Exception) {
                _uiState.value = PlayerUiState.Error(e.localizedMessage ?: "Failed to resolve stream link")
            }
        }
    }

    fun switchAudioTrack(newTrack: String, currentPositionMs: Long) {
        if (newTrack == activeTrack) return
        loadEpisodeStream(activeEpisodeId, newTrack, currentPositionMs)
    }
}
