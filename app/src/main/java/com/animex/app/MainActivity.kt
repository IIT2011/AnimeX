package com.animex.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.animex.app.core.designsystem.AnimeXTheme
import com.animex.app.navigation.AnimeXNavGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AnimeXTheme {
                val navController = rememberNavController()
                AnimeXNavGraph(navController = navController)
            }
        }
    }
}
