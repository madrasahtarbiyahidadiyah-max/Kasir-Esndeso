package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EsNdesoPrimaryDark,
    onPrimary = Color.White,
    secondary = Gradient1Start,
    onSecondary = Color.White,
    tertiary = Gradient2Start,
    onTertiary = Color.White,
    background = EsNdesoBackgroundDark,
    onBackground = EsNdesoTextMainDark,
    surface = EsNdesoSurfaceDark,
    onSurface = EsNdesoTextMainDark,
    surfaceVariant = EsNdesoNavDark,
    onSurfaceVariant = EsNdesoTextMutedDark,
    error = EsNdesoDangerDark,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = EsNdesoPrimaryLight,
    onPrimary = Color.White,
    secondary = Gradient1Start,
    onSecondary = Color.White,
    tertiary = Gradient2Start,
    onTertiary = Color.White,
    background = EsNdesoBackgroundLight,
    onBackground = EsNdesoTextMainLight,
    surface = EsNdesoSurfaceLight,
    onSurface = EsNdesoTextMainLight,
    surfaceVariant = EsNdesoNavLight,
    onSurfaceVariant = EsNdesoTextMutedLight,
    error = EsNdesoDangerLight,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
