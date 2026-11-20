package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalUseFallbackRippleImplementation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.keyboard.AppThemeMode

@Immutable
data class KeyDeckAppColors(
    val bg: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val shadowLight: Color,
    val shadowDark: Color,
    val keyBorder: Color,
    val activeFill: Color,
    val indigo: Color,
    val accentFill: Color,
    val indigoSoft: Color,
    val emerald: Color,
    val amber: Color,
    val coral: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val isDark: Boolean,
)

val LightKeyDeckColors = KeyDeckAppColors(
    bg = NeuBg,
    surface = NeuSurface,
    surfaceElevated = NeuSurfaceElevated,
    shadowLight = NeuShadowLight,
    shadowDark = NeuShadowDark,
    keyBorder = NeuKeyBorder,
    activeFill = NeuActiveFill,
    indigo = NeuIndigo,
    accentFill = NeuIndigo,
    indigoSoft = NeuIndigoSoft,
    emerald = NeuEmerald,
    amber = NeuAmber,
    coral = NeuCoral,
    textPrimary = NeuTextPrimary,
    textSecondary = NeuTextSecondary,
    textMuted = NeuTextMuted,
    isDark = false,
)

val DarkKeyDeckColors = KeyDeckAppColors(
    bg = DarkNeuBg,
    surface = DarkNeuSurface,
    surfaceElevated = DarkNeuSurfaceElevated,
    shadowLight = DarkNeuShadowLight,
    shadowDark = DarkNeuShadowDark,
    keyBorder = DarkNeuKeyBorder,
    activeFill = DarkNeuActiveFill,
    indigo = DarkNeuIndigo,
    accentFill = DarkNeuIndigoFill,
    indigoSoft = DarkNeuIndigoSoft,
    emerald = DarkNeuEmerald,
    amber = DarkNeuAmber,
    coral = DarkNeuCoral,
    textPrimary = DarkNeuTextPrimary,
    textSecondary = DarkNeuTextSecondary,
    textMuted = DarkNeuTextMuted,
    isDark = true,
)

val LocalKeyDeckColors = staticCompositionLocalOf { LightKeyDeckColors }

object KeyDeckColors {
    val current: KeyDeckAppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalKeyDeckColors.current
}

private val KeyDeckNeuLightColorScheme = lightColorScheme(
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

private val KeyDeckNeuDarkColorScheme = darkColorScheme(
    primary = DarkNeuIndigo,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = DarkNeuActiveFill,
    onPrimaryContainer = DarkNeuTextPrimary,
    secondary = DarkNeuIndigoSoft,
    onSecondary = Color.White,
    secondaryContainer = DarkNeuSurfaceElevated,
    onSecondaryContainer = DarkNeuTextPrimary,
    tertiary = DarkNeuEmerald,
    onTertiary = Color(0xFF062E22),
    background = DarkNeuBg,
    onBackground = DarkNeuTextPrimary,
    surface = DarkNeuSurface,
    onSurface = DarkNeuTextPrimary,
    surfaceVariant = DarkNeuSurfaceElevated,
    onSurfaceVariant = DarkNeuTextSecondary,
    outline = DarkNeuKeyBorder,
    outlineVariant = DarkNeuShadowDark.copy(alpha = 0.65f),
)

@Composable
fun resolveIsAppDark(mode: AppThemeMode): Boolean {
    return when (mode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyDeckTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) KeyDeckNeuDarkColorScheme else KeyDeckNeuLightColorScheme
    val appColors = if (darkTheme) DarkKeyDeckColors else LightKeyDeckColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KeyDeckTypography,
    ) {
        @Suppress("DEPRECATION")
        CompositionLocalProvider(
            LocalUseFallbackRippleImplementation provides true,
            LocalKeyDeckColors provides appColors,
            content = content,
        )
    }
}
