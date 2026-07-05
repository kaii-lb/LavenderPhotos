package com.kaii.photos.compose.widgets.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kaii.photos.R
import com.kaii.photos.compose.widgets.theme_picker.ThemeColorButton
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.presentation.ui.theme.LavenderThemes
import com.kaii.photos.presentation.ui.theme.ThemeConfiguration
import com.kaii.photos.ui.theme.PhotosTheme

@Preview
@Composable
private fun PreferencesThemeRowPreview() {
    PhotosTheme(theme = ThemeConfiguration.Default) {
        PreferencesThemeRow(
            themes = LavenderThemes.Theme.entries,
            selected = { it == LavenderThemes.Theme.Apple },
            style = LavenderThemes.Style.Light,
            position = RowPosition.Single,
            modifier = Modifier
                .width(300.dp),
            onSelect = {}
        )
    }
}

@Composable
fun PreferencesThemeRow(
    themes: List<LavenderThemes.Theme>,
    selected: (theme: LavenderThemes.Theme) -> Boolean,
    style: LavenderThemes.Style,
    position: RowPosition,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onSelect: (theme: LavenderThemes.Theme) -> Unit
) {
    CustomBodyPreferencesRow(
        title = stringResource(id = R.string.look_and_feel_palette),
        summary = stringResource(id = R.string.look_and_feel_palette_desc),
        icon = R.drawable.palette,
        modifier = modifier,
        position = position,
        enabled = enabled
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                space = 8.dp,
                alignment = Alignment.Start
            )
        ) {
            items(
                count = themes.size,
                key = {
                    themes[it].name
                }
            ) { index ->
                val isDark = style.isDark
                val themeColors = remember(index, isDark) {
                    themes[index].themeColors.getDisplayColors(isDark)
                }

                ThemeColorButton(
                    primaryColor = themeColors.primary,
                    tertiaryColor = themeColors.tertiary,
                    primaryContainer = themeColors.primaryContainer,
                    backgroundColor = themeColors.background,
                    enabled = enabled,
                    selected = { selected(themes[index]) },
                    onClick = {
                        onSelect(themes[index])
                    }
                )
            }
        }
    }
}