package com.setons.trackrep.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

fun buildDynamicDarkColorScheme(primaryAccent: Color = DarkPrimaryGold): ColorScheme {
    val luminance = primaryAccent.red * 0.299f + primaryAccent.green * 0.587f + primaryAccent.blue * 0.114f
    val onPrimary = if (luminance > 0.55f) Color(0xFF0C0A04) else Color(0xFFFFFFFF)

    return darkColorScheme(
        primary = primaryAccent,
        onPrimary = onPrimary,
        primaryContainer = DarkSurfaceVariant,
        onPrimaryContainer = primaryAccent,
        secondary = primaryAccent,
        onSecondary = onPrimary,
        secondaryContainer = DarkSurfaceVariant,
        onSecondaryContainer = primaryAccent,
        tertiary = primaryAccent.copy(alpha = 0.85f),
        onTertiary = onPrimary,
        background = DarkBackground,
        onBackground = DarkOnBackground,
        surface = DarkSurface,
        onSurface = DarkOnSurface,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = DarkOnSurface,
        outline = primaryAccent.copy(alpha = 0.45f)
    )
}

fun buildDynamicLightColorScheme(primaryAccent: Color = LightPrimaryRed): ColorScheme {
    val luminance = primaryAccent.red * 0.299f + primaryAccent.green * 0.587f + primaryAccent.blue * 0.114f
    val onPrimary = if (luminance > 0.55f) Color(0xFF0C0A04) else Color(0xFFFFFFFF)

    return lightColorScheme(
        primary = primaryAccent,
        onPrimary = onPrimary,
        primaryContainer = LightSurfaceVariant,
        onPrimaryContainer = primaryAccent,
        secondary = primaryAccent,
        onSecondary = onPrimary,
        secondaryContainer = LightSurfaceVariant,
        onSecondaryContainer = primaryAccent,
        tertiary = primaryAccent.copy(alpha = 0.85f),
        onTertiary = onPrimary,
        background = LightBackground,
        onBackground = LightOnBackground,
        surface = LightSurface,
        onSurface = LightOnSurface,
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = LightOnSurface,
        outline = primaryAccent.copy(alpha = 0.35f)
    )
}

@Composable
fun TrackRepTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    darkAccent: Color = DarkPrimaryGold,
    lightAccent: Color = LightPrimaryRed,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        buildDynamicDarkColorScheme(darkAccent)
    } else {
        buildDynamicLightColorScheme(lightAccent)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
