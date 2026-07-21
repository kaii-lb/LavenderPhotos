package com.kaii.photos.presentation.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

object ApricotColors : ThemeColors {
    override val lightColorScheme: ColorScheme = lightScheme
    override val darkColorScheme: ColorScheme = darkScheme
}

private val lightScheme = lightColorScheme(
    primary = Color(0xFF9D4300),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE97227),
    onPrimaryContainer = Color(0xFF4F1E00),
    secondary = Color(0xFF934B0E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF09454),
    onSecondaryContainer = Color(0xFF672F00),
    tertiary = Color(0xFF9A3476),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFB84D90),
    onTertiaryContainer = Color(0xFFFFFBFF),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFFFF8F6),
    onBackground = Color(0xFF241914),
    surface = Color(0xFFFFF8F8),
    onSurface = Color(0xFF201A1D),
    surfaceVariant = Color(0xFFFCDCCE),
    onSurfaceVariant = Color(0xFF574238),
    outline = Color(0xFF8B7266),
    outlineVariant = Color(0xFFDEC0B3),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF362E32),
    inverseOnSurface = Color(0xFFFBEDF1),
    inversePrimary = Color(0xFFFFB690),
    surfaceDim = Color(0xFFE4D7DB),
    surfaceBright = Color(0xFFFFF8F8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFEF0F4),
    surfaceContainer = Color(0xFFF8EAEF),
    surfaceContainerHigh = Color(0xFFF2E5E9),
    surfaceContainerHighest = Color(0xFFEDDFE3),
)

private val darkScheme = darkColorScheme(
    primary = Color(0xFFFFB690),
    onPrimary = Color(0xFF552100),
    primaryContainer = Color(0xFFE97227),
    onPrimaryContainer = Color(0xFF4F1E00),
    secondary = Color(0xFFFFB688),
    onSecondary = Color(0xFF512400),
    secondaryContainer = Color(0xFFF09454),
    onSecondaryContainer = Color(0xFF672F00),
    tertiary = Color(0xFFFFAED9),
    onTertiary = Color(0xFF610046),
    tertiaryContainer = Color(0xFFDA69AD),
    onTertiaryContainer = Color(0xFF3D002B),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1B110C),
    onBackground = Color(0xFFF4DED5),
    surface = Color(0xFF181215),
    onSurface = Color(0xFFEDDFE3),
    surfaceVariant = Color(0xFF574238),
    onSurfaceVariant = Color(0xFFDEC0B3),
    outline = Color(0xFFA68B7F),
    outlineVariant = Color(0xFF574238),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFEDDFE3),
    inverseOnSurface = Color(0xFF362E32),
    inversePrimary = Color(0xFF9D4300),
    surfaceDim = Color(0xFF181215),
    surfaceBright = Color(0xFF3F373A),
    surfaceContainerLowest = Color(0xFF130C0F),
    surfaceContainerLow = Color(0xFF201A1D),
    surfaceContainer = Color(0xFF251E21),
    surfaceContainerHigh = Color(0xFF2F282B),
    surfaceContainerHighest = Color(0xFF3B3336),
)