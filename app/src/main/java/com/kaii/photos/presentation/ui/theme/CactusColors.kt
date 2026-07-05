package com.kaii.photos.presentation.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

object CactusColors : ThemeColors {
    override val lightColorScheme: ColorScheme = lightScheme
    override val darkColorScheme: ColorScheme = darkScheme
}

private val lightScheme = lightColorScheme(
    primary = Color(0xFF236D00),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF50D809),
    onPrimaryContainer = Color(0xFF1B5800),
    secondary = Color(0xFF256D04),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFA8F783),
    onSecondaryContainer = Color(0xFF2C730C),
    tertiary = Color(0xFF006C4E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF00D79E),
    onTertiaryContainer = Color(0xFF00583E),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFF3FDE8),
    onBackground = Color(0xFF161E12),
    surface = Color(0xFFF3FDE8),
    onSurface = Color(0xFF161E12),
    surfaceVariant = Color(0xFFD9E7CC),
    onSurfaceVariant = Color(0xFF3E4A36),
    outline = Color(0xFF6E7B64),
    outlineVariant = Color(0xFFBDCBB1),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF2B3325),
    inverseOnSurface = Color(0xFFEBF4E0),
    inversePrimary = Color(0xFF5AE21C),
    surfaceDim = Color(0xFFD4DDC9),
    surfaceBright = Color(0xFFF3FDE8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEDF7E2),
    surfaceContainer = Color(0xFFE8F1DD),
    surfaceContainerHigh = Color(0xFFE2EBD7),
    surfaceContainerHighest = Color(0xFFDCE6D2),
)

private val darkScheme = darkColorScheme(
    primary = Color(0xFF6DF634),
    onPrimary = Color(0xFF0F3900),
    primaryContainer = Color(0xFF50D809),
    onPrimaryContainer = Color(0xFF1B5800),
    secondary = Color(0xFF8DDA6A),
    onSecondary = Color(0xFF0F3900),
    secondaryContainer = Color(0xFF236A01),
    onSecondaryContainer = Color(0xFF9BE977),
    tertiary = Color(0xFF45F4B9),
    onTertiary = Color(0xFF003827),
    tertiaryContainer = Color(0xFF00D79E),
    onTertiaryContainer = Color(0xFF00583E),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0E150A),
    onBackground = Color(0xFFDCE6D2),
    surface = Color(0xFF0E150A),
    onSurface = Color(0xFFDCE6D2),
    surfaceVariant = Color(0xFF3E4A36),
    onSurfaceVariant = Color(0xFFBDCBB1),
    outline = Color(0xFF87957D),
    outlineVariant = Color(0xFF3E4A36),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFDCE6D2),
    inverseOnSurface = Color(0xFF2B3325),
    inversePrimary = Color(0xFF236D00),
    surfaceDim = Color(0xFF0E150A),
    surfaceBright = Color(0xFF333C2E),
    surfaceContainerLowest = Color(0xFF091006),
    surfaceContainerLow = Color(0xFF161E12),
    surfaceContainer = Color(0xFF1A2215),
    surfaceContainerHigh = Color(0xFF242C1F),
    surfaceContainerHighest = Color(0xFF2F372A),
)