package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KeyDeckNeuColorScheme = lightColorScheme(
    primary = NeuIndigo,
    onPrimary = Color.White,
    primaryContainer = NeuActiveFill,
    onPrimaryContainer = NeuIndigo,
    secondary = NeuIndigoSoft,
    onSecondary = Color.White,
    secondaryContainer = NeuSurfaceElevated,
    onSecondaryContainer = NeuTextPrimary,
    tertiary = NeuEmerald,
    onTertiary = Color.White,
    background = NeuBg,
    onBackground = NeuTextPrimary,
    surface = NeuSurface,
    onSurface = NeuTextPrimary,
    surfaceVariant = NeuSurfaceElevated,
    onSurfaceVariant = NeuTextSecondary,
    outline = NeuShadowDark,
    outlineVariant = NeuShadowDark.copy(alpha = 0.4f),
)

@Composable
fun KeyDeckTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = KeyDeckNeuColorScheme,
        typography = KeyDeckTypography,
        content = content,
    )
}
