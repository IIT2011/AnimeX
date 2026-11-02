package com.animex.app.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.animex.app.core.recommendation.UserInterestEngine
import com.animex.app.core.designsystem.*
import com.animex.app.core.network.NetworkClient
import com.animex.app.core.session.UserSessionRepository

@Composable
fun AnimeDetailScreen(
    animeId: Long,
    onBackClick: () -> Unit,
    onEpisodeClick: (episodeId: Long, episodeNumber: Int, title: String, poster: String?) -> Unit,
    viewModel: DetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val watchlist by UserSessionRepository.watchlist.collectAsState()

    LaunchedEffect(animeId) {
        viewModel.loadAnime(animeId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidDark)
    ) {
        when (val state = uiState) {
            is DetailUiState.Loading -> {
                CircularProgressIndicator(
                    color = NeonCyan,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is DetailUiState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "DATA LINK LOST", color = NeonMagenta, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(12.dp))
                    CyberButton(text = "RETRY", onClick = { viewModel.loadAnime(animeId) })
                }
            }
            is DetailUiState.Success -> {
                val anime = state.anime
                val episodes = state.episodes
                val bannerUrl = NetworkClient.getFullPosterUrl(anime.banner ?: anime.poster)

                LaunchedEffect(anime.id) {
                    UserInterestEngine.recordAnimeView(context, anime)
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // Header Banner with Back button
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                        ) {
                            AsyncImage(
                                model = bannerUrl,
                                contentDescription = anime.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(HeroVignetteBrush)
                            )

                            // Top Back Button HUD
                            Box(
                                modifier = Modifier
                                    .statusBarsPadding()
                                    .padding(16.dp)
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ObsidianCard)
                                    .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                                    .clickable(onClick = onBackClick),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = NeonCyan
                                )
                            }
                        }
                    }

                    // Anime Title & Telemetry info
                    item {
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            Text(
                                text = anime.title,
                                color = TextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            anime.titleJp?.let { jp ->
                                Text(
                                    text = jp,
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                anime.score?.let { score ->
                                    CyberBadge(text = "★ $score", accentColor = NeonAmber)
                                }
                                CyberBadge(text = anime.type ?: "TV", accentColor = NeonCyan)
                                anime.status?.let { status ->
                                    CyberBadge(text = status, accentColor = NeonLime)
                                }
                                if ((anime.sub ?: 0) > 0) {
                                    CyberBadge(text = "SUB ${anime.sub}", accentColor = NeonMagenta)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            val isInWatchlist = watchlist.any { it.id == anime.id }
                            CyberButton(
                                text = if (isInWatchlist) "✓ IN WATCHLIST" else "+ ADD TO WATCHLIST",
                                isPrimary = !isInWatchlist,
                                onClick = {
                                    UserSessionRepository.toggleWatchlist(context, anime)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            anime.synopsis?.let { synopsis ->
                                Text(
                                    text = synopsis,
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(28.dp))

                            // Episode Selection Matrix header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "EPISODES",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                                CyberBadge(text = "${episodes.size} EPS", accentColor = NeonCyan)
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Episodes Grid
                    item {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 75.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 600.dp)
                                .padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(episodes) { ep ->
                                Box(
                                    modifier = Modifier
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(ObsidianCard)
                                        .border(1.dp, CardBorderGlowBrush, RoundedCornerShape(6.dp))
                                        .clickable { onEpisodeClick(ep.id, ep.number, anime.title, anime.poster) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "EP ${ep.number}",
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
