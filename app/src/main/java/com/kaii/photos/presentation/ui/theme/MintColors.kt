package com.kaii.photos.presentation.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

object MintColors : ThemeColors {
    override val lightColorScheme: ColorScheme = lightScheme
    override val darkColorScheme: ColorScheme = darkScheme
}

private val lightScheme = lightColorScheme(
    primary = Color(0xFF006099),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF047ABF),
    onPrimaryContainer = Color(0xFFFDFCFF),
    secondary = Color(0xFF45617D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC0DDFE),
    onSecondaryContainer = Color(0xFF46617D),
    tertiary = Color(0xFF00696D),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF3F9EA3),
    onTertiaryContainer = Color(0xFF002F32),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFF7F9FF),
    onBackground = Color(0xFF181C20),
    surface = Color(0xFFFAF9FB),
    onSurface = Color(0xFF1B1C1D),
    surfaceVariant = Color(0xFFDCE3EE),
    onSurfaceVariant = Color(0xFF404750),
    outline = Color(0xFF707882),
    outlineVariant = Color(0xFFC0C7D2),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF303032),
    inverseOnSurface = Color(0xFFF2F0F2),
    inversePrimary = Color(0xFF98CBFF),
    surfaceDim = Color(0xFFDBD9DC),
    surfaceBright = Color(0xFFFAF9FB),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F3F5),
    surfaceContainer = Color(0xFFEFEDF0),
    surfaceContainerHigh = Color(0xFFE9E8EA),
    surfaceContainerHighest = Color(0xFFE3E2E4),
)

private val darkScheme = darkColorScheme(
    primary = Color(0xFF98CBFF),
    onPrimary = Color(0xFF003354),
    primaryContainer = Color(0xFF3D96DE),
    onPrimaryContainer = Color(0xFF000D1B),
    secondary = Color(0xFFADC9E9),
    onSecondary = Color(0xFF14324C),
    secondaryContainer = Color(0xFF2D4964),
    onSecondaryContainer = Color(0xFF9BB8D7),
    tertiary = Color(0xFF7AD5DA),
    onTertiary = Color(0xFF003739),
    tertiaryContainer = Color(0xFF3F9EA3),
    onTertiaryContainer = Color(0xFF002F32),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF101418),
    onBackground = Color(0xFFE0E2E9),
    surface = Color(0xFF121315),
    onSurface = Color(0xFFE3E2E4),
    surfaceVariant = Color(0xFF404750),
    onSurfaceVariant = Color(0xFFC0C7D2),
    outline = Color(0xFF8A919C),
    outlineVariant = Color(0xFF404750),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE3E2E4),
    inverseOnSurface = Color(0xFF303032),
    inversePrimary = Color(0xFF00639D),
    surfaceDim = Color(0xFF121315),
    surfaceBright = Color(0xFF38393B),
    surfaceContainerLowest = Color(0xFF0D0E10),
    surfaceContainerLow = Color(0xFF1B1C1D),
    surfaceContainer = Color(0xFF1F2021),
    surfaceContainerHigh = Color(0xFF292A2C),
    surfaceContainerHighest = Color(0xFF343537),
)