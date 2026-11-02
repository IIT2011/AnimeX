package com.animex.tv.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.animex.tv.core.model.EpisodeItemDto
import com.animex.tv.core.network.NetworkClient
import com.animex.tv.theme.*

@Composable
fun TVEpisodeCard(
    episode: EpisodeItemDto,
    fallbackPoster: String?,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "epCardScale"
    )

    val thumbUrl = remember(episode.thumbnail, fallbackPoster) {
        NetworkClient.resolvePosterUrl(episode.thumbnail ?: fallbackPoster)
    }

    Box(
        modifier = modifier
            .width(220.dp)
            .scale(scale)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) CyberCyan.copy(alpha = 0.15f) else CyberCardBg)
            .border(
                width = if (isFocused) 2.5.dp else if (isSelected) 1.5.dp else 1.dp,
                color = if (isFocused) CyberCyan else if (isSelected) CyberCyan.copy(alpha = 0.6f) else CyberBorderSubtle,
                shape = RoundedCornerShape(10.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(8.dp)
    ) {
        Column {
            // Thumbnail container (16:9)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberVoid)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(thumbUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = episode.title ?: "Episode ${episode.number}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Ep Badge overlay
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(CyberVoid.copy(alpha = 0.85f))
                        .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        text = "EP ${episode.number}",
                        color = CyberCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Play icon overlay when focused
                if (isFocused) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(CyberCyan.copy(alpha = 0.9f))
                            .align(Alignment.Center),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Episode Title
            val titleText = episode.title?.takeIf { it.isNotBlank() } ?: "Episode ${episode.number}"
            Text(
                text = titleText,
                color = if (isFocused) CyberCyan else CyberTextPrimary,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Duration
            val durationText = episode.duration?.let { "${it / 60}m" } ?: "24m"
            Text(
                text = "$durationText • SUB",
                color = CyberTextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
