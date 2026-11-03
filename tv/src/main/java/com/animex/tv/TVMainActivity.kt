package com.animex.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.animex.tv.components.TVDestination
import com.animex.tv.components.TVTopNav
import com.animex.tv.core.network.NetworkClient
import com.animex.tv.core.updater.TVAppUpdateManager
import com.animex.tv.core.updater.TVUpdateCheckResult
import com.animex.tv.theme.AnimeXTVTheme
import com.animex.tv.theme.CyberVoid
import com.animex.tv.ui.detail.TVDetailScreen
import com.animex.tv.ui.genre.TVGenreScreen
import com.animex.tv.ui.home.TVHomeScreen
import com.animex.tv.ui.player.TVPlayerScreen
import com.animex.tv.ui.schedule.TVScheduleScreen
import com.animex.tv.ui.search.TVSearchScreen
import com.animex.tv.ui.updater.TVUpdateDialog
import com.animex.tv.ui.watchlist.TVWatchlistScreen
import kotlinx.coroutines.launch

data class TVPlayerParams(
    val episodeId: Long,
    val episodeNumber: Int,
    val animeTitle: String
)

class TVMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Leanback immersive TV fullscreen
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            AnimeXTVTheme {
                TVMainRoot()
            }
        }
    }
}

@Composable
fun TVMainRoot() {
    val context = LocalContext.current
    var currentDestination by remember { mutableStateOf(TVDestination.HOME) }
    var activeDetailAnimeId by remember { mutableStateOf<Long?>(null) }
    var activePlayerParams by remember { mutableStateOf<TVPlayerParams?>(null) }
    var tvUpdateInfo by remember { mutableStateOf<TVUpdateCheckResult?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // Automatic update check on TV launch
    LaunchedEffect(Unit) {
        val res = TVAppUpdateManager.checkForUpdate(context)
        if (res.hasUpdate) {
            tvUpdateInfo = res
        }
    }

    if (tvUpdateInfo != null) {
        TVUpdateDialog(
            updateInfo = tvUpdateInfo!!,
            onDismiss = { tvUpdateInfo = null }
        )
    }

    // Back handling for TV remote BACK key
    BackHandler(enabled = activePlayerParams != null || activeDetailAnimeId != null || currentDestination != TVDestination.HOME) {
        if (activePlayerParams != null) {
            activePlayerParams = null
        } else if (activeDetailAnimeId != null) {
            activeDetailAnimeId = null
        } else if (currentDestination != TVDestination.HOME) {
            currentDestination = TVDestination.HOME
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = CyberVoid
    ) {
        when {
            // Fullscreen TV Video Player Mode
            activePlayerParams != null -> {
                val params = activePlayerParams!!
                TVPlayerScreen(
                    episodeId = params.episodeId,
                    episodeNumber = params.episodeNumber,
                    animeTitle = params.animeTitle,
                    onBack = { activePlayerParams = null }
                )
            }

            // Anime Detail Screen Mode
            activeDetailAnimeId != null -> {
                TVDetailScreen(
                    animeId = activeDetailAnimeId!!,
                    onBack = { activeDetailAnimeId = null },
                    onPlayEpisode = { epId, epNum, title ->
                        activePlayerParams = TVPlayerParams(
                            episodeId = epId,
                            episodeNumber = epNum,
                            animeTitle = title
                        )
                    }
                )
            }

            // Top-Level Nav Screens
            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    TVTopNav(
                        currentDestination = currentDestination,
                        onSelectDestination = { dest ->
                            currentDestination = dest
                        }
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        Crossfade(targetState = currentDestination, label = "tvNavCrossfade") { dest ->
                            when (dest) {
                                TVDestination.HOME -> {
                                    TVHomeScreen(
                                        onOpenAnimeDetail = { id ->
                                            activeDetailAnimeId = id
                                        },
                                        onPlayAnimeDirectly = { animeId, episodeId, title ->
                                            if (episodeId != null) {
                                                activePlayerParams = TVPlayerParams(
                                                    episodeId = episodeId,
                                                    episodeNumber = 1,
                                                    animeTitle = title
                                                )
                                            } else {
                                                // Fetch first episode asynchronously
                                                coroutineScope.launch {
                                                    try {
                                                        val eps = NetworkClient.api.getEpisodes(animeId)
                                                        val first = eps.data.firstOrNull()
                                                        if (first != null) {
                                                            activePlayerParams = TVPlayerParams(
                                                                episodeId = first.id,
                                                                episodeNumber = first.number,
                                                                animeTitle = title
                                                            )
                                                        } else {
                                                            activeDetailAnimeId = animeId
                                                        }
                                                    } catch (_: Exception) {
                                                        activeDetailAnimeId = animeId
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }

                                TVDestination.GENRES -> {
                                    TVGenreScreen(
                                        onOpenAnimeDetail = { id ->
                                            activeDetailAnimeId = id
                                        }
                                    )
                                }

                                TVDestination.SCHEDULE -> {
                                    TVScheduleScreen(
                                        onOpenAnimeDetail = { id ->
                                            activeDetailAnimeId = id
                                        }
                                    )
                                }

                                TVDestination.SEARCH -> {
                                    TVSearchScreen(
                                        onOpenAnimeDetail = { id ->
                                            activeDetailAnimeId = id
                                        }
                                    )
                                }

                                TVDestination.WATCHLIST -> {
                                    TVWatchlistScreen(
                                        onOpenAnimeDetail = { id ->
                                            activeDetailAnimeId = id
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
