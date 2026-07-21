package com.kaii.photos.presentation.ui.theme

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import com.kaii.photos.R

object LavenderThemes {
    enum class Style(
        @param:StringRes val labelId: Int
    ) {
        System(labelId = R.string.look_and_feel_style_system),
        Light(labelId = R.string.look_and_feel_style_light),
        Dark(labelId = R.string.look_and_feel_style_dark),
        Night(labelId = R.string.look_and_feel_style_night);

        val isDark: Boolean
            @Composable
            get() = when (this) {
                Dark, Night -> true
                System if isSystemInDarkTheme() -> true
                else -> false
            }
    }

    val styles: List<Style>
        get() = listOf(
            Style.System, Style.Light, Style.Dark, Style.Night
        )

    enum class Theme(val themeColors: ThemeColors) {
        Apple(themeColors = AppleColors),
        Butterfly(themeColors = ButterflyColors),
        Mint(themeColors = MintColors),
        Cactus(themeColors = CactusColors),
        DragonFruit(themeColors = DragonFruitColors),
        Apricot(themeColors = ApricotColors),
        Forest(themeColors = ForestColors),
        Lemon(themeColors = LemonColors),
        Ocean(themeColors = OceanColors),
        Sunset(themeColors = SunsetColors)
    }

    @SuppressLint("NewApi")
    fun getTheme(
        context: Context,
        theme: Theme,
        style: Style,
        dynamic: Boolean,
        systemInDarkTheme: Boolean
    ) = when (style) {
        Style.System -> {
            if (dynamic) {
                if (systemInDarkTheme) dynamicDarkColorScheme(context)
                else dynamicLightColorScheme(context)
            } else {
                if (systemInDarkTheme) theme.themeColors.darkColorScheme
                else theme.themeColors.lightColorScheme
            }
        }

        Style.Light -> {
            if (dynamic) dynamicLightColorScheme(context)
            else theme.themeColors.lightColorScheme
        }

        Style.Dark -> {
            if (dynamic) dynamicDarkColorScheme(context)
            else theme.themeColors.darkColorScheme
        }

        Style.Night -> NightColors(context, theme.themeColors, dynamic).darkColorScheme
    }
}