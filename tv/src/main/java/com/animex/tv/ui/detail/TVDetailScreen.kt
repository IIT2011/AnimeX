package com.animex.tv.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.animex.tv.components.TVEpisodeCard
import com.animex.tv.core.model.AnimeDto
import com.animex.tv.core.model.EpisodeItemDto
import com.animex.tv.core.network.NetworkClient
import com.animex.tv.core.session.UserSessionRepository
import com.animex.tv.theme.*

@Composable
fun TVDetailScreen(
    animeId: Long,
    onBack: () -> Unit,
    onPlayEpisode: (Long, Int, String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var isLoading by remember { mutableStateOf(true) }
    var anime by remember { mutableStateOf<AnimeDto?>(null) }
    var episodes by remember { mutableStateOf<List<EpisodeItemDto>>(emptyList()) }
    var selectedEpisode by remember { mutableStateOf<EpisodeItemDto?>(null) }

    val watchlist by UserSessionRepository.watchlist.collectAsState()
    val isInWatchlist = anime?.let { UserSessionRepository.isAnimeInWatchlist(it.id) } ?: false

    LaunchedEffect(animeId) {
        try {
            val animeDetail = NetworkClient.api.getAnimeDetail(animeId)
            anime = animeDetail
            val epRes = NetworkClient.api.getEpisodes(animeId)
            episodes = epRes.data
            selectedEpisode = epRes.data.firstOrNull()
            isLoading = false
        } catch (_: Exception) {
            isLoading = false
        }
    }

    if (isLoading || anime == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(CyberVoid),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = CyberCyan, strokeWidth = 3.dp)
        }
        return
    }

    val currentAnime = anime!!
    val backdropUrl = NetworkClient.resolvePosterUrl(currentAnime.banner ?: currentAnime.poster)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberVoid)
    ) {
        // High-res Background Backdrop
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(backdropUrl)
                .crossfade(true)
                .build(),
            contentDescription = currentAnime.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient Dimming Masks
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            CyberVoid,
                            CyberVoid.copy(alpha = 0.95f),
                            CyberVoid.copy(alpha = 0.70f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            CyberVoid.copy(alpha = 0.6f),
                            CyberVoid.copy(alpha = 0.9f),
                            CyberVoid
                        )
                    )
                )
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 28.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Top Bar with Back Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TVIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    label = "BACK",
                    onClick = onBack
                )

                currentAnime.titleJp?.let { jp ->
                    Text(
                        text = jp,
                        color = CyberCyan.copy(alpha = 0.75f),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Split View: Poster + Metadata
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // Left: High-Res Poster
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .height(260.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberCardBg)
                        .border(1.5.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(NetworkClient.resolvePosterUrl(currentAnime.poster))
                            .crossfade(true)
                            .build(),
                        contentDescription = currentAnime.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Center/Right: Title, Badges, Synopsis, Actions
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 4.dp)
                ) {
                    Text(
                        text = currentAnime.title,
                        color = CyberTextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Badges Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currentAnime.score?.let { score ->
                            if (score > 0) {
                                DetailBadge(text = "★ ${String.format("%.1f", score)}", color = CyberGold)
                            }
                        }
                        currentAnime.type?.let { DetailBadge(text = it.uppercase(), color = CyberTextPrimary) }
                        currentAnime.status?.let { DetailBadge(text = it.uppercase(), color = CyberNeonGreen) }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Genres
                    currentAnime.genres?.takeIf { it.isNotEmpty() }?.let { genreList ->
                        Text(
                            text = genreList.joinToString(" • ") { it.name },
                            color = CyberCyan.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Synopsis
                    Text(
                        text = currentAnime.synopsis ?: "No detailed transmission synopsis recorded.",
                        color = CyberTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Action Buttons Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val firstEp = episodes.firstOrNull()
                        TVActionButton(
                            icon = Icons.Default.PlayArrow,
                            label = if (firstEp != null) "PLAY EPISODE 1" else "PLAY NOW",
                            isPrimary = true,
                            onClick = {
                                if (firstEp != null) {
                                    onPlayEpisode(firstEp.id, firstEp.number, currentAnime.title, currentAnime.poster)
                                }
                            }
                        )

                        TVActionButton(
                            icon = if (isInWatchlist) Icons.Default.BookmarkAdded else Icons.Default.BookmarkAdd,
                            label = if (isInWatchlist) "SAVED" else "WATCHLIST",
                            isPrimary = false,
                            onClick = {
                                UserSessionRepository.toggleWatchlist(context, currentAnime)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Horizontal Episodes Carousel Shelf
            Text(
                text = "EPISODES (${episodes.size})",
                color = CyberTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (episodes.isEmpty()) {
                Text(
                    text = "No episodes currently detected in stream broadcast.",
                    color = CyberTextSecondary,
                    fontSize = 13.sp
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(episodes, key = { it.id }) { ep ->
                        TVEpisodeCard(
                            episode = ep,
                            fallbackPoster = currentAnime.banner ?: currentAnime.poster,
                            isSelected = selectedEpisode?.id == ep.id,
                            onClick = {
                                selectedEpisode = ep
                                onPlayEpisode(ep.id, ep.number, currentAnime.title, currentAnime.poster)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CyberVoid.copy(alpha = 0.8f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun TVIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .scale(if (isFocused) 1.08f else 1.0f)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) CyberCyan.copy(alpha = 0.25f) else CyberCardBg)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) CyberCyan else CyberBorderSubtle,
                shape = RoundedCornerShape(8.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isFocused) CyberCyan else CyberTextPrimary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                color = if (isFocused) CyberCyan else CyberTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun TVActionButton(
    icon: ImageVector,
    label: String,
    isPrimary: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val bgBrush = if (isFocused) {
        Brush.horizontalGradient(listOf(CyberCyan, CyberCyan.copy(alpha = 0.85f)))
    } else if (isPrimary) {
        Brush.horizontalGradient(listOf(CyberCyan.copy(alpha = 0.9f), CyberCyan.copy(alpha = 0.7f)))
    } else {
        Brush.horizontalGradient(listOf(CyberCardBg, CyberCardBg))
    }

    val contentColor = if (isFocused || isPrimary) Color.Black else CyberTextPrimary
    val borderColor = if (isFocused) CyberCyan else if (isPrimary) Color.Transparent else CyberBorderSubtle

    Box(
        modifier = Modifier
            .scale(if (isFocused) 1.08f else 1.0f)
            .clip(RoundedCornerShape(8.dp))
            .background(bgBrush)
            .border(width = if (isFocused) 2.5.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(8.dp))
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 1.sp
            )
        }
    }
}
