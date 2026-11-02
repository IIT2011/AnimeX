package com.animex.tv.ui.player

import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import com.animex.tv.core.model.EpisodeItemDto
import com.animex.tv.core.model.PlayRequest
import com.animex.tv.core.model.PlaybackSourceDto
import com.animex.tv.core.model.WatchHistoryEntry
import com.animex.tv.core.network.NetworkClient
import com.animex.tv.core.session.UserSessionRepository
import com.animex.tv.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun TVPlayerScreen(
    episodeId: Long,
    episodeNumber: Int,
    animeTitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var currentEpId by remember { mutableStateOf(episodeId) }
    var currentEpNum by remember { mutableStateOf(episodeNumber) }

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var playbackSources by remember { mutableStateOf<List<PlaybackSourceDto>>(emptyList()) }
    var selectedSource by remember { mutableStateOf<PlaybackSourceDto?>(null) }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    var showControls by remember { mutableStateOf(true) }
    var showEpisodeDrawer by remember { mutableStateOf(false) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var seekNotice by remember { mutableStateOf<String?>(null) }

    val playPauseRequester = remember { FocusRequester() }

    // ExoPlayer Instance
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    // Auto-hide controls timer
    LaunchedEffect(showControls) {
        if (showControls && !showEpisodeDrawer && !showQualityMenu) {
            delay(4000)
            showControls = false
        }
    }

    // Periodic position updater
    LaunchedEffect(exoPlayer) {
        while (isActive) {
            if (exoPlayer.isPlaying) {
                currentPosition = exoPlayer.currentPosition
                duration = exoPlayer.duration.coerceAtLeast(0L)
            }
            delay(500)
        }
    }

    // Load Stream Data
    LaunchedEffect(currentEpId) {
        isLoading = true
        errorMessage = null
        try {
            val playRes = NetworkClient.api.playEpisode(currentEpId, PlayRequest(track = "sub"))
            playbackSources = playRes.sources
            val src = playRes.sources.firstOrNull { it.url != null }
            if (src != null) {
                selectedSource = src
                val okHttpClient = NetworkClient.createMedia3OkHttpClient(src.mediaHeaders)
                val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
                val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(src.url!!))

                exoPlayer.setMediaSource(mediaSource)
                exoPlayer.prepare()
                exoPlayer.play()
                isLoading = false
            } else {
                errorMessage = "No valid streaming server found for this episode."
                isLoading = false
            }
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Failed to initialize video stream."
            isLoading = false
        }
    }

    // Player event listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    duration = exoPlayer.duration.coerceAtLeast(0L)
                    isLoading = false
                } else if (playbackState == Player.STATE_BUFFERING) {
                    isLoading = true
                } else if (playbackState == Player.STATE_ENDED) {
                    showControls = true
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Save watch history on leave
    DisposableEffect(Unit) {
        onDispose {
            if (duration > 0) {
                val progress = (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                UserSessionRepository.recordHistory(
                    context,
                    WatchHistoryEntry(
                        animeId = 0L,
                        title = animeTitle,
                        poster = null,
                        episodeId = currentEpId,
                        episodeNumber = currentEpNum,
                        progressPercent = progress
                    )
                )
            }
        }
    }

    BackHandler {
        if (showEpisodeDrawer) {
            showEpisodeDrawer = false
        } else if (showQualityMenu) {
            showQualityMenu = false
        } else if (showControls) {
            showControls = false
        } else {
            onBack()
        }
    }

    // TV Remote D-pad key event handler
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            if (!showControls) {
                                showControls = true
                                true
                            } else {
                                false
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            showControls = true
                            val target = (exoPlayer.currentPosition - 10000).coerceAtLeast(0L)
                            exoPlayer.seekTo(target)
                            seekNotice = "-10s"
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            showControls = true
                            val target = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
                            exoPlayer.seekTo(target)
                            seekNotice = "+10s"
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            showEpisodeDrawer = !showEpisodeDrawer
                            showControls = true
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            showQualityMenu = !showQualityMenu
                            showControls = true
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .clickable {
                showControls = !showControls
            }
    ) {
        // Fullscreen ExoPlayer View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Loading spinner
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CircularProgressIndicator(color = CyberCyan, strokeWidth = 3.5.dp)
                    Text(
                        text = "BUFFERING STREAM...",
                        color = CyberCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // Seek Notice (-10s / +10s)
        LaunchedEffect(seekNotice) {
            if (seekNotice != null) {
                delay(1200)
                seekNotice = null
            }
        }
        AnimatedVisibility(
            visible = seekNotice != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberVoid.copy(alpha = 0.9f))
                    .border(2.dp, CyberCyan, RoundedCornerShape(12.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = seekNotice ?: "",
                    color = CyberCyan,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Error Banner
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberVoid.copy(alpha = 0.95f))
                    .border(2.dp, CyberMagenta, RoundedCornerShape(12.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = CyberMagenta,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = CyberTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Cyber-HUD 10-foot TV Remote Overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Gradient Vignette
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(CyberVoid.copy(alpha = 0.95f), Color.Transparent)
                            )
                        )
                        .align(Alignment.TopCenter)
                )

                // Bottom Gradient Vignette
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, CyberVoid.copy(alpha = 0.98f))
                            )
                        )
                        .align(Alignment.BottomCenter)
                )

                // Top Header HUD: Back button, Title, Episode badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 36.dp, vertical = 24.dp)
                        .align(Alignment.TopStart),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TVRemoteButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        label = "BACK",
                        onClick = onBack
                    )

                    Column {
                        Text(
                            text = animeTitle,
                            color = CyberTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "EPISODE $currentEpNum • SUB • ULTRA HD",
                            color = CyberCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Remote D-pad Quick Hints
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RemoteHintTag("▲ EPISODES")
                        RemoteHintTag("▼ QUALITY")
                        RemoteHintTag("◀/▶ SEEK")
                    }
                }

                // Bottom Controls HUD
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 48.dp, end = 48.dp, bottom = 28.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    // Progress Bar & Time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = formatTime(currentPosition),
                            color = CyberCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )

                        val progress = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()) else 0f
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(CyberCardBg)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                                    .background(
                                        Brush.horizontalGradient(listOf(CyberCyan, CyberMagenta))
                                    )
                            )
                        }

                        Text(
                            text = formatTime(duration),
                            color = CyberTextSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Remote D-pad Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TVRemoteButton(
                            icon = Icons.Default.Replay10,
                            label = "-10s",
                            onClick = {
                                val target = (exoPlayer.currentPosition - 10000).coerceAtLeast(0L)
                                exoPlayer.seekTo(target)
                            }
                        )

                        Spacer(modifier = Modifier.width(20.dp))

                        TVPlayPauseButton(
                            isPlaying = isPlaying,
                            focusRequester = playPauseRequester,
                            onClick = {
                                if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                            }
                        )

                        Spacer(modifier = Modifier.width(20.dp))

                        TVRemoteButton(
                            icon = Icons.Default.Forward10,
                            label = "+10s",
                            onClick = {
                                val target = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
                                exoPlayer.seekTo(target)
                            }
                        )

                        Spacer(modifier = Modifier.width(36.dp))

                        TVRemoteButton(
                            icon = Icons.Default.VideoLibrary,
                            label = "EPISODES",
                            onClick = { showEpisodeDrawer = !showEpisodeDrawer }
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        TVRemoteButton(
                            icon = Icons.Default.Settings,
                            label = "1080P",
                            onClick = { showQualityMenu = !showQualityMenu }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RemoteHintTag(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CyberSurfaceDark.copy(alpha = 0.9f))
            .border(1.dp, CyberBorderSubtle, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = CyberTextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun TVPlayPauseButton(
    isPlaying: Boolean,
    focusRequester: FocusRequester,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(if (isFocused) 1.15f else 1.0f, label = "playBtnScale")

    Box(
        modifier = Modifier
            .scale(scale)
            .size(56.dp)
            .clip(CircleShape)
            .background(
                if (isFocused) CyberCyan else CyberCyan.copy(alpha = 0.85f)
            )
            .border(
                width = if (isFocused) 3.dp else 1.dp,
                color = if (isFocused) Color.White else Color.Transparent,
                shape = CircleShape
            )
            .focusRequester(focusRequester)
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = Color.Black,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun TVRemoteButton(
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
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
