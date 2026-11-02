package com.animex.tv.ui.watchlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animex.tv.components.TVCard
import com.animex.tv.core.session.UserSessionRepository
import com.animex.tv.theme.*

@Composable
fun TVWatchlistScreen(
    onOpenAnimeDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val watchlist by UserSessionRepository.watchlist.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberVoid)
            .padding(horizontal = 48.dp, vertical = 20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "SAVED WATCHLIST (${watchlist.size})",
                color = CyberTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (watchlist.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = "Empty",
                        tint = CyberTextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Your watchlist is empty. Bookmark anime to watch them here.",
                        color = CyberTextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 48.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(watchlist, key = { it.id }) { anime ->
                    TVCard(
                        anime = anime,
                        onClick = { onOpenAnimeDetail(anime.id) }
                    )
                }
            }
        }
    }
}
