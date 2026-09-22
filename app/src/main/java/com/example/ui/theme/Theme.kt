package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CameraColorScheme = darkColorScheme(
    primary = CameraAccentYellow,
    onPrimary = Color.Black,
    secondary = CameraAccentCyan,
    onSecondary = Color.Black,
    tertiary = CameraAccentRose,
    onTertiary = Color.White,
    background = CameraBlack,
    onBackground = TextPrimary,
    surface = CameraDarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = CameraTranslucentControl,
    onSurfaceVariant = TextSecondary,
    error = CameraAccentRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Camera applications are inherently dark
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = CameraColorScheme,
        typography = Typography,
        content = content
    )
}
