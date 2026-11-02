package com.animex.tv.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import com.animex.tv.core.model.ScheduleDto
import com.animex.tv.core.network.NetworkClient
import com.animex.tv.theme.*

@Composable
fun TVScheduleScreen(
    onOpenAnimeDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val days = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
    var selectedDay by remember { mutableStateOf("TODAY") }
    var scheduleData by remember { mutableStateOf<ScheduleDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            scheduleData = NetworkClient.api.getSchedule()
            isLoading = false
        } catch (_: Exception) {
            isLoading = false
        }
    }

    val displayItems = remember(scheduleData) {
        scheduleData?.items?.map { item ->
            AnimeDto(
                id = item.id,
                title = item.title,
                titleJp = item.titleJp,
                poster = item.poster,
                score = item.score,
                sub = item.sub,
                dub = item.dub,
                eps = item.eps,
                episodeNumber = item.episodeNumber
            )
        } ?: emptyList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberVoid)
            .padding(top = 16.dp)
    ) {
        // Day selector tabs
        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(listOf("TODAY") + days) { day ->
                TVDayTab(
                    day = day,
                    isSelected = day == selectedDay,
                    onSelect = { selectedDay = day }
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 48.dp)
        ) {
            Text(
                text = "BROADCAST TRANSMISSIONS • $selectedDay",
                color = CyberTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            if (isLoading) {
                CircularProgressIndicator(
                    color = CyberCyan,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (displayItems.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 60.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = "No broadcasts scheduled for this cycle.",
                    color = CyberTextSecondary,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                contentPadding = PaddingValues(start = 48.dp, end = 48.dp, bottom = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayItems, key = { it.id }) { anime ->
                    TVCard(
                        anime = anime,
                        onClick = { onOpenAnimeDetail(anime.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TVDayTab(
    day: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val bgColor = when {
        isFocused -> CyberCyan.copy(alpha = 0.35f)
        isSelected -> CyberCyan.copy(alpha = 0.2f)
        else -> CyberCardBg
    }

    val borderColor = when {
        isFocused -> CyberCyan
        isSelected -> CyberCyan.copy(alpha = 0.7f)
        else -> CyberBorderSubtle
    }

    val contentColor = when {
        isFocused -> CyberCyan
        isSelected -> Color.White
        else -> CyberTextSecondary
    }

    Box(
        modifier = Modifier
            .scale(if (isFocused) 1.08f else 1.0f)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day,
            color = contentColor,
            fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}
