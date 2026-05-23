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
    primary = Color(0xFF64B5F6), // Lighter, more vibrant blue for dark mode
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF004691),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF81C784), // Lighter green for dark mode
    onSecondary = Color.Black,
    background = SofaBackgroundDark,
    surface = SofaSurfaceDark,
    onBackground = SofaTextPrimaryDark,
    onSurface = SofaTextPrimaryDark,
    outline = SofaDividerDark,
    error = SofaError
)

private val LightColorScheme = lightColorScheme(
    primary = SofaBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = SofaBlueDark,
    secondary = SofaAccent,
    onSecondary = Color.White,
    background = SofaBackgroundLight,
    surface = SofaSurfaceLight,
    onBackground = SofaTextPrimaryLight,
    onSurface = SofaTextPrimaryLight,
    outline = SofaDivider,
    error = SofaError
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
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
