package com.kaii.photos.presentation.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

object AppleColors : ThemeColors {
    override val lightColorScheme: ColorScheme = lightScheme
    override val darkColorScheme: ColorScheme = darkScheme
}

private val lightScheme = lightColorScheme(
    primary = Color(0xFF904B3D),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD3),
    onPrimaryContainer = Color(0xFF733428),
    secondary = Color(0xFF775750),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDAD3),
    onSecondaryContainer = Color(0xFF5D3F39),
    tertiary = Color(0xFF6E5C2E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF9E0A6),
    onTertiaryContainer = Color(0xFF554519),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFFFF8F6),
    onBackground = Color(0xFF231918),
    surface = Color(0xFFFFF8F6),
    onSurface = Color(0xFF231918),
    surfaceVariant = Color(0xFFF5DDD9),
    onSurfaceVariant = Color(0xFF534340),
    outline = Color(0xFF85736F),
    outlineVariant = Color(0xFFD8C2BD),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF392E2C),
    inverseOnSurface = Color(0xFFFFEDE9),
    inversePrimary = Color(0xFFFFB4A5),
    surfaceDim = Color(0xFFE8D6D3),
    surfaceBright = Color(0xFFFFF8F6),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF0EE),
    surfaceContainer = Color(0xFFFCEAE6),
    surfaceContainerHigh = Color(0xFFF7E4E1),
    surfaceContainerHighest = Color(0xFFF1DFDB),
)

private val darkScheme = darkColorScheme(
    primary = Color(0xFFFFB4A5),
    onPrimary = Color(0xFF561E14),
    primaryContainer = Color(0xFF733428),
    onPrimaryContainer = Color(0xFFFFDAD3),
    secondary = Color(0xFFE7BDB5),
    onSecondary = Color(0xFF442A24),
    secondaryContainer = Color(0xFF5D3F39),
    onSecondaryContainer = Color(0xFFFFDAD3),
    tertiary = Color(0xFFDCC48C),
    onTertiary = Color(0xFF3D2E04),
    tertiaryContainer = Color(0xFF554519),
    onTertiaryContainer = Color(0xFFF9E0A6),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1A1110),
    onBackground = Color(0xFFF1DFDB),
    surface = Color(0xFF1A1110),
    onSurface = Color(0xFFF1DFDB),
    surfaceVariant = Color(0xFF534340),
    onSurfaceVariant = Color(0xFFD8C2BD),
    outline = Color(0xFFA08C89),
    outlineVariant = Color(0xFF534340),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFF1DFDB),
    inverseOnSurface = Color(0xFF392E2C),
    inversePrimary = Color(0xFF904B3D),
    surfaceDim = Color(0xFF1A1110),
    surfaceBright = Color(0xFF423734),
    surfaceContainerLowest = Color(0xFF140C0B),
    surfaceContainerLow = Color(0xFF231918),
    surfaceContainer = Color(0xFF271D1B),
    surfaceContainerHigh = Color(0xFF322826),
    surfaceContainerHighest = Color(0xFF3D3230),
)