package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.model.ThemeMode

@Immutable
data class AlqanasPalette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceSolid: Color,
    val surfaceSubtle: Color,
    val border: Color,
    val borderStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val accentInk: Color,
    val warnBg: Color,
    val warnBorder: Color,
    val warnText: Color,
    val critBg: Color,
    val critBorder: Color,
    val critText: Color
)

private val DarkPalette = AlqanasPalette(
    isDark = true,
    background = DarkBg,
    surface = DarkSurface,
    surfaceSolid = DarkSurfaceSolid,
    surfaceSubtle = DarkSurfaceSubtle,
    border = DarkBorder,
    borderStrong = DarkBorderStrong,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textMuted = DarkTextMuted,
    accent = DarkAccent,
    accentInk = DarkAccentInk,
    warnBg = DarkAlertWarnBg,
    warnBorder = DarkAlertWarnBorder,
    warnText = DarkAlertWarnText,
    critBg = DarkAlertCritBg,
    critBorder = DarkAlertCritBorder,
    critText = DarkAlertCritText
)

private val LightPalette = AlqanasPalette(
    isDark = false,
    background = LightBg,
    surface = LightSurface,
    surfaceSolid = LightSurfaceSolid,
    surfaceSubtle = LightSurfaceSubtle,
    border = LightBorder,
    borderStrong = LightBorderStrong,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textMuted = LightTextMuted,
    accent = LightAccent,
    accentInk = LightAccentInk,
    warnBg = LightAlertWarnBg,
    warnBorder = LightAlertWarnBorder,
    warnText = LightAlertWarnText,
    critBg = LightAlertCritBg,
    critBorder = LightAlertCritBorder,
    critText = LightAlertCritText
)

val LocalAlqanasPalette = staticCompositionLocalOf { DarkPalette }

private val AlqanasDarkColorScheme = darkColorScheme(
    primary = DarkAccent,
    onPrimary = DarkAccentInk,
    background = DarkBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurfaceSolid,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    error = DangerRed,
    onError = Color.White
)

private val AlqanasLightColorScheme = lightColorScheme(
    primary = LightAccent,
    onPrimary = LightAccentInk,
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurfaceSolid,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurface,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    error = DangerRed,
    onError = Color.White
)

private val AlqanasShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp)
)

@Composable
fun AlqanasTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }
    val palette = if (useDark) DarkPalette else LightPalette
    val colorScheme = if (useDark) AlqanasDarkColorScheme else AlqanasLightColorScheme

    CompositionLocalProvider(LocalAlqanasPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AlqanasShapes,
            content = content
        )
    }
}
