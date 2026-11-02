package com.animex.app.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.animex.app.core.designsystem.AppTab
import com.animex.app.core.designsystem.CyberBottomNav
import com.animex.app.core.designsystem.VoidDark
import com.animex.app.feature.account.AccountScreen
import com.animex.app.feature.genre.GenreScreen
import com.animex.app.feature.home.HomeScreen
import com.animex.app.feature.schedule.ScheduleScreen

@Composable
fun MainTabScreen(
    onAnimeClick: (Long) -> Unit,
    onPlayClick: (episodeId: Long, animeTitle: String?, episodeNumber: Int?, poster: String?, localPath: String?) -> Unit,
    onSearchClick: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(AppTab.HOME) }

    Scaffold(
        containerColor = VoidDark,
        bottomBar = {
            CyberBottomNav(
                currentTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VoidDark)
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                AppTab.HOME -> {
                    HomeScreen(
                        onAnimeClick = onAnimeClick,
                        onPlayClick = onPlayClick,
                        onSearchClick = onSearchClick
                    )
                }
                AppTab.GENRE -> {
                    GenreScreen(
                        onAnimeClick = onAnimeClick
                    )
                }
                AppTab.TIMINGS -> {
                    ScheduleScreen(
                        onAnimeClick = onAnimeClick
                    )
                }
                AppTab.ACCOUNT -> {
                    AccountScreen(
                        onAnimeClick = onAnimeClick,
                        onPlayClick = onPlayClick
                    )
                }
            }
        }
    }
}
