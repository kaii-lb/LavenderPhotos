package com.kaii.photos.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun PhotosTheme(
    darkTheme: Int = 0,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when(darkTheme) {
        0 -> {
        	val systemInDarkTheme = isSystemInDarkTheme()

			if (dynamicColor) {
				if (systemInDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
			} else {
				if (systemInDarkTheme) DarkColorScheme else LightColorScheme
			}
        }


        1 -> if (dynamicColor) dynamicDarkColorScheme(context) else DarkColorScheme
        2 -> if (dynamicColor) dynamicLightColorScheme(context) else LightColorScheme

        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
