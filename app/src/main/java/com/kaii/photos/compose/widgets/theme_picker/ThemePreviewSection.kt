package com.kaii.photos.compose.widgets.theme_picker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kaii.photos.presentation.ui.theme.LavenderThemes
import com.kaii.photos.presentation.ui.theme.ThemeConfiguration
import com.kaii.photos.ui.theme.PhotosTheme
import kotlin.random.Random

@Preview
@Composable
private fun ThemePreviewSectionPreview() {
    PhotosTheme(theme = ThemeConfiguration.Default) {
        ThemePreviewSection(
            contentColor = LavenderThemes.Theme.Ocean.themeColors.darkColorScheme.surfaceContainer,
            modifier = Modifier
                .width(300.dp)
                .background(LavenderThemes.Theme.Ocean.themeColors.darkColorScheme.background)
        )
    }
}

@Composable
fun ThemePreviewSection(
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    val randomWidth = remember {
        0.3f * Random.nextFloat() + 0.5f
    }

    Box(
        modifier = modifier
            .padding(all = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(randomWidth)
                .height(10.dp)
                .clip(CircleShape)
                .background(contentColor)
        )
    }
}