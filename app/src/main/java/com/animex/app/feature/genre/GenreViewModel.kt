package com.animex.app.feature.genre

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animex.app.core.network.NetworkClient
import com.animex.app.core.network.model.AnimeDto
import com.animex.app.core.network.model.GenreItemDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface GenreUiState {
    data object Loading : GenreUiState
    data class Success(
        val genres: List<GenreItemDto>,
        val selectedGenre: GenreItemDto? = null,
        val animeList: List<AnimeDto> = emptyList(),
        val isSearchingAnime: Boolean = false
    ) : GenreUiState
    data class Error(val message: String) : GenreUiState
}

class GenreViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<GenreUiState>(GenreUiState.Loading)
    val uiState: StateFlow<GenreUiState> = _uiState.asStateFlow()

    init {
        loadGenres()
    }

    fun loadGenres() {
        viewModelScope.launch {
            _uiState.value = GenreUiState.Loading
            try {
                val genres = NetworkClient.api.getGenres()
                _uiState.value = GenreUiState.Success(genres = genres)
            } catch (e: Exception) {
                _uiState.value = GenreUiState.Error(e.localizedMessage ?: "Failed to load genres")
            }
        }
    }

    fun selectGenre(genre: GenreItemDto) {
        val current = _uiState.value
        if (current is GenreUiState.Success) {
            _uiState.value = current.copy(
                selectedGenre = genre,
                isSearchingAnime = true
            )
            viewModelScope.launch {
                try {
                    // Query anime matching the genre keyword
                    val results = NetworkClient.api.search(genre.name)
                    val stateNow = _uiState.value
                    if (stateNow is GenreUiState.Success) {
                        _uiState.value = stateNow.copy(
                            animeList = results,
                            isSearchingAnime = false
                        )
                    }
                } catch (e: Exception) {
                    val stateNow = _uiState.value
                    if (stateNow is GenreUiState.Success) {
                        _uiState.value = stateNow.copy(isSearchingAnime = false)
                    }
                }
            }
        }
    }

    fun clearGenreSelection() {
        val current = _uiState.value
        if (current is GenreUiState.Success) {
            _uiState.value = current.copy(
                selectedGenre = null,
                animeList = emptyList(),
                isSearchingAnime = false
            )
        }
    }
}
