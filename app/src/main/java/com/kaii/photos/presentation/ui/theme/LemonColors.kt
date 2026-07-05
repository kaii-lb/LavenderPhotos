package com.kaii.photos.presentation.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

object LemonColors : ThemeColors {
    override val lightColorScheme: ColorScheme = lightScheme
    override val darkColorScheme: ColorScheme = darkScheme
}

private val lightScheme = lightColorScheme(
    primary = Color(0xFF666000),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFF119),
    onPrimaryContainer = Color(0xFF736D00),
    secondary = Color(0xFF625E11),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF7B7729),
    onSecondaryContainer = Color(0xFFFFFBFF),
    tertiary = Color(0xFF4C6700),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCCFE57),
    onTertiaryContainer = Color(0xFF577400),
    error = Color(0xFFB9003A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFE8004A),
    onErrorContainer = Color(0xFFFFFBFF),
    background = Color(0xFFFFFAE5),
    onBackground = Color(0xFF1D1C10),
    surface = Color(0xFFFFFAE5),
    onSurface = Color(0xFF1D1C10),
    surfaceVariant = Color(0xFFE8E3C6),
    onSurfaceVariant = Color(0xFF4A4732),
    outline = Color(0xFF7B785F),
    outlineVariant = Color(0xFFCCC7AB),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF323123),
    inverseOnSurface = Color(0xFFF6F1DD),
    inversePrimary = Color(0xFFD6CA00),
    surfaceDim = Color(0xFFDFDAC7),
    surfaceBright = Color(0xFFFFFAE5),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF9F4DF),
    surfaceContainer = Color(0xFFF3EEDA),
    surfaceContainerHigh = Color(0xFFEDE8D4),
    surfaceContainerHighest = Color(0xFFE7E3CF),
)

private val darkScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF353100),
    primaryContainer = Color(0xFFF4E700),
    onPrimaryContainer = Color(0xFF6D6600),
    secondary = Color(0xFFCFCA72),
    onSecondary = Color(0xFF343200),
    secondaryContainer = Color(0xFF7B7729),
    onSecondaryContainer = Color(0xFFFFFBFF),
    tertiary = Color(0xFFFFFFFF),
    onTertiary = Color(0xFF263500),
    tertiaryContainer = Color(0xFFC2F34D),
    onTertiaryContainer = Color(0xFF516D00),
    error = Color(0xFFFFB2B7),
    onError = Color(0xFF67001C),
    errorContainer = Color(0xFFFF506C),
    onErrorContainer = Color(0xFF3F000E),
    background = Color(0xFF151408),
    onBackground = Color(0xFFE7E3CF),
    surface = Color(0xFF151408),
    onSurface = Color(0xFFE7E3CF),
    surfaceVariant = Color(0xFF4A4732),
    onSurfaceVariant = Color(0xFFCCC7AB),
    outline = Color(0xFF959177),
    outlineVariant = Color(0xFF4A4732),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE7E3CF),
    inverseOnSurface = Color(0xFF323123),
    inversePrimary = Color(0xFF666000),
    surfaceDim = Color(0xFF151408),
    surfaceBright = Color(0xFF3B3A2C),
    surfaceContainerLowest = Color(0xFF100F05),
    surfaceContainerLow = Color(0xFF1D1C10),
    surfaceContainer = Color(0xFF212014),
    surfaceContainerHigh = Color(0xFF2C2A1D),
    surfaceContainerHighest = Color(0xFF373528),
)
