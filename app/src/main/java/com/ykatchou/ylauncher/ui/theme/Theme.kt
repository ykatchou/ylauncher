package com.ykatchou.ylauncher.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = HalRed,
    onPrimary = LightBackground,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
)

private val DarkColorScheme = darkColorScheme(
    primary = HalRedBright,
    onPrimary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
)

@Composable
fun YLauncherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    fontScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val scaledTypography = if (fontScale != 1f) {
        YLauncherTypography.copy(
            displayLarge = YLauncherTypography.displayLarge.copy(fontSize = YLauncherTypography.displayLarge.fontSize * fontScale),
            displayMedium = YLauncherTypography.displayMedium.copy(fontSize = YLauncherTypography.displayMedium.fontSize * fontScale),
            headlineLarge = YLauncherTypography.headlineLarge.copy(fontSize = YLauncherTypography.headlineLarge.fontSize * fontScale),
            headlineMedium = YLauncherTypography.headlineMedium.copy(fontSize = YLauncherTypography.headlineMedium.fontSize * fontScale),
            headlineSmall = YLauncherTypography.headlineSmall.copy(fontSize = YLauncherTypography.headlineSmall.fontSize * fontScale),
            titleLarge = YLauncherTypography.titleLarge.copy(fontSize = YLauncherTypography.titleLarge.fontSize * fontScale),
            titleMedium = YLauncherTypography.titleMedium.copy(fontSize = YLauncherTypography.titleMedium.fontSize * fontScale),
            bodyLarge = YLauncherTypography.bodyLarge.copy(fontSize = YLauncherTypography.bodyLarge.fontSize * fontScale),
            bodyMedium = YLauncherTypography.bodyMedium.copy(fontSize = YLauncherTypography.bodyMedium.fontSize * fontScale),
            bodySmall = YLauncherTypography.bodySmall.copy(fontSize = YLauncherTypography.bodySmall.fontSize * fontScale),
            labelLarge = YLauncherTypography.labelLarge.copy(fontSize = YLauncherTypography.labelLarge.fontSize * fontScale),
            labelMedium = YLauncherTypography.labelMedium.copy(fontSize = YLauncherTypography.labelMedium.fontSize * fontScale),
            labelSmall = YLauncherTypography.labelSmall.copy(fontSize = YLauncherTypography.labelSmall.fontSize * fontScale),
        )
    } else {
        YLauncherTypography
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = scaledTypography,
        content = content,
    )
}
