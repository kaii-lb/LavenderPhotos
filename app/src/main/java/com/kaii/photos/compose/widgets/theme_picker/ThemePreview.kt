package com.kaii.photos.compose.widgets.theme_picker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kaii.photos.presentation.ui.theme.LavenderThemes
import com.kaii.photos.presentation.ui.theme.ThemeConfiguration
import com.kaii.photos.ui.theme.PhotosTheme

@Preview
@Composable
private fun ThemePreviewPreview() {
    PhotosTheme(theme = ThemeConfiguration.Default) {
        ThemePreview(
            previewTheme = LavenderThemes.Theme.Cactus,
            style = LavenderThemes.Style.System,
            dynamic = true,
            modifier = Modifier
                .width(200.dp)
        )
    }
}

@Composable
fun ThemePreview(
    previewTheme: LavenderThemes.Theme,
    style: LavenderThemes.Style,
    dynamic: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDarkScheme = style.isDark
    val themeColors = remember(style, previewTheme, dynamic) {
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
            .background(color = themeColors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, start = 12.dp, end = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .height(18.dp)
                        .width(24.dp)
                        .clip(
                            shape = RoundedCornerShape(
                                topStart = 32.dp, topEnd = 4.dp,
                                bottomStart = 32.dp, bottomEnd = 4.dp
                            )
                        )
                        .background(themeColors.primary)
                )

                Box(
                    modifier = Modifier
                        .height(18.dp)
                        .width(28.dp)
                        .clip(
                            shape = RoundedCornerShape(
                                topStart = 4.dp, topEnd = 32.dp,
                                bottomStart = 4.dp, bottomEnd = 32.dp
                            )
                        )
                        .background(themeColors.surfaceContainerHighest)
                )

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(themeColors.surfaceContainerHighest)
                )
            }

            ThemePreviewSection(
                contentColor = themeColors.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                repeat(3) {
                    ThemePreviewItem(
                        contentColor = themeColors.surfaceContainer,
                        containerColor = themeColors.primary,
                        selected = it == 0,
                        modifier = Modifier
                            .weight(1f)
                    )
                }
            }

            ThemePreviewSection(
                contentColor = themeColors.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
            )

            repeat(2) { i1 ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    repeat(3) { i2 ->
                        ThemePreviewItem(
                            contentColor = themeColors.surfaceContainer,
                            containerColor = themeColors.primary,
                            selected = when (i1) {
                                0 if i2 == 0 -> true
                                0 if i2 == 1 -> true
                                1 if i2 == 0 -> true
                                else -> false
                            },
                            modifier = Modifier
                                .weight(1f)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-12).dp)
                .fillMaxWidth(0.9f)
                .height(38.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = Color.Black,
                    spotColor = Color.Black
                )
                .clip(CircleShape)
                .background(themeColors.surfaceContainer)
                .padding(all = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(themeColors.primary)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color = themeColors.onPrimary)
                )

                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .weight(1f)
                        .clip(CircleShape)
                        .background(color = themeColors.onPrimary)
                )
            }

            repeat(3) {
                Box(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color = themeColors.onSurfaceVariant)
                    )
                }
            }
        }
    }
}