package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AmberAccent,
    secondary = TealAccent,
    tertiary = GoldSecondary,
    background = DeepSlate,
    surface = SlateMedium,
    onPrimary = DeepSlate,
    onSecondary = IceWhite,
    onTertiary = IceWhite,
    onBackground = IceWhite,
    onSurface = IceWhite
)

private val LightColorScheme = lightColorScheme(
    primary = AmberAccent,
    secondary = TealAccent,
    tertiary = GoldSecondary,
    background = DeepSlate,
    surface = SlateMedium,
    onPrimary = DeepSlate,
    onSecondary = IceWhite,
    onTertiary = IceWhite,
    onBackground = IceWhite,
    onSurface = IceWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark premium theme for cohesive Midnight Persian vibe
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
