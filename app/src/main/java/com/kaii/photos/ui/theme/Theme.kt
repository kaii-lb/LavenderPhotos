package com.kaii.photos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.kaii.photos.presentation.ui.theme.LavenderThemes
import com.kaii.photos.presentation.ui.theme.ThemeConfiguration

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PhotosTheme(
    theme: ThemeConfiguration,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    val colorScheme = remember(isDark, theme) {
        LavenderThemes.getTheme(
            context = context,
            style = theme.style,
            theme = theme.theme,
            dynamic = theme.dynamic,
            systemInDarkTheme = isDark
        )
    }

    CompositionLocalProvider(
        LocalExtraColorsPalette provides ExtraColorsPalette()
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
            motionScheme = MotionScheme.expressive()
        )
    }
}
