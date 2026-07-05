package com.kaii.photos.presentation.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

object DragonFruitColors : ThemeColors {
    override val lightColorScheme: ColorScheme = lightScheme
    override val darkColorScheme: ColorScheme = darkScheme
}

private val lightScheme = lightColorScheme(
    primary = Color(0xFF9C12B1),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB939CD),
    onPrimaryContainer = Color(0xFFFFFBFF),
    secondary = Color(0xFF85468C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFAB0FE),
    onSecondaryContainer = Color(0xFF793C81),
    tertiary = Color(0xFFB31253),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD5336C),
    onTertiaryContainer = Color(0xFFFFFBFF),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFFFF7FA),
    onBackground = Color(0xFF221921),
    surface = Color(0xFFFFF7FA),
    onSurface = Color(0xFF221921),
    surfaceVariant = Color(0xFFF2DCEF),
    onSurfaceVariant = Color(0xFF514251),
    outline = Color(0xFF837282),
    outlineVariant = Color(0xFFD5C0D2),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF372D37),
    inverseOnSurface = Color(0xFFFDECF9),
    inversePrimary = Color(0xFFFAABFF),
    surfaceDim = Color(0xFFE6D5E2),
    surfaceBright = Color(0xFFFFF7FA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFEFFB),
    surfaceContainer = Color(0xFFFAE9F6),
    surfaceContainerHigh = Color(0xFFF4E3F0),
    surfaceContainerHighest = Color(0xFFEEDEEA),
)

private val darkScheme = darkColorScheme(
    primary = Color(0xFFFAABFF),
    onPrimary = Color(0xFF570065),
    primaryContainer = Color(0xFFDA59EC),
    onPrimaryContainer = Color(0xFF3F0049),
    secondary = Color(0xFFF7ADFB),
    onSecondary = Color(0xFF50155A),
    secondaryContainer = Color(0xFF6D3074),
    onSecondaryContainer = Color(0xFFE89FEC),
    tertiary = Color(0xFFFFB1C2),
    onTertiary = Color(0xFF66002B),
    tertiaryContainer = Color(0xFFFB5187),
    onTertiaryContainer = Color(0xFF4A001D),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF191119),
    onBackground = Color(0xFFEEDEEA),
    surface = Color(0xFF191119),
    onSurface = Color(0xFFEEDEEA),
    surfaceVariant = Color(0xFF514251),
    onSurfaceVariant = Color(0xFFD5C0D2),
    outline = Color(0xFF9E8B9C),
    outlineVariant = Color(0xFF514251),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFEEDEEA),
    inverseOnSurface = Color(0xFF372D37),
    inversePrimary = Color(0xFF9F17B4),
    surfaceDim = Color(0xFF191119),
    surfaceBright = Color(0xFF403640),
    surfaceContainerLowest = Color(0xFF140B14),
    surfaceContainerLow = Color(0xFF221921),
    surfaceContainer = Color(0xFF261D26),
    surfaceContainerHigh = Color(0xFF312730),
    surfaceContainerHighest = Color(0xFF3C323B),
)
