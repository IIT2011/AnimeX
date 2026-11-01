package com.animex.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animex.app.core.recommendation.UserInterestEngine
import com.animex.app.core.recommendation.ScoredAnime
import com.animex.app.core.network.NetworkClient
import com.animex.app.core.network.model.AnimeDto
import com.animex.app.core.network.model.HomeFeedDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeFeedData(
    val rollingSpotlight: List<ScoredAnime>,
    val recommendedForYou: List<ScoredAnime>,
    val favoriteGenreTitle: String?,
    val favoriteGenrePicks: List<AnimeDto>,
    val trending: List<AnimeDto>,
    val latest: List<AnimeDto>,
    val topAiring: List<AnimeDto>,
    val mostPopular: List<AnimeDto>
)

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val data: HomeFeedData) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var rawFeedCache: HomeFeedDto? = null

    init {
        loadHome()
        observeRecommendationsUpdates()
    }

    private fun observeRecommendationsUpdates() {
        viewModelScope.launch {
            UserInterestEngine.intelVersion.collect {
                // When user preferences and watch history update, refresh recommendations
                val feed = rawFeedCache
                if (feed != null && _uiState.value is HomeUiState.Success) {
                    processFeedData(feed)
                }
            }
        }
    }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val feed = NetworkClient.api.getHome()
                rawFeedCache = feed
                processFeedData(feed)
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.localizedMessage ?: "Failed to load telemetry feed")
            }
        }
    }

    private fun processFeedData(feed: HomeFeedDto) {
        // Collect candidate pool
        val candidatePool = (feed.spotlight + feed.trending + feed.latest + feed.topAiring + feed.mostPopular)
            .distinctBy { it.id }

        // Rank candidates using interest scoring
        val rankedAll = UserInterestEngine.rankAndRecommend(candidatePool, limit = 24)

        // Upper rolling recommendations: take spotlight entries augmented with top ranked titles
        val spotlightCandidates = (feed.spotlight + candidatePool.take(6)).distinctBy { it.id }
        val rollingRecommendations = UserInterestEngine.rankAndRecommend(spotlightCandidates, limit = 6)

        // Down animes: Top recommendations for user
        val rollingIds = rollingRecommendations.map { it.anime.id }.toSet()
        val recommendedForYou = rankedAll.filter { it.anime.id !in rollingIds }.take(10)

        // User's top favorite genre
        val topGenre = UserInterestEngine.getTopFavoriteGenres(1).firstOrNull() ?: "Action"
        val favoriteGenrePicks = candidatePool.filter { anime ->
            anime.genres?.any { it.name.equals(topGenre, ignoreCase = true) } == true
        }.take(8)

        val feedData = HomeFeedData(
            rollingSpotlight = rollingRecommendations.ifEmpty {
                rankedAll.take(5)
            },
            recommendedForYou = if (recommendedForYou.isNotEmpty()) recommendedForYou else rankedAll.take(8),
            favoriteGenreTitle = topGenre.uppercase(),
            favoriteGenrePicks = favoriteGenrePicks,
            trending = feed.trending,
            latest = feed.latest,
            topAiring = feed.topAiring,
            mostPopular = feed.mostPopular
        )

        _uiState.value = HomeUiState.Success(feedData)
    }
}
