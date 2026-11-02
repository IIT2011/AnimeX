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
    data object Player : Screen("player/{episodeId}?animeTitle={animeTitle}&episodeNumber={episodeNumber}&poster={poster}") {
        fun createRoute(
            episodeId: Long,
            animeTitle: String? = null,
            episodeNumber: Int? = null,
            poster: String? = null
        ): String {
            val encodedTitle = java.net.URLEncoder.encode(animeTitle ?: "", "UTF-8")
            val epNum = episodeNumber ?: -1
            val encodedPoster = java.net.URLEncoder.encode(poster ?: "", "UTF-8")
            return "player/$episodeId?animeTitle=$encodedTitle&episodeNumber=$epNum&poster=$encodedPoster"
        }
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
                onPlayClick = { episodeId, animeTitle, episodeNumber, poster ->
                    navController.navigate(Screen.Player.createRoute(episodeId, animeTitle, episodeNumber, poster))
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
                onEpisodeClick = { episodeId, episodeNumber, title, poster ->
                    navController.navigate(Screen.Player.createRoute(episodeId, title, episodeNumber, poster))
                }
            )
        }

        composable(
            route = Screen.Player.route,
            arguments = listOf(
                navArgument("episodeId") { type = NavType.LongType },
                navArgument("animeTitle") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("episodeNumber") {
                    type = NavType.IntType
                    defaultValue = -1
                },
                navArgument("poster") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val episodeId = backStackEntry.arguments?.getLong("episodeId") ?: return@composable
            val rawTitle = backStackEntry.arguments?.getString("animeTitle") ?: ""
            val animeTitle = try {
                java.net.URLDecoder.decode(rawTitle, "UTF-8").takeIf { it.isNotBlank() }
            } catch (e: Exception) {
                null
            }
            val epNum = backStackEntry.arguments?.getInt("episodeNumber")?.takeIf { it > 0 }
            val rawPoster = backStackEntry.arguments?.getString("poster") ?: ""
            val poster = try {
                java.net.URLDecoder.decode(rawPoster, "UTF-8").takeIf { it.isNotBlank() }
            } catch (e: Exception) {
                null
            }

            PlayerScreen(
                episodeId = episodeId,
                animeTitle = animeTitle,
                episodeNumber = epNum,
                poster = poster,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
