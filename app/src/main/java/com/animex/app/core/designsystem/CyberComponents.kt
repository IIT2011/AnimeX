package com.animex.app.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * High-tech HUD telemetry badge for episode status, resolution, and audio type.
 */
@Composable
fun CyberBadge(
    text: String,
    modifier: Modifier = Modifier,
    accentColor: Color = NeonCyan,
    backgroundColor: Color = Color(0x33000000)
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.6f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            color = accentColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * Cyberpunk interactive button with glowing neon outline.
 */
@Composable
fun CyberButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    isPrimary: Boolean = true
) {
    val backgroundBrush = if (isPrimary) {
        Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.25f), NeonMagenta.copy(alpha = 0.25f)))
    } else {
        Brush.horizontalGradient(listOf(GlassSurface, GlassSurface))
    }

    val borderBrush = if (isPrimary) {
        CyberNeonBrush
    } else {
        Brush.horizontalGradient(listOf(GlassBorder, GlassBorder))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundBrush)
            .border(1.dp, borderBrush, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.let {
                it()
                Box(modifier = Modifier.size(8.dp))
            }
            Text(
                text = text,
                color = if (isPrimary) NeonCyan else TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }
    }
}

/**
 * Futuristic pulsating online/status beacon indicator.
 */
@Composable
fun StatusBeacon(
    color: Color = NeonLime,
    size: Dp = 8.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, color.copy(alpha = 0.4f), CircleShape)
    )
}
