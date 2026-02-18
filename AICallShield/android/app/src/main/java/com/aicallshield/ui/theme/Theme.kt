package com.aicallshield.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Green600,
    onPrimary = Color.White,
    primaryContainer = Green50,
    onPrimaryContainer = Green800,
    secondary = GreenAccent,
    onSecondary = Color.White,
    secondaryContainer = MintLight,
    error = Red500,
    onError = Color.White,
    background = Color.White,
    onBackground = Gray900,
    surface = Color.White,
    onSurface = Gray800,
    surfaceVariant = MintSurface,
    onSurfaceVariant = Gray600,
    outline = Gray400,
)

private val DarkColorScheme = darkColorScheme(
    primary = Green400,
    onPrimary = Green800,
    primaryContainer = Green800,
    onPrimaryContainer = Green50,
    secondary = GreenAccent,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF003300),
    error = Color(0xFFEF5350),
    onError = Color.Black,
    background = Color(0xFF121212),
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Gray400,
    outline = Gray600,
)

@Composable
fun AICallShieldTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.White.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
