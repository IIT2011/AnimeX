package com.animex.app.feature.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.animex.app.core.designsystem.*
import com.animex.app.core.network.NetworkClient
import com.animex.app.core.network.model.ScheduleDayDto
import com.animex.app.core.network.model.ScheduleItemDto

@Composable
fun ScheduleScreen(
    onAnimeClick: (Long) -> Unit,
    viewModel: ScheduleViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidDark)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "BROADCAST",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "TIMINGS",
                    color = NeonCyan,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusBeacon(color = NeonCyan)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "CHRONO",
                    color = NeonCyan,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                is ScheduleUiState.Loading -> {
                    CircularProgressIndicator(
                        color = NeonCyan,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is ScheduleUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "TIMING SYNC FAILED", color = NeonMagenta, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(8.dp))
                        CyberButton(text = "RELOAD", onClick = { viewModel.loadSchedule() })
                    }
                }
                is ScheduleUiState.Success -> {
                    val days = state.schedule.days
                    val selectedDay = state.selectedDay
                    val items = state.filteredItems

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Days of week selector
                        if (days.isNotEmpty()) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(days) { day ->
                                    DayChip(
                                        day = day,
                                        isSelected = day.date == selectedDay?.date,
                                        onClick = { viewModel.selectDay(day) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (items.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "NO BROADCASTS SCHEDULED FOR THIS TIME WINDOW",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        } else {
                            // Timeline Items
                            LazyColumn(
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(items) { item ->
                                    ScheduleTimelineCard(
                                        item = item,
                                        onClick = { onAnimeClick(item.id) }
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
fun DayChip(
    day: ScheduleDayDto,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) ObsidianCard else Color(0x1A1B2236)
    val borderColor = if (isSelected) NeonCyan else GlassBorder
    val textColor = if (isSelected) NeonCyan else TextSecondary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.label.uppercase(),
            color = textColor,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun ScheduleTimelineCard(
    item: ScheduleItemDto,
    onClick: () -> Unit
) {
    val posterUrl = NetworkClient.getFullPosterUrl(item.poster)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ObsidianCard)
            .border(1.dp, CardBorderGlowBrush, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Air Time Badge
        Column(
            modifier = Modifier.width(62.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = item.time ?: "--:--",
                color = NeonCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "AIRING",
                color = TextSecondary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Vertical divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(48.dp)
                .background(GlassBorder)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Anime Thumbnail
        Box(
            modifier = Modifier
                .size(46.dp, 60.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black)
        ) {
            AsyncImage(
                model = posterUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item.episodeNumber?.let { ep ->
                    CyberBadge(text = "EP $ep", accentColor = NeonAmber)
                }
                if ((item.sub ?: 0) > 0) {
                    CyberBadge(text = "SUB", accentColor = NeonCyan)
                }
                item.score?.let { score ->
                    CyberBadge(text = "★ $score", accentColor = NeonMagenta)
                }
            }
        }
    }
}
