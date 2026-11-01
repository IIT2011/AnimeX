package com.animex.app.feature.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.animex.app.core.recommendation.UserInterestEngine
import com.animex.app.core.network.NetworkClient
import com.animex.app.core.network.model.AnimeDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchFilters(
    val type: String? = null, // "TV", "Movie", "OVA"
    val status: String? = null, // "airing", "completed"
    val sort: String = "most_popular" // "most_popular", "latest", "score"
)

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(
        val results: List<AnimeDto>,
        val query: String,
        val filters: SearchFilters
    ) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _filters = MutableStateFlow(SearchFilters())
    val filters: StateFlow<SearchFilters> = _filters.asStateFlow()

    private var currentQuery = ""
    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        currentQuery = query
        searchJob?.cancel()

        if (query.isBlank()) {
            // If filters are active, run browse with filters
            if (_filters.value.type != null || _filters.value.status != null) {
                applyFilters(_filters.value)
            } else {
                _uiState.value = SearchUiState.Idle
            }
            return
        }

        searchJob = viewModelScope.launch {
            delay(300) // Debounce typing
            if (query.length >= 3) {
                UserInterestEngine.recordSearch(getApplication(), query)
            }
            _uiState.value = SearchUiState.Loading
            try {
                val results = NetworkClient.api.search(query)
                // Filter locally by type if filter active
                val filtered = if (_filters.value.type != null) {
                    results.filter { it.type.equals(_filters.value.type, ignoreCase = true) }
                } else {
                    results
                }
                _uiState.value = SearchUiState.Success(
                    results = filtered,
                    query = query,
                    filters = _filters.value
                )
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(e.localizedMessage ?: "Search scan failed")
            }
        }
    }

    fun applyFilters(newFilters: SearchFilters) {
        _filters.value = newFilters
        searchJob?.cancel()

        searchJob = viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            try {
                if (currentQuery.isNotBlank()) {
                    val results = NetworkClient.api.search(currentQuery)
                    val filtered = results.filter { item ->
                        (newFilters.type == null || item.type.equals(newFilters.type, ignoreCase = true)) &&
                        (newFilters.status == null || item.status.equals(newFilters.status, ignoreCase = true))
                    }
                    _uiState.value = SearchUiState.Success(filtered, currentQuery, newFilters)
                } else {
                    val paged = NetworkClient.api.browse(
                        type = newFilters.type,
                        status = newFilters.status,
                        sort = newFilters.sort,
                        page = 1
                    )
                    _uiState.value = SearchUiState.Success(paged.data, "", newFilters)
                }
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(e.localizedMessage ?: "Filter search failed")
            }
        }
    }

    fun clearFilters() {
        applyFilters(SearchFilters())
    }
}
