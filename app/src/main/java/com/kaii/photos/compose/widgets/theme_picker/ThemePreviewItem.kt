package com.kaii.photos.compose.widgets.theme_picker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kaii.photos.compose.widgets.ShowSelectedState
import com.kaii.photos.presentation.ui.theme.LavenderThemes
import com.kaii.photos.presentation.ui.theme.ThemeConfiguration
import com.kaii.photos.ui.theme.PhotosTheme

@Preview
@Composable
private fun ThemePreviewItemPreview() {
    PhotosTheme(theme = ThemeConfiguration.Default) {
        val themeColors = LavenderThemes.Theme.Ocean.themeColors.darkColorScheme
        ThemePreviewItem(
            contentColor = themeColors.surfaceContainer,
            containerColor = themeColors.primary.copy(alpha = 0.5f),
            selected = true,
            modifier = Modifier
                .width(48.dp)
                .background(LavenderThemes.Theme.Ocean.themeColors.darkColorScheme.background)
        )
    }
}

@Composable
fun ThemePreviewItem(
    contentColor: Color,
    containerColor: Color,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(containerColor.copy(alpha = 0.5f)),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .height(16.dp)
                .scale(if (selected) 0.8f else 1f)
                .clip(
                    shape = RoundedCornerShape(
                        size = if (selected) 16.dp else 0.dp
                    )
                )
                .background(contentColor)
        )

        ShowSelectedState(
            isSelected = { selected },
            showIcon = true,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(20.dp)
        )
    }
}