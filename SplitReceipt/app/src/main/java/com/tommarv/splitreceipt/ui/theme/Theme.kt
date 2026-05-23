package com.tommarv.splitreceipt.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF3700B3),
    onPrimaryContainer = Color.White,
    secondary = SecondaryDark,
    onSecondary = Color.Black,
    tertiary = TertiaryDark,
    onTertiary = Color.Black,
    background = BackgroundDark,
    surface = SurfaceDark,
    onBackground = TextOnDark,
    onSurface = TextOnDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryVibrant,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBB86FC),
    onPrimaryContainer = Color.Black,
    secondary = SecondaryVibrant,
    onSecondary = Color.Black,
    tertiary = TertiaryVibrant,
    onTertiary = Color.White,
    background = BackgroundLight,
    surface = SurfaceLight,
    onBackground = TextOnLight,
    onSurface = TextOnLight
)

@Composable
fun SplitReceiptTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
