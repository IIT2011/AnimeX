package com.animex.tv.ui.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animex.tv.components.TVCard
import com.animex.tv.components.TVHeroBillboard
import com.animex.tv.components.TVHistoryCard
import com.animex.tv.core.model.AnimeDto
import com.animex.tv.core.model.HomeFeedDto
import com.animex.tv.core.model.WatchHistoryEntry
import com.animex.tv.core.network.NetworkClient
import com.animex.tv.core.session.UserSessionRepository
import com.animex.tv.theme.*
import kotlinx.coroutines.launch

@Composable
fun TVHomeScreen(
    onOpenAnimeDetail: (Long) -> Unit,
    onPlayAnimeDirectly: (Long, Long?, String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var homeFeed by remember { mutableStateOf<HomeFeedDto?>(null) }

    var focusedAnime by remember { mutableStateOf<AnimeDto?>(null) }
    val watchlist by UserSessionRepository.watchlist.collectAsState()
    val continueWatching by UserSessionRepository.continueWatching.collectAsState()

    // Fetch Home Data
    LaunchedEffect(Unit) {
        try {
            val feed = NetworkClient.api.getHome()
            homeFeed = feed
            if (focusedAnime == null) {
                focusedAnime = feed.spotlight.firstOrNull()
                    ?: feed.trending.firstOrNull()
                    ?: feed.latest.firstOrNull()
            }
            isLoading = false
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Failed to load TV feeds"
            isLoading = false
        }
    }

    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(CyberVoid),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    color = CyberCyan,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "INITIALIZING ANIMEX TV FEED...",
                    color = CyberCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }
        return
    }

    val currentFocused = focusedAnime ?: homeFeed?.trending?.firstOrNull()
    val isFocusedInWatchlist = currentFocused?.let { UserSessionRepository.isAnimeInWatchlist(it.id) } ?: false

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberVoid),
        contentPadding = PaddingValues(bottom = 60.dp)
    ) {
        // Hero Billboard Header
        item {
            TVHeroBillboard(
                anime = currentFocused,
                isInWatchlist = isFocusedInWatchlist,
                onPlayClick = {
                    currentFocused?.let { anime ->
                        onPlayAnimeDirectly(anime.id, anime.episodeId, anime.title, anime.poster)
                    }
                },
                onWatchlistClick = {
                    currentFocused?.let { anime ->
                        UserSessionRepository.toggleWatchlist(context, anime)
                    }
                },
                onDetailsClick = {
                    currentFocused?.let { anime ->
                        onOpenAnimeDetail(anime.id)
                    }
                }
            )
        }

        // Continue Watching Shelf
        if (continueWatching.isNotEmpty()) {
            item {
                TVContinueWatchingShelf(
                    historyList = continueWatching,
                    onResume = { entry ->
                        onPlayAnimeDirectly(entry.animeId, entry.episodeId, entry.title, entry.poster)
                    }
                )
            }
        }

        // Horizontal Content Shelves
        homeFeed?.trending?.takeIf { it.isNotEmpty() }?.let { list ->
            item {
                TVShelfRow(
                    title = "🔥 TRENDING ON ANIMEX",
                    animeList = list,
                    onAnimeFocused = { focusedAnime = it },
                    onAnimeSelected = { onOpenAnimeDetail(it.id) }
                )
            }
        }

        homeFeed?.spotlight?.takeIf { it.isNotEmpty() }?.let { list ->
            item {
                TVShelfRow(
                    title = "⭐ RECOMMENDED FOR YOU",
                    animeList = list,
                    onAnimeFocused = { focusedAnime = it },
                    onAnimeSelected = { onOpenAnimeDetail(it.id) }
                )
            }
        }

        homeFeed?.latest?.takeIf { it.isNotEmpty() }?.let { list ->
            item {
                TVShelfRow(
                    title = "⚡ LATEST BROADCASTS",
                    animeList = list,
                    onAnimeFocused = { focusedAnime = it },
                    onAnimeSelected = { onOpenAnimeDetail(it.id) }
                )
            }
        }

        homeFeed?.topAiring?.takeIf { it.isNotEmpty() }?.let { list ->
            item {
                TVShelfRow(
                    title = "👑 TOP RATED AIRING",
                    animeList = list,
                    onAnimeFocused = { focusedAnime = it },
                    onAnimeSelected = { onOpenAnimeDetail(it.id) }
                )
            }
        }

        homeFeed?.mostPopular?.takeIf { it.isNotEmpty() }?.let { list ->
            item {
                TVShelfRow(
                    title = "💎 ALL-TIME POPULAR",
                    animeList = list,
                    onAnimeFocused = { focusedAnime = it },
                    onAnimeSelected = { onOpenAnimeDetail(it.id) }
                )
            }
        }
    }
}

@Composable
private fun TVShelfRow(
    title: String,
    animeList: List<AnimeDto>,
    onAnimeFocused: (AnimeDto) -> Unit,
    onAnimeSelected: (AnimeDto) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = title,
            color = CyberTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 48.dp, bottom = 12.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(animeList, key = { it.id }) { anime ->
                TVCard(
                    anime = anime,
                    onClick = { onAnimeSelected(anime) },
                    onFocus = { onAnimeFocused(anime) }
                )
            }
        }
    }
}

@Composable
private fun TVContinueWatchingShelf(
    historyList: List<WatchHistoryEntry>,
    onResume: (WatchHistoryEntry) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = "▶ CONTINUE WATCHING",
            color = CyberCyan,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 48.dp, bottom = 12.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(historyList, key = { "${it.animeId}_${it.episodeId}_${it.episodeNumber}" }) { item ->
                TVHistoryCard(
                    entry = item,
                    onClick = { onResume(item) }
                )
            }
        }
    }
}
