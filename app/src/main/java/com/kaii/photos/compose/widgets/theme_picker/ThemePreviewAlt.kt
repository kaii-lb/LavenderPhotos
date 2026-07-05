package com.kaii.photos.compose.widgets.theme_picker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kaii.photos.presentation.ui.theme.LavenderThemes
import com.kaii.photos.presentation.ui.theme.ThemeConfiguration
import com.kaii.photos.ui.theme.PhotosTheme

@Preview
@Composable
private fun ThemePreviewAltPreview() {
    PhotosTheme(theme = ThemeConfiguration.Default) {
        ThemePreviewAlt(
            previewTheme = LavenderThemes.Theme.Cactus,
            style = LavenderThemes.Style.System,
            dynamic = true,
            modifier = Modifier
                .width(200.dp)
        )
    }
}

@Composable
fun ThemePreviewAlt(
    previewTheme: LavenderThemes.Theme,
    style: LavenderThemes.Style,
    dynamic: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDarkScheme = style.isDark
    val colorScheme = remember(style, previewTheme, dynamic) {
        LavenderThemes.getTheme(
            context = context,
            style = style,
            theme = previewTheme,
            systemInDarkTheme = isDarkScheme,
            dynamic = dynamic
        )
    }

    Box(
        modifier = modifier
            .aspectRatio(9f / 18f)
            .clip(shape = RoundedCornerShape(size = 24.dp))
            .background(colorScheme.background)
            .background(color = colorScheme.tertiary.copy(alpha = 0.6f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(colorScheme.tertiary)
                .align(Alignment.Center)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.TopCenter),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 12.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(space = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(colorScheme.surfaceContainer)
                )

                Box(
                    modifier = Modifier
                        .height(18.dp)
                        .weight(1f)
                        .clip(CircleShape)
                        .background(colorScheme.surfaceContainer)
                )

                Spacer(modifier = Modifier.weight(0.1f))

                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(colorScheme.primary)
                )

                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(shape = RoundedCornerShape(size = 8.dp))
                        .background(colorScheme.primary)
                )
            }

            ThemePreviewAltTags(
                colorScheme = colorScheme,
                modifier = Modifier
                    .padding(all = 8.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 12.dp)
                    .offset(y = (-12).dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    space = 8.dp,
                    alignment = Alignment.CenterHorizontally
                )
            ) {
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .weight(1f)
                        .clip(CircleShape)
                        .background(colorScheme.primaryContainer)
                )

                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(shape = RoundedCornerShape(size = 8.dp))
                        .background(colorScheme.tertiaryContainer)
                )
            }
        }
    }
}