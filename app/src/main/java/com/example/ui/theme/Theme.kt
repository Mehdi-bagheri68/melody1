package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AccentGold,
    onPrimary = Color(0xFF1E1300),
    primaryContainer = Color(0xFF382700),
    onPrimaryContainer = Color(0xFFFFDEA3),
    
    secondary = AccentOrange,
    onSecondary = Color(0xFF2E0F00),
    secondaryContainer = Color(0xFF4C1D00),
    onSecondaryContainer = Color(0xFFFFDBCD),
    
    tertiary = AccentAmber,
    onTertiary = Color(0xFF281900),
    
    background = OledBackground,
    onBackground = TextPrimary,
    
    surface = OledSurface,
    onSurface = TextPrimary,
    surfaceVariant = OledSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    
    outline = BorderSubtle,
    outlineVariant = BorderHighlight
)

@Composable
fun MeloGrooveTheme(
    darkTheme: Boolean = true, // Default to stunning dark theme for music experience
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MeloGrooveTheme(darkTheme = true, content = content)
}
