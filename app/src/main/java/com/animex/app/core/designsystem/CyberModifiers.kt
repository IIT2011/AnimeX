package com.animex.app.core.designsystem

import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Animated pulsating Cyberpunk neon border that dynamically shifts hues.
 */
@Composable
fun Modifier.cyberPulseBorder(
    shape: Shape = RoundedCornerShape(10.dp),
    strokeWidth: Dp = 1.5.dp
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "cyberBorder")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase"
    )

    val gradient = Brush.linearGradient(
        colors = listOf(
            NeonCyan.copy(alpha = 0.3f + (phase * 0.7f)),
            NeonMagenta.copy(alpha = 0.2f + ((1f - phase) * 0.7f)),
            NeonCyan.copy(alpha = 0.2f)
        )
    )

    this.border(width = strokeWidth, brush = gradient, shape = shape)
}

/**
 * High-tech holographic laser scanning sweep line across the surface.
 */
@Composable
fun Modifier.holographicScanline(): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "scanline")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanlineOffset"
    )

    this.drawWithContent {
        drawContent()
        val lineY = size.height * offsetY
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, NeonCyan.copy(alpha = 0.45f), NeonMagenta.copy(alpha = 0.45f), Color.Transparent)
            ),
            start = Offset(0f, lineY),
            end = Offset(size.width, lineY),
            strokeWidth = 2.dp.toPx()
        )
    }
}

/**
 * Springy tactile micro-interaction on button/card press.
 */
@Composable
fun Modifier.cyberBounceClick(
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "bounce"
    )

    this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}
