package com.kaii.photos.presentation.ui.theme

import androidx.compose.material3.ColorScheme

interface ThemeColors {
    val lightColorScheme: ColorScheme
    val darkColorScheme: ColorScheme

    fun getDisplayColors(isDarkScheme: Boolean): ThemeDisplayColors {
        return if (isDarkScheme) {
            ThemeDisplayColors(
                primary = darkColorScheme.primary,
                tertiary = darkColorScheme.tertiary,
                primaryContainer = darkColorScheme.primaryContainer,
                background = darkColorScheme.background
            )
        } else {
            ThemeDisplayColors(
                primary = lightColorScheme.primary,
                tertiary = lightColorScheme.tertiary,
                primaryContainer = darkColorScheme.primaryContainer,
                background = lightColorScheme.background
            )
        }
    }
}