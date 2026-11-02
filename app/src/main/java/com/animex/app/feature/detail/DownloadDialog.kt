package com.animex.app.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.animex.app.core.designsystem.*
import com.animex.app.core.download.DownloadDestination
import com.animex.app.core.network.model.EpisodeItemDto

@Composable
fun DownloadDialog(
    animeTitle: String,
    episodes: List<EpisodeItemDto>,
    onDismiss: () -> Unit,
    onStartDownload: (
        selectedEpisodes: List<Pair<Long, Int>>,
        destination: DownloadDestination,
        quality: String,
        audioTrack: String,
        subtitle: String?
    ) -> Unit
) {
    var selectedEpisodeIds by remember {
        mutableStateOf(episodes.map { it.id }.toSet())
    }
    var destination by remember { mutableStateOf(DownloadDestination.IN_APP) }
    var selectedQuality by remember { mutableStateOf("720p") }
    var selectedAudio by remember { mutableStateOf("sub") }
    var selectedSubtitle by remember { mutableStateOf("English") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(16.dp))
                .background(VoidDark)
                .border(1.dp, CardBorderGlowBrush, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "DOWNLOAD EPISODES",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = animeTitle,
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ObsidianCard)
                            .border(1.dp, GlassBorder, CircleShape)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Destination Option
                Text(
                    text = "DOWNLOAD LOCATION",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DestinationCard(
                        title = "In App Download",
                        subtitle = "Saved in app profile for offline viewing",
                        icon = Icons.Default.PhoneAndroid,
                        isSelected = destination == DownloadDestination.IN_APP,
                        onClick = { destination = DownloadDestination.IN_APP },
                        modifier = Modifier.weight(1f)
                    )

                    DestinationCard(
                        title = "External Download",
                        subtitle = "Downloads/AnimeX/<Anime>/<Title S1 EP#>.mp4",
                        icon = Icons.Default.Folder,
                        isSelected = destination == DownloadDestination.EXTERNAL,
                        onClick = { destination = DownloadDestination.EXTERNAL },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Quality Selector
                Text(
                    text = "VIDEO QUALITY",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("1080p", "720p", "360p").forEach { q ->
                        CyberOptionChip(
                            text = q,
                            isSelected = selectedQuality == q,
                            onClick = { selectedQuality = q },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Audio & Subtitle Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AUDIO",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("sub" to "SUB", "dub" to "DUB").forEach { (k, v) ->
                                CyberOptionChip(
                                    text = v,
                                    isSelected = selectedAudio == k,
                                    onClick = { selectedAudio = k },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SUBTITLES",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("English", "None").forEach { s ->
                                CyberOptionChip(
                                    text = s,
                                    isSelected = selectedSubtitle == s,
                                    onClick = { selectedSubtitle = s },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Episode Selection Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SELECT EPISODES (${selectedEpisodeIds.size}/${episodes.size})",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = if (selectedEpisodeIds.size == episodes.size) "DESELECT ALL" else "SELECT ALL",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.clickable {
                            selectedEpisodeIds = if (selectedEpisodeIds.size == episodes.size) emptySet()
                            else episodes.map { it.id }.toSet()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Episodes Grid
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 65.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(episodes) { ep ->
                        val isSelected = selectedEpisodeIds.contains(ep.id)
                        Box(
                            modifier = Modifier
                                .height(38.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) ObsidianCard else VoidDark)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) NeonCyan else GlassBorder,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    selectedEpisodeIds = if (isSelected) {
                                        selectedEpisodeIds - ep.id
                                    } else {
                                        selectedEpisodeIds + ep.id
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "EP ${ep.number}",
                                color = if (isSelected) NeonCyan else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Button
                CyberButton(
                    text = if (selectedEpisodeIds.isEmpty()) "SELECT EPISODES TO DOWNLOAD"
                           else "START DOWNLOAD (${selectedEpisodeIds.size} EPS)",
                    isPrimary = selectedEpisodeIds.isNotEmpty(),
                    onClick = {
                        if (selectedEpisodeIds.isNotEmpty()) {
                            val selectedList = episodes
                                .filter { selectedEpisodeIds.contains(it.id) }
                                .map { it.id to it.number }
                            onStartDownload(
                                selectedList,
                                destination,
                                selectedQuality,
                                selectedAudio,
                                selectedSubtitle
                            )
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun DestinationCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) ObsidianCard else VoidDark)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                brush = if (isSelected) CardBorderGlowBrush else Brush.linearGradient(listOf(GlassBorder, GlassBorder)),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) NeonCyan else TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    color = if (isSelected) TextPrimary else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun CyberOptionChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) ObsidianCard else VoidDark)
            .border(
                width = 1.dp,
                color = if (isSelected) NeonCyan else GlassBorder,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) NeonCyan else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontFamily = FontFamily.Monospace
        )
    }
}
