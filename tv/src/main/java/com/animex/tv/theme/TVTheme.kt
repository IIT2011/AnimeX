package com.animex.tv.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Cyber TV Color Palette
val CyberVoid = Color(0xFF050811)
val CyberSurfaceDark = Color(0xFF0C101D)
val CyberCardBg = Color(0xFF141C30)
val CyberBorderSubtle = Color(0xFF1E293B)
val CyberCyan = Color(0xFF00F0FF)
val CyberMagenta = Color(0xFFFF0055)
val CyberPurple = Color(0xFF9D4EDD)
val CyberNeonGreen = Color(0xFF39FF14)
val CyberGold = Color(0xFFFFD700)
val CyberTextPrimary = Color(0xFFF0F6FC)
val CyberTextSecondary = Color(0xFF8B949E)
val CyberOverlayDark = Color(0xCC050811)

private val TVDarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color.Black,
    secondary = CyberMagenta,
    onSecondary = Color.White,
    background = CyberVoid,
    onBackground = CyberTextPrimary,
    surface = CyberSurfaceDark,
    onSurface = CyberTextPrimary
)

@Composable
fun AnimeXTVTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TVDarkColorScheme,
        content = {
            Surface(
                color = CyberVoid,
                contentColor = CyberTextPrimary
            ) {
                content()
            }
        }
    )
}

/**
 * TV Focus modifier for Leanback remote navigation.
 * Scales element up to 1.08x, adds glowing CyberCyan/Magenta border, and handles clicks.
 */
@Composable
fun Modifier.tvFocusModifier(
    focusedBorderColor: Color = CyberCyan,
    unfocusedBorderColor: Color = CyberBorderSubtle,
    shape: Shape = RoundedCornerShape(12.dp),
    scaleTarget: Float = 1.06f,
    onFocus: () -> Unit = {},
    onClick: () -> Unit = {}
): Modifier {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) scaleTarget else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tvScale"
    )

    return this
        .scale(scale)
        .onFocusChanged { state ->
            isFocused = state.isFocused
            if (state.isFocused) {
                onFocus()
            }
        }
        .border(
            width = if (isFocused) 2.5.dp else 1.dp,
            color = if (isFocused) focusedBorderColor else unfocusedBorderColor,
            shape = shape
        )
        .clip(shape)
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
}
