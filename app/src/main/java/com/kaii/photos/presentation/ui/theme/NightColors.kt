package com.kaii.photos.presentation.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.ui.graphics.Color
import com.kaii.photos.helpers.editing.darkenColor

class NightColors(
    context: Context,
    themeColors: ThemeColors,
    dynamic: Boolean
) : ThemeColors {
    private val colorScheme = run {
        val darkenAmount = 0.3f
        val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dynamic) {
            dynamicDarkColorScheme(context)
        } else {
            themeColors.darkColorScheme
        }

        colorScheme.copy(
            background = Color.Black,
            surface = darkenColor(colorScheme.surface, darkenAmount),
            surfaceVariant = darkenColor(colorScheme.surfaceVariant, darkenAmount),
            primaryContainer = darkenColor(colorScheme.primaryContainer, darkenAmount),
            secondaryContainer = darkenColor(
                colorScheme.secondaryContainer,
                darkenAmount
            ),
            tertiaryContainer = darkenColor(colorScheme.tertiaryContainer, darkenAmount),
            surfaceContainer = darkenColor(colorScheme.surfaceContainer, darkenAmount),
        )
    }

    override val lightColorScheme: ColorScheme = colorScheme
    override val darkColorScheme: ColorScheme = colorScheme
}