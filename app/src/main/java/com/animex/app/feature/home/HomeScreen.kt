package com.animex.app.feature.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.animex.app.R
import com.animex.app.core.recommendation.ScoredAnime
import com.animex.app.core.designsystem.*
import com.animex.app.core.network.NetworkClient
import com.animex.app.core.network.model.AnimeDto
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onAnimeClick: (Long) -> Unit,
    onPlayClick: (episodeId: Long, animeTitle: String?, episodeNumber: Int?, poster: String?, localPath: String?) -> Unit,
    onSearchClick: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = VoidDark,
        topBar = {
            CyberTopBar(onSearchClick = onSearchClick)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeonCyan)
                    }
                }
                is HomeUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "SIGNAL DISRUPTED",
                                color = NeonMagenta,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = state.message, color = TextSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            CyberButton(text = "RECONNECT", onClick = { viewModel.loadHome() })
                        }
                    }
                }
                is HomeUiState.Success -> {
                    val feed = state.data
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 36.dp)
                    ) {
                        // 1. Upper Rolling Spotlight Carousel
                        if (feed.rollingSpotlight.isNotEmpty()) {
                            item {
                                RollingRecommendationsCarousel(
                                    slides = feed.rollingSpotlight,
                                    onAnimeClick = onAnimeClick,
                                    onPlayClick = onPlayClick
                                )
                            }
                        }

                        // 2. Down Animes: Recommended for You
                        if (feed.recommendedForYou.isNotEmpty()) {
                            item {
                                RecommendedForYouSection(
                                    scoredAnimes = feed.recommendedForYou,
                                    onAnimeClick = onAnimeClick
                                )
                            }
                        }

                        // 3. User's Favorite Genre Sector
                        if (feed.favoriteGenreTitle != null && feed.favoriteGenrePicks.isNotEmpty()) {
                            item {
                                AnimeSectionRail(
                                    sectionTitle = "TRENDING IN ${feed.favoriteGenreTitle}",
                                    badgeText = "HOT",
                                    animes = feed.favoriteGenrePicks,
                                    onAnimeClick = onAnimeClick
                                )
                            }
                        }

                        // 4. Trending Now
                        if (feed.trending.isNotEmpty()) {
                            item {
                                AnimeSectionRail(
                                    sectionTitle = "TRENDING NOW",
                                    badgeText = "HOT",
                                    animes = feed.trending,
                                    onAnimeClick = onAnimeClick
                                )
                            }
                        }

                        // 5. Latest Releases Section
                        if (feed.latest.isNotEmpty()) {
                            item {
                                AnimeSectionRail(
                                    sectionTitle = "LATEST RELEASES",
                                    badgeText = "NEW",
                                    animes = feed.latest,
                                    onAnimeClick = onAnimeClick
                                )
                            }
                        }

                        // 6. Top Airing Series
                        if (feed.topAiring.isNotEmpty()) {
                            item {
                                AnimeSectionRail(
                                    sectionTitle = "TOP AIRING SERIES",
                                    badgeText = "ONGOING",
                                    animes = feed.topAiring,
                                    onAnimeClick = onAnimeClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CyberTopBar(
    onSearchClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(VoidDark)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "AnimeX Logo",
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "ANIME",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
            Text(
                text = "X",
                color = NeonCyan,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(10.dp))
            StatusBeacon(color = NeonLime)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "ONLINE",
                color = NeonLime,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(ObsidianCard)
                .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                .cyberBounceClick(onClick = onSearchClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = NeonCyan,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Upper Home Screen: Auto-Rolling Hero Carousel powered by HorizontalPager.
 */
@Composable
fun RollingRecommendationsCarousel(
    slides: List<ScoredAnime>,
    onAnimeClick: (Long) -> Unit,
    onPlayClick: (episodeId: Long, animeTitle: String?, episodeNumber: Int?, poster: String?, localPath: String?) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { slides.size })

    // Auto-advance rolling recommendations every 4.5 seconds
    LaunchedEffect(pagerState, slides.size) {
        if (slides.size <= 1) return@LaunchedEffect
        while (true) {
            delay(4500L)
            try {
                val targetPage = (pagerState.currentPage + 1) % slides.size
                pagerState.animateScrollToPage(
                    page = targetPage,
                    animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing)
                )
            } catch (e: Exception) {
                break
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(390.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val scored = slides[page]
            val anime = scored.anime
            val fullBanner = NetworkClient.getFullPosterUrl(anime.banner ?: anime.poster)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onAnimeClick(anime.id) }
            ) {
                AsyncImage(
                    model = fullBanner,
                    contentDescription = anime.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Dark Vignette
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(HeroVignetteBrush)
                )

                // Top Floating HUD Chip: Slide counter & Match Score
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ObsidianCard.copy(alpha = 0.85f))
                            .border(1.dp, GlassBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusBeacon(color = NeonCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TOP PICKS 0${page + 1} / 0${slides.size}",
                                color = TextPrimary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeonCyan.copy(alpha = 0.15f))
                            .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${scored.matchPercentage}% MATCH",
                                color = NeonCyan,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                // Bottom Content
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 20.dp, vertical = 22.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CyberBadge(text = anime.type ?: "TV", accentColor = NeonCyan)
                        anime.score?.let { score ->
                            CyberBadge(text = "★ $score", accentColor = NeonAmber)
                        }
                        if ((anime.sub ?: 0) > 0) {
                            CyberBadge(text = "SUB ${anime.sub}", accentColor = NeonMagenta)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = anime.title,
                        color = TextPrimary,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    anime.synopsis?.let { synopsis ->
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = synopsis,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CyberButton(
                            text = "STREAM NOW",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = {
                                anime.episodeId?.let { epId ->
                                    onPlayClick(epId, anime.title, anime.episodeNumber ?: 1, anime.poster, null)
                                } ?: onAnimeClick(anime.id)
                            }
                        )

                        // Carousel Dots Indicator
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(slides.size) { dotIndex ->
                                val isActive = pagerState.currentPage == dotIndex
                                val width by animateDpAsState(
                                    targetValue = if (isActive) 22.dp else 6.dp,
                                    label = "dotWidth"
                                )
                                Box(
                                    modifier = Modifier
                                        .height(6.dp)
                                        .width(width)
                                        .clip(CircleShape)
                                        .background(if (isActive) NeonCyan else GlassBorder)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom cyber neon separator line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.BottomCenter)
                .background(CyberNeonBrush)
        )
    }
}

/**
 * Recommended For You Section: Tailored to user's interest and history.
 */
@Composable
fun RecommendedForYouSection(
    scoredAnimes: List<ScoredAnime>,
    onAnimeClick: (Long) -> Unit
) {
    Column(modifier = Modifier.padding(top = 24.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "RECOMMENDED FOR YOU",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    CyberBadge(text = "TOP PICKS", accentColor = NeonCyan)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Personalized picks based on your watch history",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(scoredAnimes) { scored ->
                RecommendedAnimeCard(
                    scored = scored,
                    onClick = { onAnimeClick(scored.anime.id) }
                )
            }
        }
    }
}

@Composable
fun RecommendedAnimeCard(
    scored: ScoredAnime,
    onClick: () -> Unit
) {
    val anime = scored.anime
    val posterUrl = NetworkClient.getFullPosterUrl(anime.poster)

    Column(
        modifier = Modifier
            .width(145.dp)
            .cyberBounceClick(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ObsidianCard)
                .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
        ) {
            AsyncImage(
                model = posterUrl,
                contentDescription = anime.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Top Floating Match Beacon
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(ObsidianCard.copy(alpha = 0.92f))
                    .border(1.dp, NeonCyan.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "${scored.matchPercentage}% MATCH",
                    color = NeonCyan,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                )
            }

            // Bottom subtle vignette
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, VoidDark.copy(alpha = 0.95f))
                        )
                    )
            )

            // Matched reason tag
            Text(
                text = scored.matchReason.uppercase(),
                color = NeonAmber,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = anime.title,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = anime.type ?: "TV",
                color = TextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            anime.score?.let { score ->
                Text(
                    text = "★ $score",
                    color = NeonAmber,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun AnimeSectionRail(
    sectionTitle: String,
    badgeText: String,
    animes: List<AnimeDto>,
    onAnimeClick: (Long) -> Unit
) {
    Column(modifier = Modifier.padding(top = 24.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = sectionTitle,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                CyberBadge(text = badgeText, accentColor = NeonMagenta)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(animes) { anime ->
                AnimePosterCard(anime = anime, onClick = { onAnimeClick(anime.id) })
            }
        }
    }
}

@Composable
fun AnimePosterCard(
    anime: AnimeDto,
    onClick: () -> Unit
) {
    val posterUrl = NetworkClient.getFullPosterUrl(anime.poster)

    Column(
        modifier = Modifier
            .width(135.dp)
            .cyberBounceClick(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(195.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(ObsidianCard)
                .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = posterUrl,
                contentDescription = anime.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Episode counter badge
            if ((anime.sub ?: 0) > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(ObsidianCard.copy(alpha = 0.9f))
                        .border(1.dp, GlassBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "EP ${anime.sub}",
                        color = NeonCyan,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = anime.title,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = anime.type ?: "TV",
                color = TextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            anime.score?.let { score ->
                Text(
                    text = "★ $score",
                    color = NeonAmber,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
