package com.animex.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.animex.app.feature.detail.AnimeDetailScreen
import com.animex.app.feature.main.MainTabScreen
import com.animex.app.feature.player.PlayerScreen
import com.animex.app.feature.search.SearchScreen

sealed class Screen(val route: String) {
    data object Main : Screen("main")
    data object Search : Screen("search")
    data object Detail : Screen("detail/{animeId}") {
        fun createRoute(animeId: Long) = "detail/$animeId"
    }
    data object Player : Screen("player/{episodeId}") {
        fun createRoute(episodeId: Long) = "player/$episodeId"
    }
}

@Composable
fun AnimeXNavGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Main.route
    ) {
        composable(Screen.Main.route) {
            MainTabScreen(
                onAnimeClick = { animeId ->
                    navController.navigate(Screen.Detail.createRoute(animeId))
                },
                onPlayClick = { episodeId ->
                    navController.navigate(Screen.Player.createRoute(episodeId))
                },
                onSearchClick = {
                    navController.navigate(Screen.Search.route)
                }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onAnimeClick = { animeId ->
                    navController.navigate(Screen.Detail.createRoute(animeId))
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Detail.route,
            arguments = listOf(navArgument("animeId") { type = NavType.LongType })
        ) { backStackEntry ->
            val animeId = backStackEntry.arguments?.getLong("animeId") ?: return@composable
            AnimeDetailScreen(
                animeId = animeId,
                onBackClick = { navController.popBackStack() },
                onEpisodeClick = { episodeId ->
                    navController.navigate(Screen.Player.createRoute(episodeId))
                }
            )
        }

        composable(
            route = Screen.Player.route,
            arguments = listOf(navArgument("episodeId") { type = NavType.LongType })
        ) { backStackEntry ->
            val episodeId = backStackEntry.arguments?.getLong("episodeId") ?: return@composable
            PlayerScreen(
                episodeId = episodeId,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
