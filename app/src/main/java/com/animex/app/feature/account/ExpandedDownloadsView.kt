package com.animex.app.feature.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.animex.app.core.designsystem.*
import com.animex.app.core.download.AnimeDownloadGroup
import com.animex.app.core.download.DownloadDestination
import com.animex.app.core.download.DownloadRepository
import com.animex.app.core.download.DownloadStatus
import com.animex.app.core.download.DownloadTask
import com.animex.app.core.network.NetworkClient

enum class DownloadsTab(val title: String) {
    DOWNLOADS("Downloads"),
    DOWNLOADING("Downloading")
}

@Composable
fun ExpandedDownloadsView(
    onBackClick: () -> Unit,
    onPlayOffline: (localPath: String, animeTitle: String, episodeNumber: Int) -> Unit
) {
    val context = LocalContext.current
    val allTasks by DownloadRepository.tasks.collectAsState()

    // 1. In-App Completed Tasks (Tab: Downloads)
    val inAppCompletedTasks = remember(allTasks) {
        allTasks.filter { it.destination == DownloadDestination.IN_APP && it.status == DownloadStatus.COMPLETED }
    }

    val inAppGroups = remember(inAppCompletedTasks) {
        inAppCompletedTasks
            .groupBy { it.animeId }
            .map { (animeId, tasks) ->
                val first = tasks.first()
                AnimeDownloadGroup(
                    animeId = animeId,
                    animeTitle = first.animeTitle,
                    animePoster = first.animePoster,
                    tasks = tasks.sortedBy { it.episodeNumber }
                )
            }
            .sortedBy { it.animeTitle }
    }

    // 2. In-Progress & Queued Tasks (Tab: Downloading) - BOTH IN-APP AND EXTERNAL
    val activeTasks = remember(allTasks) {
        allTasks.filter {
            it.status == DownloadStatus.DOWNLOADING ||
            it.status == DownloadStatus.QUEUED ||
            it.status == DownloadStatus.FAILED
        }
    }

    val downloadingGroups = remember(activeTasks) {
        activeTasks
            .groupBy { it.animeId }
            .map { (animeId, tasks) ->
                val first = tasks.first()
                AnimeDownloadGroup(
                    animeId = animeId,
                    animeTitle = first.animeTitle,
                    animePoster = first.animePoster,
                    tasks = tasks.sortedBy { it.episodeNumber }
                )
            }
            .sortedBy { it.animeTitle }
    }

    // Active count for badge
    val activeDownloadCount = remember(activeTasks) {
        activeTasks.count { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED }
    }

    // Default to Downloading tab if active downloads exist, else Downloads tab
    var selectedTab by remember {
        mutableStateOf(if (activeDownloadCount > 0) DownloadsTab.DOWNLOADING else DownloadsTab.DOWNLOADS)
    }

    val totalSizeMb = inAppGroups.sumOf { it.totalSizeMb }
    val formattedTotalSize = if (totalSizeMb >= 1000) {
        String.format("%.2f GB", totalSizeMb / 1024.0)
    } else {
        String.format("%.1f MB", totalSizeMb)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidDark)
            .statusBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ObsidianCard)
                    .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                    .clickable(onClick = onBackClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Downloads",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${inAppGroups.size} IN-APP ANIMES • $formattedTotalSize USED",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Cyberpunk Tabs: [ Downloads (In-App) ] and [ Downloading (Both In-App & External) ]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ObsidianCard)
                .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                .padding(4.dp)
        ) {
            // Tab 1: Downloads
            val isDownloadsSelected = selectedTab == DownloadsTab.DOWNLOADS
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDownloadsSelected) NeonCyan.copy(alpha = 0.2f) else Color.Transparent)
                    .border(
                        1.dp,
                        if (isDownloadsSelected) NeonCyan else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { selectedTab = DownloadsTab.DOWNLOADS },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Downloads",
                        color = if (isDownloadsSelected) NeonCyan else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isDownloadsSelected) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                    if (inAppCompletedTasks.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isDownloadsSelected) NeonCyan else ObsidianCard)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "${inAppCompletedTasks.size}",
                                color = if (isDownloadsSelected) Color.Black else TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            // Tab 2: Downloading
            val isDownloadingSelected = selectedTab == DownloadsTab.DOWNLOADING
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDownloadingSelected) NeonMagenta.copy(alpha = 0.2f) else Color.Transparent)
                    .border(
                        1.dp,
                        if (isDownloadingSelected) NeonMagenta else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { selectedTab = DownloadsTab.DOWNLOADING },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Downloading",
                        color = if (isDownloadingSelected) NeonMagenta else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isDownloadingSelected) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                    if (activeTasks.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (activeDownloadCount > 0) NeonMagenta else ObsidianCard)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "${activeTasks.size}",
                                color = if (activeDownloadCount > 0) Color.White else TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tab Content
        when (selectedTab) {
            DownloadsTab.DOWNLOADS -> {
                // Completed In-App Downloads Grouped Anime-Wise
                if (inAppGroups.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.FolderOff,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "NO IN-APP DOWNLOADS",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Episodes downloaded with \"In App Download\" option will appear here for full offline playback.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(inAppGroups, key = { it.animeId }) { group ->
                            AnimeDownloadAccordionCard(
                                group = group,
                                onPlayOffline = onPlayOffline,
                                onDeleteEpisode = { taskId ->
                                    DownloadRepository.cancelOrDelete(context, taskId)
                                }
                            )
                        }
                    }
                }
            }

            DownloadsTab.DOWNLOADING -> {
                // In-Progress & Queued Downloads (Both In-App & External) Grouped Anime-Wise
                if (downloadingGroups.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "ALL DOWNLOADS COMPLETE",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Active progress for both External and In-App downloads will appear here in real time.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(downloadingGroups, key = { it.animeId }) { group ->
                            AnimeDownloadingGroupCard(
                                group = group,
                                onCancelTask = { taskId ->
                                    DownloadRepository.cancelOrDelete(context, taskId)
                                },
                                onRetryTask = { taskId ->
                                    DownloadRepository.retryDownload(context, taskId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Group Card for Active/Queued Downloading Tasks Sorted Anime-Wise
 */
@Composable
private fun AnimeDownloadingGroupCard(
    group: AnimeDownloadGroup,
    onCancelTask: (String) -> Unit,
    onRetryTask: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianCard)
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        // Group Header: Anime poster + title + active count
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 44.dp, height = 62.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(VoidDark)
            ) {
                AsyncImage(
                    model = NetworkClient.resolvePosterUrl(group.animePoster),
                    contentDescription = group.animeTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.animeTitle,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(NeonMagenta)
                    )
                    Text(
                        text = "${group.tasks.size} DOWNLOADING EPISODES",
                        color = NeonMagenta,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = GlassBorder.copy(alpha = 0.5f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(12.dp))

        // Downloading Task Rows for this Anime
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            group.tasks.forEach { task ->
                ActiveDownloadingTaskRow(
                    task = task,
                    onCancel = { onCancelTask(task.id) },
                    onRetry = { onRetryTask(task.id) }
                )
            }
        }
    }
}

/**
 * Individual Active Downloading Episode Row with Live Progress Bar
 */
@Composable
private fun ActiveDownloadingTaskRow(
    task: DownloadTask,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    val isExternal = task.destination == DownloadDestination.EXTERNAL
    val isDownloading = task.status == DownloadStatus.DOWNLOADING
    val isQueued = task.status == DownloadStatus.QUEUED
    val isFailed = task.status == DownloadStatus.FAILED

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(VoidDark.copy(alpha = 0.6f))
            .border(1.dp, if (isDownloading) NeonCyan.copy(alpha = 0.5f) else GlassBorder, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        // Episode Title & Destination Tag
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.fileName,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Destination Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isExternal) NeonCyan.copy(alpha = 0.15f) else NeonMagenta.copy(alpha = 0.15f))
                            .border(1.dp, if (isExternal) NeonCyan else NeonMagenta, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isExternal) "EXTERNAL DOWNLOAD" else "IN-APP OFFLINE",
                            color = if (isExternal) NeonCyan else NeonMagenta,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Quality Badge
                    Text(
                        text = "${task.quality} • ${task.audioTrack.uppercase()}",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Cancel or Retry Button
            if (isFailed) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NeonCyan.copy(alpha = 0.2f))
                        .border(1.dp, NeonCyan, RoundedCornerShape(6.dp))
                        .clickable(onClick = onRetry)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            tint = NeonCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "RETRY",
                            color = NeonCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(ObsidianCard)
                        .border(1.dp, GlassBorder, CircleShape)
                        .clickable(onClick = onCancel),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Progress Bar (Height 5.dp, CyberCyan to NeonMagenta gradient)
        val progress = when {
            isDownloading -> task.progress.coerceIn(0.05f, 0.99f)
            isQueued -> 0.05f
            isFailed -> 0f
            else -> 1.0f
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(ObsidianCard)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(NeonCyan, if (isExternal) NeonCyan else NeonMagenta)
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Progress & Status Sub-Label
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val statusText = when {
                isDownloading -> "DOWNLOADING (${(task.progress * 100).toInt()}%)"
                isQueued -> "QUEUED IN QUEUE"
                isFailed -> "FAILED: ${task.error ?: "Network error"}"
                else -> "COMPLETED"
            }

            val statusColor = when {
                isDownloading -> NeonCyan
                isQueued -> NeonAmber
                isFailed -> NeonMagenta
                else -> NeonLime
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isDownloading) {
                    StatusBeacon(color = NeonCyan)
                }
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Transferred size info
            val transferredText = if (isDownloading && task.downloadedBytes > 0) {
                val mbDownloaded = task.downloadedBytes / (1024.0 * 1024.0)
                if (task.totalBytes > 0) {
                    val mbTotal = task.totalBytes / (1024.0 * 1024.0)
                    String.format("%.1f MB / %.1f MB", mbDownloaded, mbTotal)
                } else {
                    String.format("%.1f MB", mbDownloaded)
                }
            } else if (isExternal) {
                "Saving to Downloads/AnimeX/"
            } else {
                task.formattedSize
            }

            Text(
                text = transferredText,
                color = TextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Accordion Card for Completed In-App Downloads (Anime-Wise)
 */
@Composable
private fun AnimeDownloadAccordionCard(
    group: AnimeDownloadGroup,
    onPlayOffline: (localPath: String, animeTitle: String, episodeNumber: Int) -> Unit,
    onDeleteEpisode: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianCard)
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
    ) {
        // Group Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 46.dp, height = 64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(VoidDark)
            ) {
                AsyncImage(
                    model = NetworkClient.resolvePosterUrl(group.animePoster),
                    contentDescription = group.animeTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.animeTitle,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${group.completedCount} EPISODES DOWNLOADED",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Total Space: ${group.formattedTotalSize}",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(24.dp)
            )
        }

        // Expanded Episode List
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VoidDark.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                group.tasks.forEach { task ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ObsidianCard)
                            .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.fileName,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${task.quality} • ${task.audioTrack.uppercase()} • ${task.formattedSize}",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Play Offline Button
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan.copy(alpha = 0.2f))
                                    .border(1.dp, NeonCyan, CircleShape)
                                    .clickable {
                                        onPlayOffline(task.targetPath, task.animeTitle, task.episodeNumber)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play Offline",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Delete Episode Button
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(NeonMagenta.copy(alpha = 0.15f))
                                    .border(1.dp, NeonMagenta, CircleShape)
                                    .clickable { onDeleteEpisode(task.id) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = NeonMagenta,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
