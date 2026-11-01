package com.animex.app.core.designsystem

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Futuristic Void & Obsidian Surfaces
val VoidDark = Color(0xFF07080D)
val ObsidianSurface = Color(0xFF0E1018)
val ObsidianCard = Color(0xFF141724)
val GlassSurface = Color(0x1A1B2236)
val GlassBorder = Color(0x3300F0FF)

// Cyber High-Tech Neon Accents
val NeonCyan = Color(0xFF00F0FF)
val NeonMagenta = Color(0xFFFF007F)
val NeonViolet = Color(0xFF7928CA)
val NeonAmber = Color(0xFFFFB800)
val NeonLime = Color(0xFF00FF88)

// Typography & Contrast
val TextPrimary = Color(0xFFF3F6FD)
val TextSecondary = Color(0xFF8E9BAE)
val TextTertiary = Color(0xFF555F72)

// Cyber Gradient Brushes
val CyberNeonBrush = Brush.linearGradient(
    colors = listOf(NeonCyan, NeonMagenta)
)

val CardBorderGlowBrush = Brush.linearGradient(
    colors = listOf(Color(0x8000F0FF), Color(0x20FF007F), Color(0x1000F0FF))
)

val HeroVignetteBrush = Brush.verticalGradient(
    colors = listOf(Color.Transparent, Color(0xB307080D), VoidDark)
)
