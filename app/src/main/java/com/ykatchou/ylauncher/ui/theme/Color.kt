package com.ykatchou.ylauncher.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow

// Shared text shadow for readability over wallpaper
val WallpaperTextShadow = Shadow(
    color = Color.Black.copy(alpha = 0.6f),
    offset = Offset(1f, 1f),
    blurRadius = 4f,
)

// HAL 9000 inspired accent
val HalRed = Color(0xFFCC0000)
val HalRedBright = Color(0xFFFF2222)
val HalAmber = Color(0xFFFF6600)
val HalBezel = Color(0xFF2A2A2E)

// Home screen text (renders over wallpaper — always white with shadow)
val HomeTextColor = Color.White
val HomeTextColorDim = Color(0xBBFFFFFF) // 73% white

// Light theme
val LightBackground = Color(0xFFFFFBFF)
val LightOnBackground = Color(0xFF1C1B1E)
val LightSurface = Color(0xFFFFFBFF)
val LightOnSurface = Color(0xFF1C1B1E)
val LightSurfaceVariant = Color(0xFFE7E0EB)
val LightOnSurfaceVariant = Color(0xFF49454E)

// Dark theme
val DarkBackground = Color(0xFF1C1B1E)
val DarkOnBackground = Color(0xFFE6E1E6)
val DarkSurface = Color(0xFF1C1B1E)
val DarkOnSurface = Color(0xFFE6E1E6)
val DarkSurfaceVariant = Color(0xFF49454E)
val DarkOnSurfaceVariant = Color(0xFFCAC4CF)
