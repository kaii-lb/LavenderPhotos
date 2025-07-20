package com.kaii.photos.ui.theme

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.kaii.photos.helpers.darkenColor

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

@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun amoledBlackTheme(context: Context, darkenAmount: Float = 0.3f) = run {
    val colorScheme = dynamicDarkColorScheme(context)
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PhotosTheme(
    theme: Int = 0,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when (theme) {
        0 -> {
            val systemInDarkTheme = isSystemInDarkTheme()

            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (systemInDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(
                    context
                )
            } else {
                if (systemInDarkTheme) DarkColorScheme else LightColorScheme
            }
        }


        1 -> if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicDarkColorScheme(
            context
        ) else DarkColorScheme

        2 -> if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicLightColorScheme(
            context
        ) else LightColorScheme

        3 -> {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                amoledBlackTheme(context = context)
            } else {
                DarkColorScheme.copy(
                    background = Color.Black
                )
            }
        }

        else -> LightColorScheme
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
        motionScheme = MotionScheme.expressive()
    )
}
