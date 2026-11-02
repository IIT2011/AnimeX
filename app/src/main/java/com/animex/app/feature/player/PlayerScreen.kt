package com.animex.app.feature.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import com.animex.app.core.designsystem.*
import com.animex.app.core.network.NetworkClient
import com.animex.app.core.network.model.PlaybackSourceDto
import com.animex.app.core.network.model.SubtitleDto
import com.animex.app.core.session.UserSessionRepository
import kotlinx.coroutines.delay

enum class SeekSide { LEFT, RIGHT }
enum class SettingsMenu { MAIN, QUALITY, SUBTITLE, AUDIO, SPEED }

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    episodeId: Long,
    animeTitle: String? = null,
    episodeNumber: Int? = null,
    poster: String? = null,
    localPath: String? = null,
    onBackClick: () -> Unit,
    viewModel: PlayerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isOffline = remember(localPath) {
        !localPath.isNullOrBlank() && java.io.File(localPath).exists()
    }

    LaunchedEffect(episodeId, isOffline) {
        if (!isOffline && episodeId > 0) {
            viewModel.loadEpisodeStream(episodeId)
        }
    }

    val effectiveState = remember(uiState, isOffline, localPath) {
        if (isOffline) {
            val file = java.io.File(localPath!!)
            val parent = file.parentFile
            val base = file.nameWithoutExtension
            val localSubs = mutableListOf<SubtitleDto>()
            if (parent != null && parent.exists()) {
                val vtt = java.io.File(parent, "$base.vtt")
                val srt = java.io.File(parent, "$base.srt")
                val enSrt = java.io.File(parent, "$base.en.srt")
                if (vtt.exists()) {
                    localSubs.add(SubtitleDto(lang = "en", label = "English (VTT)", url = "file://${vtt.absolutePath}", default = true))
                }
                if (srt.exists()) {
                    localSubs.add(SubtitleDto(lang = "en", label = "English (SRT)", url = "file://${srt.absolutePath}", default = localSubs.isEmpty()))
                } else if (enSrt.exists()) {
                    localSubs.add(SubtitleDto(lang = "en", label = "English (SRT)", url = "file://${enSrt.absolutePath}", default = localSubs.isEmpty()))
                }
            }

            val localSource = PlaybackSourceDto(
                kind = "local",
                url = "file://${file.absolutePath}",
                server = "Local Offline Storage",
                subtitles = localSubs
            )
            PlayerUiState.Ready(
                playData = com.animex.app.core.network.model.PlayResponseDto(
                    episodeId = episodeId,
                    sources = listOf(localSource)
                ),
                activeSource = localSource,
                currentTrack = "offline"
            )
        } else {
            uiState
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidDark)
    ) {
        when (val state = effectiveState) {
            is PlayerUiState.Loading -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = NeonCyan)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "ESTABLISHING QUANTUM VIDEO STREAM...",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            }
            is PlayerUiState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "STREAM NODE UNREACHABLE", color = NeonMagenta, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = state.message, color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    CyberButton(text = "RECONNECT", onClick = { viewModel.loadEpisodeStream(episodeId) })
                }
            }
            is PlayerUiState.Ready -> {
                CyberVideoSurface(
                    ready = state,
                    animeTitle = animeTitle,
                    episodeNumber = episodeNumber,
                    poster = poster,
                    onBackClick = onBackClick,
                    onSwitchAudio = { newTrack, pos ->
                        viewModel.switchAudioTrack(newTrack, pos)
                    }
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun CyberVideoSurface(
    ready: PlayerUiState.Ready,
    animeTitle: String? = null,
    episodeNumber: Int? = null,
    poster: String? = null,
    onBackClick: () -> Unit,
    onSwitchAudio: (String, Long) -> Unit
) {
    val source = ready.activeSource
    val context = LocalContext.current
    val activity = context as? Activity

    val displayTitle = when {
        !animeTitle.isNullOrBlank() -> animeTitle
        source.server != null && !source.server.contains("anifam", ignoreCase = true) -> source.server
        else -> "AnimeX Stream"
    }
    val displayEpText = when {
        episodeNumber != null && episodeNumber > 0 -> "EPISODE $episodeNumber"
        else -> "STREAM"
    }

    // Playback and UI States
    var isPlaying by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isFullscreen by remember { mutableStateOf(false) }

    // Double Tap Seek State
    var seekSide by remember { mutableStateOf<SeekSide?>(null) }
    var seekSeconds by remember { mutableIntStateOf(0) }
    var lastTapTimestamp by remember { mutableLongStateOf(0L) }

    // Settings State
    var showSettingsModal by remember { mutableStateOf(false) }
    var currentSettingsMenu by remember { mutableStateOf(SettingsMenu.MAIN) }
    var selectedQuality by remember { mutableStateOf("Auto") }
    var selectedSubtitle by remember {
        mutableStateOf<SubtitleDto?>(
            source.subtitles.firstOrNull { it.default } ?: source.subtitles.firstOrNull()
        )
    }
    var currentPlaybackSpeed by remember { mutableFloatStateOf(1.0f) }

    // Handle system fullscreen / landscape orientation
    DisposableEffect(isFullscreen) {
        activity?.let { act ->
            val windowInsetsController = WindowCompat.getInsetsController(act.window, act.window.decorView)
            if (isFullscreen) {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                windowInsetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            activity?.let { act ->
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                val windowInsetsController = WindowCompat.getInsetsController(act.window, act.window.decorView)
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Intercept Back button when in Fullscreen
    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    // Build ExoPlayer with HLS / Local source & Subtitles
    val exoPlayer = remember(source.url) {
        val isLocal = source.kind == "local" || source.url?.startsWith("file://") == true
        if (isLocal) {
            val localSubs = source.subtitles.map { sub ->
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
            ExoPlayer.Builder(context).build().apply {
                val mediaItem = MediaItem.Builder()
                    .setUri(Uri.parse(source.url))
                    .setSubtitleConfigurations(localSubs)
                    .build()
                setMediaItem(mediaItem)
                prepare()
                if (ready.initialSeekMs > 0L) {
                    seekTo(ready.initialSeekMs)
                }
                playWhenReady = true
            }
        } else {
            val okHttpClient = NetworkClient.createMedia3OkHttpClient(source.mediaHeaders)
            val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)

            val subtitleConfigs = source.subtitles.map { sub ->
                MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.url))
                    .setMimeType(MimeTypes.TEXT_VTT)
                    .setLanguage(sub.lang)
                    .setLabel(sub.label)
                    .setSelectionFlags(if (sub.default) C.SELECTION_FLAG_DEFAULT else 0)
                    .build()
            }

            val mediaItem = MediaItem.Builder()
                .setUri(source.url)
                .setMimeType(MimeTypes.APPLICATION_M3U8)
                .setSubtitleConfigurations(subtitleConfigs)
                .build()

            val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)

            ExoPlayer.Builder(context).build().apply {
                setMediaSource(mediaSource)
                prepare()
                if (ready.initialSeekMs > 0L) {
                    seekTo(ready.initialSeekMs)
                }
                playWhenReady = true
            }
        }
    }

    // Progress Loop & Auto-record to Continue Watching
    LaunchedEffect(exoPlayer) {
        var tick = 0
        while (true) {
            currentPosition = exoPlayer.currentPosition
            duration = exoPlayer.duration.coerceAtLeast(0L)
            isPlaying = exoPlayer.isPlaying

            tick++
            if (tick % 10 == 0 && duration > 0 && currentPosition > 3000) {
                UserSessionRepository.recordPlaybackProgress(
                    context = context,
                    animeId = ready.playData.episodeId,
                    title = displayTitle,
                    poster = poster,
                    episodeId = ready.playData.episodeId,
                    episodeNumber = episodeNumber ?: 1,
                    currentPosMs = currentPosition,
                    durationMs = duration
                )
            }
            delay(500)
        }
    }

    // Controls Auto-Hide
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(4000)
            showControls = false
        }
    }

    // Double Tap Auto-Fadeout (YouTube behavior)
    LaunchedEffect(lastTapTimestamp) {
        if (lastTapTimestamp > 0) {
            delay(750)
            seekSide = null
            seekSeconds = 0
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (duration > 0 && currentPosition > 3000) {
                UserSessionRepository.recordPlaybackProgress(
                    context = context,
                    animeId = ready.playData.episodeId,
                    title = displayTitle,
                    poster = poster,
                    episodeId = ready.playData.episodeId,
                    episodeNumber = episodeNumber ?: 1,
                    currentPosMs = currentPosition,
                    durationMs = duration
                )
            }
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        // Single tap toggles HUD controls ONLY when not currently double-tapping
                        if (seekSide == null) {
                            showControls = !showControls
                        }
                    },
                    onDoubleTap = { offset ->
                        val now = System.currentTimeMillis()
                        val isRightSide = offset.x >= (size.width / 2)
                        val side = if (isRightSide) SeekSide.RIGHT else SeekSide.LEFT

                        // HIDE all HUD controls while fast forwarding / rewinding (YouTube behavior)
                        showControls = false

                        // Loop / accumulate seconds on consecutive taps within 750ms
                        if (seekSide == side && (now - lastTapTimestamp) < 750) {
                            seekSeconds += 10
                        } else {
                            seekSide = side
                            seekSeconds = 10
                        }
                        lastTapTimestamp = now

                        // Perform actual seek
                        if (isRightSide) {
                            exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration))
                        } else {
                            exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0))
                        }
                    }
                )
            }
    ) {
        // Video Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Neon Ambient Glow Frame
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(BorderStroke(2.dp, Brush.verticalGradient(listOf(NeonCyan.copy(alpha = 0.35f), Color.Transparent, NeonMagenta.copy(alpha = 0.3f)))))
        )

        // YouTube-style Animated Double Tap Feedback Overlay (Left / Right)
        AnimatedVisibility(
            visible = seekSide != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(if (seekSide == SeekSide.RIGHT) Alignment.CenterEnd else Alignment.CenterStart)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(180.dp)
                    .background(
                        Brush.horizontalGradient(
                            if (seekSide == SeekSide.RIGHT) {
                                listOf(Color.Transparent, Color(0x6600F0FF))
                            } else {
                                listOf(Color(0x6600F0FF), Color.Transparent)
                            }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (seekSide == SeekSide.RIGHT) Icons.Default.FastForward else Icons.Default.FastRewind,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${seekSeconds}s",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Custom Futuristic HUD Controls (Hidden while double tapping!)
        AnimatedVisibility(
            visible = showControls && seekSide == null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x8007080D))
            ) {
                // Top HUD Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(ObsidianCard)
                                .border(1.dp, GlassBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    if (isFullscreen) isFullscreen = false else onBackClick()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = NeonCyan
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = displayTitle,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$displayEpText • ${ready.currentTrack.uppercase()} • ULTRA HD",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Settings Gear Button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(ObsidianCard)
                                .border(1.dp, GlassBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    currentSettingsMenu = SettingsMenu.MAIN
                                    showSettingsModal = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Fullscreen Landscape Toggle Button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(ObsidianCard)
                                .border(1.dp, GlassBorder, RoundedCornerShape(6.dp))
                                .clickable { isFullscreen = !isFullscreen },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = NeonCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Center Play/Pause & Skip Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0)) }) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = NeonCyan,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(ObsidianCard)
                            .border(2.dp, CyberNeonBrush, CircleShape)
                            .clickable {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = NeonCyan,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    IconButton(onClick = { exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)) }) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10s",
                            tint = NeonCyan,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Bottom HUD: Scrubber & Telemetry
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${formatTime(currentPosition)} / ${formatTime(duration)}",
                            color = NeonCyan,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CyberBadge(
                                text = selectedQuality,
                                accentColor = NeonCyan,
                                modifier = Modifier.clickable {
                                    currentSettingsMenu = SettingsMenu.QUALITY
                                    showSettingsModal = true
                                }
                            )
                            CyberBadge(
                                text = selectedSubtitle?.lang?.uppercase() ?: "CC: OFF",
                                accentColor = if (selectedSubtitle != null) NeonLime else TextSecondary,
                                modifier = Modifier.clickable {
                                    currentSettingsMenu = SettingsMenu.SUBTITLE
                                    showSettingsModal = true
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Slider(
                        value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                        onValueChange = { frac ->
                            val seekTo = (frac * duration).toLong()
                            exoPlayer.seekTo(seekTo)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = ObsidianCard
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Futuristic Settings Modal Dialog
        if (showSettingsModal) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xB307080D))
                    .clickable { showSettingsModal = false },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 340.dp)
                        .fillMaxWidth(0.85f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianCard)
                        .border(1.5.dp, CardBorderGlowBrush, RoundedCornerShape(14.dp))
                        .clickable(enabled = false) {}
                        .padding(18.dp)
                ) {
                    when (currentSettingsMenu) {
                        SettingsMenu.MAIN -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "STREAM CONFIGURATION",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )

                                Divider(color = GlassBorder)

                                SettingsMenuItem(
                                    title = "Video Quality",
                                    currentValue = selectedQuality,
                                    icon = Icons.Default.HighQuality,
                                    onClick = { currentSettingsMenu = SettingsMenu.QUALITY }
                                )

                                SettingsMenuItem(
                                    title = "Subtitles",
                                    currentValue = selectedSubtitle?.label ?: "Off",
                                    icon = Icons.Default.Subtitles,
                                    onClick = { currentSettingsMenu = SettingsMenu.SUBTITLE }
                                )

                                SettingsMenuItem(
                                    title = "Audio Track",
                                    currentValue = ready.currentTrack.uppercase(),
                                    icon = Icons.Default.Audiotrack,
                                    onClick = { currentSettingsMenu = SettingsMenu.AUDIO }
                                )

                                SettingsMenuItem(
                                    title = "Playback Speed",
                                    currentValue = "${currentPlaybackSpeed}x",
                                    icon = Icons.Default.Speed,
                                    onClick = { currentSettingsMenu = SettingsMenu.SPEED }
                                )
                            }
                        }

                        SettingsMenu.QUALITY -> {
                            val qualities = remember(source.qualities) {
                                val list = source.qualities.mapNotNull { it.height?.let { h -> "${h}p" } }
                                if (list.isNotEmpty()) listOf("Auto") + list else listOf("Auto", "1080p", "720p", "480p", "360p")
                            }

                            SettingsSubmenu(
                                title = "SELECT VIDEO QUALITY",
                                onBack = { currentSettingsMenu = SettingsMenu.MAIN }
                            ) {
                                qualities.forEach { q ->
                                    SettingsOptionItem(
                                        title = q,
                                        isSelected = q == selectedQuality,
                                        onClick = {
                                            selectedQuality = q
                                            // Apply actual ExoPlayer track constraints
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
                                            showSettingsModal = false
                                        }
                                    )
                                }
                            }
                        }

                        SettingsMenu.SUBTITLE -> {
                            SettingsSubmenu(
                                title = "SELECT SUBTITLES",
                                onBack = { currentSettingsMenu = SettingsMenu.MAIN }
                            ) {
                                SettingsOptionItem(
                                    title = "Off",
                                    isSelected = selectedSubtitle == null,
                                    onClick = {
                                        selectedSubtitle = null
                                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                            .buildUpon()
                                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                                            .build()
                                        showSettingsModal = false
                                    }
                                )

                                source.subtitles.forEach { sub ->
                                    SettingsOptionItem(
                                        title = sub.label,
                                        isSelected = selectedSubtitle?.url == sub.url,
                                        onClick = {
                                            selectedSubtitle = sub
                                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                                .buildUpon()
                                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                                .setPreferredTextLanguage(sub.lang)
                                                .build()
                                            showSettingsModal = false
                                        }
                                    )
                                }
                            }
                        }

                        SettingsMenu.AUDIO -> {
                            SettingsSubmenu(
                                title = "SELECT AUDIO TRACK",
                                onBack = { currentSettingsMenu = SettingsMenu.MAIN }
                            ) {
                                ready.playData.tracks.forEach { trk ->
                                    val isCurrent = trk.equals(ready.currentTrack, ignoreCase = true)
                                    SettingsOptionItem(
                                        title = if (trk == "sub") "SUB (Original Audio)" else "DUB (English Dubbed)",
                                        isSelected = isCurrent,
                                        onClick = {
                                            if (!isCurrent) {
                                                onSwitchAudio(trk, exoPlayer.currentPosition)
                                            }
                                            showSettingsModal = false
                                        }
                                    )
                                }
                            }
                        }

                        SettingsMenu.SPEED -> {
                            val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                            SettingsSubmenu(
                                title = "PLAYBACK SPEED",
                                onBack = { currentSettingsMenu = SettingsMenu.MAIN }
                            ) {
                                speeds.forEach { spd ->
                                    SettingsOptionItem(
                                        title = if (spd == 1.0f) "1.0x (Normal)" else "${spd}x",
                                        isSelected = spd == currentPlaybackSpeed,
                                        onClick = {
                                            currentPlaybackSpeed = spd
                                            exoPlayer.setPlaybackSpeed(spd)
                                            showSettingsModal = false
                                        }
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

@Composable
fun SettingsMenuItem(
    title: String,
    currentValue: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Text(text = currentValue, color = TextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun SettingsSubmenu(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = NeonCyan,
                modifier = Modifier
                    .size(20.dp)
                    .clickable(onClick = onBack)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = GlassBorder)
        Spacer(modifier = Modifier.height(4.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 240.dp)
        ) {
            item {
                Column(content = content)
            }
        }
    }
}

@Composable
fun SettingsOptionItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) Color(0x3300F0FF) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = if (isSelected) NeonCyan else TextPrimary,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontFamily = FontFamily.Monospace
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = NeonCyan,
                modifier = Modifier.size(18.dp)
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
