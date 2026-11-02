package com.robo.robotuner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TunerColorScheme = darkColorScheme(
    primary = AccentGreen,
    secondary = AccentOrange,
    tertiary = AccentRed,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = DarkBackground,
    onSecondary = DarkBackground,
    onTertiary = DarkBackground,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun RoboTunerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TunerColorScheme,
        typography = Typography,
        content = content
    )
}
