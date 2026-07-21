package com.kaii.photos.presentation.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

object SunsetColors : ThemeColors {
    override val lightColorScheme: ColorScheme = lightScheme
    override val darkColorScheme: ColorScheme = darkScheme
}

private val lightScheme = lightColorScheme(
    primary = Color(0xFFC2417B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9E2),
    onPrimaryContainer = Color(0xFF3E0021),
    secondary = Color(0xFF4A61A8),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDDE1FF),
    onSecondaryContainer = Color(0xFF00154B),
    tertiary = Color(0xFF8B5E8A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCD7FAF),
    onTertiaryContainer = Color(0xFF361038),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFAF7F9),
    onBackground = Color(0xFF201A1E),
    surface = Color(0xFFFAF7F9),
    onSurface = Color(0xFF201A1E),
    surfaceVariant = Color(0xFFEEDEE5),
    onSurfaceVariant = Color(0xFF4E444B),
    outline = Color(0xFF80747C),
    outlineVariant = Color(0xFFD1C3CB),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF352E33),
    inverseOnSurface = Color(0xFFF7EEF3),
    inversePrimary = Color(0xFFFFAECE),
    surfaceDim = Color(0xFFE2D7DC),
    surfaceBright = Color(0xFFFAF7F9),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F1F5),
    surfaceContainer = Color(0xFFF1EBEF),
    surfaceContainerHigh = Color(0xFFEBE5E9),
    surfaceContainerHighest = Color(0xFFE5DFE3),
)

private val darkScheme = darkColorScheme(
    primary = Color(0xFFFFAECE),
    onPrimary = Color(0xFF53002E),
    primaryContainer = Color(0xFF721C47),
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = Color(0xFFB8C5FF),
    onSecondary = Color(0xFF192D72),
    secondaryContainer = Color(0xFF32478E),
    onSecondaryContainer = Color(0xFFDDE1FF),
    tertiary = Color(0xFFEBB4E7),
    onTertiary = Color(0xFF49204A),
    tertiaryContainer = Color(0xFF623762),
    onTertiaryContainer = Color(0xFFCD7FAF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF171216),
    onBackground = Color(0xFFEBE0E5),
    surface = Color(0xFF171216),
    onSurface = Color(0xFFEBE0E5),
    surfaceVariant = Color(0xFF4E444B),
    onSurfaceVariant = Color(0xFFD1C3CB),
    outline = Color(0xFF9A8D95),
    outlineVariant = Color(0xFF4E444B),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFEBE0E5),
    inverseOnSurface = Color(0xFF352E33),
    inversePrimary = Color(0xFFC2417B),
    surfaceDim = Color(0xFF171216),
    surfaceBright = Color(0xFF3E373C),
    surfaceContainerLowest = Color(0xFF120D11),
    surfaceContainerLow = Color(0xFF201A1E),
    surfaceContainer = Color(0xFF241E22),
    surfaceContainerHigh = Color(0xFF2F282D),
    surfaceContainerHighest = Color(0xFF3A3338),
)