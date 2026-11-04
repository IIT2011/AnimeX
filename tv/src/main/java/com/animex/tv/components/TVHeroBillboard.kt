package com.animex.tv.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.animex.tv.core.model.AnimeDto
import com.animex.tv.core.network.NetworkClient
import com.animex.tv.theme.*

@Composable
fun TVHeroBillboard(
    anime: AnimeDto?,
    isInWatchlist: Boolean,
    onPlayClick: () -> Unit,
    onWatchlistClick: () -> Unit,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(340.dp)
    ) {
        if (anime != null) {
            val backdropUrl = remember(anime.banner, anime.poster) {
                NetworkClient.resolvePosterUrl(anime.banner ?: anime.poster)
            }

            AnimatedContent(
                targetState = backdropUrl,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "billboardBackdrop"
            ) { targetUrl ->
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(targetUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = anime.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Cinematic Multi-Stop Gradient Overlays (Void Black left-to-right & bottom-to-top)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                CyberVoid,
                                CyberVoid.copy(alpha = 0.90f),
                                CyberVoid.copy(alpha = 0.50f),
                                Color.Transparent
                            ),
                            startX = 0f,
                            endX = 1400f
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                CyberVoid.copy(alpha = 0.40f),
                                CyberVoid
                            )
                        )
                    )
            )

            // Content Left Column
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 48.dp, bottom = 28.dp)
                    .widthIn(max = 680.dp)
            ) {
                // Japanese Romaji Sub-title
                anime.titleJp?.let { jp ->
                    Text(
                        text = jp,
                        color = CyberCyan.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Main Title
                Text(
                    text = anime.title,
                    color = CyberTextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Metadata Badges Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Rating Badge
                    if (anime.score != null && anime.score > 0) {
                        BadgePill(
                            icon = Icons.Default.Star,
                            text = String.format("%.1f", anime.score),
                            contentColor = CyberGold,
                            borderColor = CyberGold.copy(alpha = 0.5f)
                        )
                    }

                    // Episode Count
                    val eps = anime.eps ?: anime.sub ?: 0
                    if (eps > 0) {
                        BadgePill(
                            text = "$eps EPISODES",
                            contentColor = CyberTextPrimary,
                            borderColor = CyberBorderSubtle
                        )
                    }

                    // Status
                    anime.status?.let { status ->
                        BadgePill(
                            text = status.uppercase(),
                            contentColor = CyberNeonGreen,
                            borderColor = CyberNeonGreen.copy(alpha = 0.4f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Synopsis Preview
                val synopsis = anime.synopsis ?: "Experience high-definition anime streaming with instant playback and crystal clear audio."
                Text(
                    text = synopsis,
                    color = CyberTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(20.dp))

                // D-pad Remote Focusable Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TVActionButton(
                        icon = Icons.Default.PlayArrow,
                        label = "WATCH NOW",
                        isPrimary = true,
                        onClick = onPlayClick
                    )

                    TVActionButton(
                        icon = if (isInWatchlist) Icons.Default.BookmarkAdded else Icons.Default.BookmarkAdd,
                        label = if (isInWatchlist) "SAVED" else "WATCHLIST",
                        isPrimary = false,
                        onClick = onWatchlistClick
                    )

                    TVActionButton(
                        icon = Icons.Default.Info,
                        label = "DETAILS",
                        isPrimary = false,
                        onClick = onDetailsClick
                    )
                }
            }
        }
    }
}

@Composable
private fun BadgePill(
    text: String,
    contentColor: Color,
    borderColor: Color,
    icon: ImageVector? = null
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CyberVoid.copy(alpha = 0.75f))
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = text,
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
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
            .padding(horizontal = 18.dp, vertical = 10.dp),
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
                modifier = Modifier.size(18.dp)
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
