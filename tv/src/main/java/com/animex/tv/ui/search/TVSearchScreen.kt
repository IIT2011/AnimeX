package com.animex.tv.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animex.tv.components.TVCard
import com.animex.tv.core.model.AnimeDto
import com.animex.tv.core.network.NetworkClient
import com.animex.tv.theme.*
import kotlinx.coroutines.delay

@Composable
fun TVSearchScreen(
    onOpenAnimeDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<AnimeDto>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.trim().length >= 2) {
            isSearching = true
            delay(350)
            try {
                val results = NetworkClient.api.search(searchQuery.trim())
                searchResults = results
            } catch (_: Exception) {}
            isSearching = false
        } else if (searchQuery.isBlank()) {
            searchResults = emptyList()
        }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(CyberVoid)
            .padding(horizontal = 48.dp, vertical = 24.dp)
    ) {
        // Left Column: Virtual D-pad On-Screen Keyboard
        Column(
            modifier = Modifier
                .width(340.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Search Query Display Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberSurfaceDark)
                    .border(1.5.dp, CyberCyan, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (searchQuery.isEmpty()) "SEARCH ANIME TITLE..." else searchQuery,
                        color = if (searchQuery.isEmpty()) CyberTextSecondary else CyberTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Keyboard Rows (A-Z, 0-9, SPACE, BACKSPACE, CLEAR)
            val rows = listOf(
                listOf("A", "B", "C", "D", "E", "F"),
                listOf("G", "H", "I", "J", "K", "L"),
                listOf("M", "N", "O", "P", "Q", "R"),
                listOf("S", "T", "U", "V", "W", "X"),
                listOf("Y", "Z", "1", "2", "3", "4"),
                listOf("5", "6", "7", "8", "9", "0")
            )

            rows.forEach { rowKeys ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rowKeys.forEach { key ->
                        TVKeyButton(
                            text = key,
                            modifier = Modifier.weight(1f),
                            onClick = { searchQuery += key }
                        )
                    }
                }
            }

            // Bottom Action Keys: SPACE, BACKSPACE, CLEAR
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TVKeyButton(
                    text = "SPACE",
                    modifier = Modifier.weight(1.8f),
                    onClick = { searchQuery += " " }
                )

                TVKeyButton(
                    text = "DEL",
                    modifier = Modifier.weight(1.2f),
                    onClick = {
                        if (searchQuery.isNotEmpty()) {
                            searchQuery = searchQuery.dropLast(1)
                        }
                    }
                )

                TVKeyButton(
                    text = "CLR",
                    modifier = Modifier.weight(1f),
                    onClick = { searchQuery = "" }
                )
            }
        }

        Spacer(modifier = Modifier.width(36.dp))

        // Right Column: Search Results Grid
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (searchQuery.isBlank()) "POPULAR SUGGESTIONS" else "RESULTS FOR \"$searchQuery\"",
                    color = CyberTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                if (isSearching) {
                    CircularProgressIndicator(
                        color = CyberCyan,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (searchResults.isEmpty() && searchQuery.isNotBlank() && !isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = "No matching anime detected in transmission database.",
                        color = CyberTextSecondary,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(160.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 40.dp)
                ) {
                    items(searchResults, key = { it.id }) { anime ->
                        TVCard(
                            anime = anime,
                            onClick = { onOpenAnimeDetail(anime.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TVKeyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = modifier
            .height(38.dp)
            .scale(if (isFocused) 1.08f else 1.0f)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isFocused) CyberCyan.copy(alpha = 0.3f) else CyberCardBg)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) CyberCyan else CyberBorderSubtle,
                shape = RoundedCornerShape(6.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isFocused) CyberCyan else CyberTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
