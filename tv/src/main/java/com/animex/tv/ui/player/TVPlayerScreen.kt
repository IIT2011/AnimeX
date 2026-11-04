package com.animex.tv.ui.player

import android.net.Uri
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import coil.compose.rememberAsyncImagePainter
import com.animex.tv.core.model.EpisodeItemDto
import com.animex.tv.core.model.PlayRequest
import com.animex.tv.core.model.PlayResponseDto
import com.animex.tv.core.model.PlaybackSourceDto
import com.animex.tv.core.model.SubtitleDto
import com.animex.tv.core.model.WatchHistoryEntry
import com.animex.tv.core.network.NetworkClient
import com.animex.tv.core.session.UserSessionRepository
import com.animex.tv.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun TVPlayerScreen(
    animeId: Long? = null,
    episodeId: Long,
    episodeNumber: Int,
    animeTitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentEpId by remember { mutableStateOf(episodeId) }
    var currentEpNum by remember { mutableStateOf(episodeNumber) }
    var currentAudioTrack by remember { mutableStateOf("sub") }

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var playResponse by remember { mutableStateOf<PlayResponseDto?>(null) }
    var selectedSource by remember { mutableStateOf<PlaybackSourceDto?>(null) }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    // UI overlays
    var showControls by remember { mutableStateOf(false) }
    var showEpisodeDrawer by remember { mutableStateOf(false) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var showAudioMenu by remember { mutableStateOf(false) }
    var showSubtitleMenu by remember { mutableStateOf(false) }

    // Settings
    var selectedQuality by remember { mutableStateOf("Auto") }
    var selectedSubtitle by remember { mutableStateOf<SubtitleDto?>(null) }
    var episodeList by remember { mutableStateOf<List<EpisodeItemDto>>(emptyList()) }

    // Seeking & Crunchyroll-style Frame Scrubbing
    var seekNotice by remember { mutableStateOf<String?>(null) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableLongStateOf(0L) }
    var scrubHoldCount by remember { mutableIntStateOf(0) }
    var scrubDirection by remember { mutableStateOf("FORWARD") }

    val playPauseRequester = remember { FocusRequester() }

    // High performance TV LoadControl (lightning fast 1s start, 60s deep buffer ahead)
    val loadControl = remember {
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 60_000,
                /* bufferForPlaybackMs = */ 1_000,
                /* bufferForPlaybackAfterRebufferMs = */ 2_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    // Media3 ExoPlayer Instance
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build().apply {
                playWhenReady = true
            }
    }

    // Auto-hide controls timer
    LaunchedEffect(showControls) {
        if (showControls && !showEpisodeDrawer && !showQualityMenu && !showAudioMenu && !showSubtitleMenu) {
            delay(5000)
            showControls = false
        }
    }

    // Focus play/pause button when controls open
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(100)
            try {
                playPauseRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    // Fetch episodes for episode picker drawer
    LaunchedEffect(animeId) {
        if (animeId != null && animeId > 0) {
            try {
                val res = NetworkClient.api.getEpisodes(animeId)
                episodeList = res.data
            } catch (_: Exception) {}
        }
    }

    // Periodic position updater
    LaunchedEffect(exoPlayer) {
        while (isActive) {
            if (exoPlayer.isPlaying && !isScrubbing) {
                currentPosition = exoPlayer.currentPosition
                duration = exoPlayer.duration.coerceAtLeast(0L)
            }
            delay(500)
        }
    }

    // Stream loader function
    fun loadStream(epId: Long, track: String, seekToMs: Long = 0L) {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val res = NetworkClient.api.playEpisode(epId, PlayRequest(track = track))
                playResponse = res
                val src = res.sources.firstOrNull { it.url != null }
                if (src != null) {
                    selectedSource = src
                    currentAudioTrack = track

                    val okHttpClient = NetworkClient.createMedia3OkHttpClient(src.mediaHeaders)
                    val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)

                    // Attach companion subtitles to MediaItem so ExoPlayer renders them properly
                    val subConfigs = src.subtitles.map { sub ->
                        val mime = if (sub.url.endsWith(".srt", ignoreCase = true)) {
                            MimeTypes.APPLICATION_SUBRIP
                        } else {
                            MimeTypes.TEXT_VTT
                        }
                        MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.url))
                            .setMimeType(mime)
                            .setLanguage(sub.lang)
                            .setLabel(sub.label)
                            .setSelectionFlags(if (sub.default) C.SELECTION_FLAG_DEFAULT else 0)
                            .build()
                    }

                    // Set default subtitle
                    if (selectedSubtitle == null) {
                        selectedSubtitle = src.subtitles.firstOrNull { it.default } ?: src.subtitles.firstOrNull()
                    }

                    val mediaItem = MediaItem.Builder()
                        .setUri(src.url)
                        .setMimeType(MimeTypes.APPLICATION_M3U8)
                        .setSubtitleConfigurations(subConfigs)
                        .build()

                    val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(mediaItem)

                    exoPlayer.setMediaSource(mediaSource)
                    exoPlayer.prepare()
                    if (seekToMs > 0L) {
                        exoPlayer.seekTo(seekToMs)
                    }
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
    }

    // Initial episode load
    LaunchedEffect(currentEpId) {
        loadStream(currentEpId, currentAudioTrack)
    }

    // Player event listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        duration = exoPlayer.duration.coerceAtLeast(0L)
                        isLoading = false
                    }
                    Player.STATE_BUFFERING -> {
                        isLoading = true
                    }
                    Player.STATE_ENDED -> {
                        showControls = true
                    }
                    else -> {}
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
                        animeId = animeId ?: 0L,
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
        when {
            showEpisodeDrawer -> showEpisodeDrawer = false
            showQualityMenu -> showQualityMenu = false
            showAudioMenu -> showAudioMenu = false
            showSubtitleMenu -> showSubtitleMenu = false
            showControls -> showControls = false
            isScrubbing -> isScrubbing = false
            else -> onBack()
        }
    }

    // Root Container with D-pad & Media Remote key handling
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusable()
            .onKeyEvent { keyEvent ->
                val native = keyEvent.nativeKeyEvent
                val isDown = native.action == KeyEvent.ACTION_DOWN
                val isUp = native.action == KeyEvent.ACTION_UP
                val repeat = native.repeatCount

                // Handle global play/pause media buttons regardless of focus
                if (isDown && (native.keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
                               native.keyCode == KeyEvent.KEYCODE_MEDIA_PLAY ||
                               native.keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE)) {
                    if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                    return@onKeyEvent true
                }

                // If any popup menu is open, let user press BACK or navigate within it
                if (showQualityMenu || showAudioMenu || showSubtitleMenu || showEpisodeDrawer) {
                    if (isDown && native.keyCode == KeyEvent.KEYCODE_BACK) {
                        showQualityMenu = false
                        showAudioMenu = false
                        showSubtitleMenu = false
                        showEpisodeDrawer = false
                        return@onKeyEvent true
                    }
                    return@onKeyEvent false
                }

                // When HUD is NOT showing and NOT scrubbing:
                if (!showControls && !isScrubbing) {
                    if (isDown) {
                        when (native.keyCode) {
                            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_SPACE -> {
                                if (isPlaying) {
                                    exoPlayer.pause()
                                    seekNotice = "PAUSE"
                                } else {
                                    exoPlayer.play()
                                    seekNotice = "PLAY"
                                }
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                if (repeat > 0) {
                                    // Holding Left: Start Crunchyroll-style Frame/Time Scrubbing Mode
                                    isScrubbing = true
                                    scrubHoldCount = repeat
                                    scrubDirection = "REWIND"
                                    val step = if (repeat < 5) 5000L else if (repeat < 15) 15000L else 30000L
                                    scrubPositionMs = (currentPosition - (step * repeat)).coerceAtLeast(0L)
                                    true
                                } else {
                                    // Single click Left: Seek -10s
                                    val target = (exoPlayer.currentPosition - 10000).coerceAtLeast(0L)
                                    exoPlayer.seekTo(target)
                                    currentPosition = target
                                    seekNotice = "-10s"
                                    true
                                }
                            }
                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                if (repeat > 0) {
                                    // Holding Right: Start Crunchyroll-style Frame/Time Scrubbing Mode
                                    isScrubbing = true
                                    scrubHoldCount = repeat
                                    scrubDirection = "FORWARD"
                                    val step = if (repeat < 5) 5000L else if (repeat < 15) 15000L else 30000L
                                    scrubPositionMs = (currentPosition + (step * repeat)).coerceAtMost(duration)
                                    true
                                } else {
                                    // Single click Right: Seek +10s
                                    val target = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
                                    exoPlayer.seekTo(target)
                                    currentPosition = target
                                    seekNotice = "+10s"
                                    true
                                }
                            }
                            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                                // Show controls HUD and focus play/pause button
                                showControls = true
                                true
                            }
                            else -> false
                        }
                    } else false
                } else if (isScrubbing) {
                    // In Crunchyroll scrubbing mode:
                    if (isDown) {
                        when (native.keyCode) {
                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                scrubDirection = "REWIND"
                                scrubHoldCount++
                                val step = if (scrubHoldCount < 5) 5000L else if (scrubHoldCount < 15) 15000L else 30000L
                                scrubPositionMs = (scrubPositionMs - step).coerceAtLeast(0L)
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                scrubDirection = "FORWARD"
                                scrubHoldCount++
                                val step = if (scrubHoldCount < 5) 5000L else if (scrubHoldCount < 15) 15000L else 30000L
                                scrubPositionMs = (scrubPositionMs + step).coerceAtMost(duration)
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                                exoPlayer.seekTo(scrubPositionMs)
                                currentPosition = scrubPositionMs
                                isScrubbing = false
                                exoPlayer.play()
                                true
                            }
                            else -> false
                        }
                    } else if (isUp && (native.keyCode == KeyEvent.KEYCODE_DPAD_LEFT || native.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)) {
                        // Key released: Commit seek and continue playback
                        exoPlayer.seekTo(scrubPositionMs)
                        currentPosition = scrubPositionMs
                        isScrubbing = false
                        exoPlayer.play()
                        true
                    } else false
                } else {
                    // Controls are showing: let child buttons receive focus and handle DPAD_LEFT/RIGHT
                    false
                }
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
                    subtitleView?.setUserDefaultStyle()
                    subtitleView?.setUserDefaultTextSize()
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

        // Quick Seek Notice (-10s / +10s / PLAY / PAUSE)
        LaunchedEffect(seekNotice) {
            if (seekNotice != null) {
                delay(1200)
                seekNotice = null
            }
        }
        AnimatedVisibility(
            visible = seekNotice != null && !isScrubbing,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberVoid.copy(alpha = 0.92f))
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

        // Crunchyroll-style Frame & Time Scrubbing Interface
        AnimatedVisibility(
            visible = isScrubbing,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 36.dp, start = 48.dp, end = 48.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyberVoid.copy(alpha = 0.96f))
                    .border(2.dp, CyberCyan, RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                // Frame preview card with scrub time
                Box(
                    modifier = Modifier
                        .width(260.dp)
                        .height(146.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberSurfaceDark)
                        .border(1.5.dp, CyberCyan.copy(alpha = 0.8f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (scrubDirection == "FORWARD") Icons.Default.FastForward else Icons.Default.FastRewind,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = formatTime(scrubPositionMs),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Time Indicator Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(scrubPositionMs),
                        color = CyberCyan,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        text = if (scrubDirection == "FORWARD") "FAST FORWARD >>" else "<< REWIND",
                        color = CyberMagenta,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )

                    Text(
                        text = formatTime(duration),
                        color = CyberTextSecondary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrubbing Progress Bar
                val scrubProgress = if (duration > 0) (scrubPositionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(CyberCardBg)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(scrubProgress)
                            .background(Brush.horizontalGradient(listOf(CyberCyan, CyberMagenta)))
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Release remote arrow or press OK to resume playback",
                    color = CyberTextSecondary,
                    fontSize = 11.sp,
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
            visible = showControls && !isScrubbing,
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

                // Top Header HUD: Back button, Title, Episode badge (NO "ULTRA HD")
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
                            text = "EPISODE $currentEpNum • ${currentAudioTrack.uppercase()}",
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
                        RemoteHintTag("◀/▶ CLICK: ±10s")
                        RemoteHintTag("◀/▶ HOLD: SCRUB")
                        RemoteHintTag("OK: PLAY/PAUSE")
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

                        Spacer(modifier = Modifier.width(16.dp))

                        TVPlayPauseButton(
                            isPlaying = isPlaying,
                            focusRequester = playPauseRequester,
                            onClick = {
                                if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                            }
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        TVRemoteButton(
                            icon = Icons.Default.Forward10,
                            label = "+10s",
                            onClick = {
                                val target = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
                                exoPlayer.seekTo(target)
                            }
                        )

                        Spacer(modifier = Modifier.width(28.dp))

                        // True Working Quality Picker
                        TVRemoteButton(
                            icon = Icons.Default.Settings,
                            label = selectedQuality.uppercase(),
                            onClick = {
                                showQualityMenu = true
                                showAudioMenu = false
                                showSubtitleMenu = false
                                showEpisodeDrawer = false
                            }
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // True Working Audio Track Switcher (SUB / DUB)
                        TVRemoteButton(
                            icon = Icons.Default.VolumeUp,
                            label = currentAudioTrack.uppercase(),
                            onClick = {
                                showAudioMenu = true
                                showQualityMenu = false
                                showSubtitleMenu = false
                                showEpisodeDrawer = false
                            }
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // True Working Subtitle Switcher
                        TVRemoteButton(
                            icon = Icons.Default.Subtitles,
                            label = if (selectedSubtitle != null) "CC: ON" else "CC: OFF",
                            onClick = {
                                showSubtitleMenu = true
                                showQualityMenu = false
                                showAudioMenu = false
                                showEpisodeDrawer = false
                            }
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // True Working Episodes Drawer
                        TVRemoteButton(
                            icon = Icons.Default.VideoLibrary,
                            label = "EPISODES",
                            onClick = {
                                showEpisodeDrawer = true
                                showQualityMenu = false
                                showAudioMenu = false
                                showSubtitleMenu = false
                            }
                        )
                    }
                }
            }
        }

        // Quality Selection Dialog
        if (showQualityMenu) {
            TVOptionPickerDialog(
                title = "SELECT VIDEO QUALITY",
                options = listOf("Auto", "1080p", "720p", "480p", "360p"),
                selectedOption = selectedQuality,
                onDismiss = { showQualityMenu = false },
                onSelectOption = { q ->
                    selectedQuality = q
                    if (q == "Auto") {
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .clearVideoSizeConstraints()
                            .build()
                    } else {
                        val h = q.removeSuffix("p").toIntOrNull() ?: 720
                        val w = (h * 16) / 9
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .setMaxVideoSize(w, h)
                            .setMinVideoSize(w, h)
                            .build()
                    }
                    showQualityMenu = false
                }
            )
        }

        // Audio Track Selection Dialog (SUB vs DUB)
        if (showAudioMenu) {
            val availableAudio = playResponse?.tracks?.ifEmpty { listOf("sub", "dub") } ?: listOf("sub", "dub")
            TVOptionPickerDialog(
                title = "SELECT AUDIO TRACK",
                options = availableAudio.map { if (it.equals("sub", ignoreCase = true)) "SUB (Original Audio)" else "DUB (English Dubbed)" },
                selectedOption = if (currentAudioTrack.equals("sub", ignoreCase = true)) "SUB (Original Audio)" else "DUB (English Dubbed)",
                onDismiss = { showAudioMenu = false },
                onSelectOption = { chosen ->
                    val newTrack = if (chosen.contains("SUB", ignoreCase = true)) "sub" else "dub"
                    if (!newTrack.equals(currentAudioTrack, ignoreCase = true)) {
                        loadStream(currentEpId, newTrack, exoPlayer.currentPosition)
                    }
                    showAudioMenu = false
                }
            )
        }

        // Subtitles Selection Dialog
        if (showSubtitleMenu) {
            val subs = selectedSource?.subtitles ?: emptyList()
            val subOptions = mutableListOf("Off")
            subs.forEach { subOptions.add(it.label) }

            TVOptionPickerDialog(
                title = "SELECT SUBTITLES",
                options = subOptions,
                selectedOption = selectedSubtitle?.label ?: "Off",
                onDismiss = { showSubtitleMenu = false },
                onSelectOption = { chosen ->
                    if (chosen.equals("Off", ignoreCase = true)) {
                        selectedSubtitle = null
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                            .build()
                    } else {
                        val found = subs.firstOrNull { it.label == chosen }
                        if (found != null) {
                            selectedSubtitle = found
                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                .buildUpon()
                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                .setPreferredTextLanguage(found.lang)
                                .build()
                        }
                    }
                    showSubtitleMenu = false
                }
            )
        }

        // Episodes Drawer Overlay
        if (showEpisodeDrawer) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CyberVoid)
                        .border(2.dp, CyberCyan, RoundedCornerShape(16.dp))
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SELECT EPISODE",
                            color = CyberTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )

                        TVRemoteButton(
                            icon = Icons.Default.Close,
                            label = "CLOSE",
                            onClick = { showEpisodeDrawer = false }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val epsToDisplay = if (episodeList.isNotEmpty()) {
                        episodeList
                    } else {
                        (1..24).map { EpisodeItemDto(id = currentEpId, number = it, title = "Episode $it") }
                    }

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(epsToDisplay) { ep ->
                            val isCurrent = ep.number == currentEpNum
                            TVEpisodePickerCard(
                                episode = ep,
                                isCurrent = isCurrent,
                                onClick = {
                                    if (ep.number != currentEpNum) {
                                        currentEpNum = ep.number
                                        currentEpId = ep.id
                                        loadStream(ep.id, currentAudioTrack)
                                    }
                                    showEpisodeDrawer = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TVOptionPickerDialog(
    title: String,
    options: List<String>,
    selectedOption: String,
    onDismiss: () -> Unit,
    onSelectOption: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(420.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CyberVoid)
                .border(2.dp, CyberCyan, RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = CyberTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                TVRemoteButton(
                    icon = Icons.Default.Close,
                    label = "CLOSE",
                    onClick = onDismiss
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(options) { opt ->
                    val isSelected = opt == selectedOption
                    val interactionSource = remember { MutableInteractionSource() }
                    val isFocused by interactionSource.collectIsFocusedAsState()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    isFocused -> CyberCyan.copy(alpha = 0.35f)
                                    isSelected -> CyberCardBg
                                    else -> CyberSurfaceDark
                                }
                            )
                            .border(
                                width = if (isFocused) 2.dp else 1.dp,
                                color = if (isFocused) CyberCyan else if (isSelected) CyberCyan.copy(alpha = 0.6f) else CyberBorderSubtle,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .focusable(interactionSource = interactionSource)
                            .clickable(interactionSource = interactionSource, indication = null) {
                                onSelectOption(opt)
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = opt,
                                color = if (isSelected || isFocused) CyberCyan else CyberTextPrimary,
                                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(18.dp)
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
private fun TVEpisodePickerCard(
    episode: EpisodeItemDto,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .width(160.dp)
            .height(110.dp)
            .scale(if (isFocused) 1.08f else 1.0f)
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isFocused -> CyberCyan.copy(alpha = 0.3f)
                    isCurrent -> CyberMagenta.copy(alpha = 0.25f)
                    else -> CyberCardBg
                }
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) CyberCyan else if (isCurrent) CyberMagenta else CyberBorderSubtle,
                shape = RoundedCornerShape(10.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "EP ${episode.number}",
                    color = if (isCurrent) CyberMagenta else CyberCyan,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CyberMagenta)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(text = "PLAYING", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text(
                text = episode.title ?: "Episode ${episode.number}",
                color = CyberTextSecondary,
                fontSize = 11.sp,
                maxLines = 2
            )
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
            .background(if (isFocused) CyberCyan.copy(alpha = 0.35f) else CyberCardBg)
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
