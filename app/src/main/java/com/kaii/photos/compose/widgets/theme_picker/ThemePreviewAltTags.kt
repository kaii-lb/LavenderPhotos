package com.kaii.photos.compose.widgets.theme_picker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kaii.photos.presentation.ui.theme.LavenderThemes
import com.kaii.photos.presentation.ui.theme.ThemeConfiguration
import com.kaii.photos.ui.theme.PhotosTheme

@Preview
@Composable
private fun ThemePreviewAltTagsPreview() {
    PhotosTheme(theme = ThemeConfiguration.Default) {
        ThemePreviewAltTags(
            colorScheme = LavenderThemes.Theme.Ocean.themeColors.lightColorScheme,
            modifier = Modifier
                .width(140.dp)
        )
    }
}

@Composable
fun ThemePreviewAltTags(
    colorScheme: ColorScheme,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .clip(CircleShape)
                    .background(colorScheme.surfaceContainerHigh)
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(colorScheme.primary)
            )
        }

        Column(
            modifier = Modifier
                .clip(shape = RoundedCornerShape(size = 16.dp))
                .background(colorScheme.surfaceContainerHigh)
                .padding(all = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .height(12.dp)
                        .weight(1f)
                        .clip(CircleShape)
                        .background(colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                )

                Spacer(modifier = Modifier.weight(0.6f))

                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .fillMaxWidth(0.8f)
                        .clip(CircleShape)
                        .background(colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}