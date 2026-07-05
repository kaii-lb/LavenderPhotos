package com.kaii.photos.compose.widgets.theme_picker

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kaii.photos.presentation.ui.theme.CactusColors
import com.kaii.photos.presentation.ui.theme.ThemeConfiguration
import com.kaii.photos.ui.theme.PhotosTheme

@Preview
@Composable
private fun ThemeColorButtonPreview() {
    PhotosTheme(theme = ThemeConfiguration.Default) {
        val colors = remember {
            CactusColors.getDisplayColors(isDarkScheme = true)
        }

        ThemeColorButton(
            primaryColor = colors.primary,
            tertiaryColor = colors.tertiary,
            primaryContainer = colors.primaryContainer,
            backgroundColor = colors.background,
            selected = { false },
            onClick = {}
        )
    }
}

@Composable
fun ThemeColorButton(
    primaryColor: Color,
    tertiaryColor: Color,
    primaryContainer: Color,
    backgroundColor: Color,
    selected: () -> Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val cornerRadius by animateDpAsState(
        targetValue = if (selected() && enabled) 20.dp else 128.dp,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
    )

    val borderWidth by animateDpAsState(
        targetValue = if (selected() && enabled) 4.dp else 0.dp,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
    )

    val padding by remember {
        derivedStateOf {
            4.dp + borderWidth
        }
    }

    val colorRadius by remember {
        derivedStateOf {
            (cornerRadius - padding).coerceAtLeast(0.dp)
        }
    }

    Column(
        modifier = modifier
            .size(64.dp)
            .clip(shape = RoundedCornerShape(size = cornerRadius))
            .background(color = MaterialTheme.colorScheme.surfaceContainerHighest)
            .then(
                if (borderWidth > 1.dp) Modifier.border(
                    width = borderWidth,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(size = cornerRadius)
                ) else Modifier
            )
            .clickable(onClick = onClick, enabled = enabled)
            .alpha(alpha = if (enabled) 1f else 0.6f)
            .padding(all = padding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .clip(
                        shape = RoundedCornerShape(
                            topStart = colorRadius
                        )
                    )
                    .background(primaryColor)
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .clip(
                        shape = RoundedCornerShape(
                            topEnd = colorRadius
                        )
                    )
                    .background(tertiaryColor)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .clip(
                        shape = RoundedCornerShape(
                            bottomStart = colorRadius
                        )
                    )
                    .background(primaryContainer)
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .clip(
                        shape = RoundedCornerShape(
                            bottomEnd = colorRadius
                        )
                    )
                    .background(backgroundColor)
            )
        }
    }
}