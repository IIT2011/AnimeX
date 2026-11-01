package com.animex.app.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animex.app.core.network.NetworkClient
import com.animex.app.core.network.model.AnimeDto
import com.animex.app.core.network.model.EpisodeItemDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Success(
        val anime: AnimeDto,
        val episodes: List<EpisodeItemDto>
    ) : DetailUiState
    data class Error(val message: String) : DetailUiState
}

class DetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun loadAnime(animeId: Long) {
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            try {
                val anime = NetworkClient.api.getAnimeDetail(animeId)
                val epResponse = NetworkClient.api.getEpisodes(animeId)
                _uiState.value = DetailUiState.Success(
                    anime = anime,
                    episodes = epResponse.data
                )
            } catch (e: Exception) {
                _uiState.value = DetailUiState.Error(e.localizedMessage ?: "Failed to load telemetry")
            }
        }
    }
}
