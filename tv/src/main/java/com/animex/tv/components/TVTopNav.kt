package com.animex.tv.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animex.tv.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TVDestination(val title: String, val icon: ImageVector) {
    HOME("HOME", Icons.Default.Home),
    GENRES("GENRES", Icons.Default.Category),
    SCHEDULE("SCHEDULE", Icons.Default.CalendarToday),
    SEARCH("SEARCH", Icons.Default.Search),
    WATCHLIST("WATCHLIST", Icons.Default.Bookmark)
}

@Composable
fun TVTopNav(
    currentDestination: TVDestination,
    onSelectDestination: (TVDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) {
            currentTime = sdf.format(Date())
            delay(10000)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        CyberVoid,
                        CyberVoid.copy(alpha = 0.85f),
                        Color.Transparent
                    )
                )
            )
            .padding(horizontal = 36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Logo & TV Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(CyberCyan, CyberMagenta))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "X",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(
                text = "ANIMEX",
                color = CyberTextPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                letterSpacing = 2.sp
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(CyberMagenta)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "TV",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        // Center: D-pad focusable navigation tabs
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TVDestination.entries.forEach { dest ->
                TVNavTabItem(
                    destination = dest,
                    isSelected = dest == currentDestination,
                    onSelect = { onSelectDestination(dest) }
                )
            }
        }

        // Right: Clock & Operational Status
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Time HUD
            Text(
                text = currentTime,
                color = CyberCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace
            )

            // Status Indicator
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberSurfaceDark)
                    .border(1.dp, CyberBorderSubtle, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(CyberNeonGreen)
                    )
                    Text(
                        text = "ONLINE",
                        color = CyberTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun TVNavTabItem(
    destination: TVDestination,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val bgColor by animateColorAsState(
        targetValue = when {
            isFocused -> CyberCyan.copy(alpha = 0.22f)
            isSelected -> CyberCardBg
            else -> Color.Transparent
        },
        label = "navTabBg"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isFocused -> CyberCyan
            isSelected -> Color.White
            else -> CyberTextSecondary
        },
        label = "navTabColor"
    )

    val borderColor = if (isFocused) CyberCyan else if (isSelected) CyberBorderSubtle else Color.Transparent

    Box(
        modifier = Modifier
            .scale(if (isFocused) 1.08f else 1.0f)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(width = if (isFocused) 2.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(8.dp))
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = destination.icon,
                contentDescription = destination.title,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = destination.title,
                color = contentColor,
                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp,
                letterSpacing = 1.sp
            )
        }
    }
}
