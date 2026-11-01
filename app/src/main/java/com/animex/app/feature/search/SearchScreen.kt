package com.animex.app.feature.search

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.animex.app.core.designsystem.*
import com.animex.app.feature.home.AnimePosterCard

@Composable
fun SearchScreen(
    onAnimeClick: (Long) -> Unit,
    onBackClick: () -> Unit,
    viewModel: SearchViewModel = viewModel()
) {
    var queryText by remember { mutableStateOf("") }
    var showFilterDrawer by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val filters by viewModel.filters.collectAsState()

    val activeFilterCount = (if (filters.type != null) 1 else 0) +
            (if (filters.status != null) 1 else 0) +
            (if (filters.sort != "most_popular") 1 else 0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidDark)
            .statusBarsPadding()
    ) {
        // Top Search Bar HUD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ObsidianCard)
                    .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                    .clickable(onClick = onBackClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Glowing Input Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ObsidianCard)
                    .cyberPulseBorder(RoundedCornerShape(10.dp), strokeWidth = 1.dp)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        if (queryText.isEmpty()) {
                            Text(
                                text = "SCAN ANIME DATABASE...",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        BasicTextField(
                            value = queryText,
                            onValueChange = {
                                queryText = it
                                viewModel.onQueryChange(it)
                            },
                            textStyle = TextStyle(
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            ),
                            cursorBrush = SolidColor(NeonCyan),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (queryText.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = TextSecondary,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable {
                                    queryText = ""
                                    viewModel.onQueryChange("")
                                }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Filter Toggle Button with Badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (showFilterDrawer || activeFilterCount > 0) Color(0x3300F0FF) else ObsidianCard)
                    .border(
                        1.dp,
                        if (showFilterDrawer || activeFilterCount > 0) NeonCyan else GlassBorder,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { showFilterDrawer = !showFilterDrawer },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filters",
                    tint = if (showFilterDrawer || activeFilterCount > 0) NeonCyan else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )

                if (activeFilterCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.TopEnd)
                            .clip(RoundedCornerShape(7.dp))
                            .background(NeonMagenta),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = activeFilterCount.toString(),
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Animated Filter Drawer
        AnimatedVisibility(
            visible = showFilterDrawer,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianCard)
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "QUANTUM FILTERS",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )

                    // Type Row
                    FilterChipRow(
                        label = "TYPE",
                        options = listOf("All", "TV", "Movie", "OVA"),
                        selected = filters.type ?: "All",
                        onSelect = {
                            val newType = if (it == "All") null else it
                            viewModel.applyFilters(filters.copy(type = newType))
                        }
                    )

                    // Status Row
                    FilterChipRow(
                        label = "STATUS",
                        options = listOf("All", "airing", "completed"),
                        selected = filters.status ?: "All",
                        onSelect = {
                            val newStatus = if (it == "All") null else it
                            viewModel.applyFilters(filters.copy(status = newStatus))
                        }
                    )

                    // Sort Row
                    FilterChipRow(
                        label = "SORT",
                        options = listOf("most_popular", "latest", "score"),
                        selected = filters.sort,
                        onSelect = {
                            viewModel.applyFilters(filters.copy(sort = it))
                        }
                    )
                }
            }
        }

        // Search Results Content
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                is SearchUiState.Idle -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x1A00F0FF))
                                .border(1.5.dp, NeonCyan, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "AWAITING SCAN QUERY",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Type an anime title or select filters above",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
                is SearchUiState.Loading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = NeonCyan)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "SCANNING SECTORS...",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                is SearchUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "SCAN NODE ERROR", color = NeonMagenta, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = state.message, color = TextSecondary, fontSize = 12.sp)
                    }
                }
                is SearchUiState.Success -> {
                    val results = state.results
                    if (results.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "NO MATCHING ANIME FOUND IN DATABASE",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 130.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(results) { anime ->
                                AnimePosterCard(
                                    anime = anime,
                                    onClick = { onAnimeClick(anime.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChipRow(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(55.dp)
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(options) { opt ->
                val isSel = opt.equals(selected, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSel) Color(0x3300F0FF) else ObsidianSurface)
                        .border(1.dp, if (isSel) NeonCyan else GlassBorder, RoundedCornerShape(6.dp))
                        .clickable { onSelect(opt) }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = opt.uppercase(),
                        color = if (isSel) NeonCyan else TextPrimary,
                        fontSize = 10.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
