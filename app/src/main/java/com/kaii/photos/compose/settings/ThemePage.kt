package com.kaii.photos.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.widgets.PreferencesSwitchRow
import com.kaii.photos.compose.widgets.preferences.PreferencesStyleRow
import com.kaii.photos.compose.widgets.preferences.PreferencesThemeRow
import com.kaii.photos.compose.widgets.theme_picker.ThemePreview
import com.kaii.photos.compose.widgets.theme_picker.ThemePreviewAlt
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.models.theme.ThemeViewModel
import com.kaii.photos.presentation.ui.theme.LavenderThemes
import com.kaii.photos.presentation.ui.theme.ThemeConfiguration
import com.kaii.photos.ui.theme.PhotosTheme

@Preview
@Composable
private fun ThemePagePreview() {
    PhotosTheme(theme = ThemeConfiguration.Default) {
        var previewTheme by remember {
            mutableStateOf(LavenderThemes.Theme.Cactus)
        }

        var style by remember {
            mutableStateOf(LavenderThemes.Style.System)
        }

        var dynamic by remember {
            mutableStateOf(false)
        }

        ThemePageImpl(
            previewTheme = { previewTheme },
            themes = LavenderThemes.Theme.entries,
            style = { style },
            dynamic = { dynamic },
            hasChanges = { false },
            navController = rememberNavController(),
            setPreviewTheme = { previewTheme = it },
            setStyle = { style = it },
            setDynamic = { dynamic = it },
            applyTheme = {}
        )
    }
}

@Composable
fun ThemePage(
    viewModel: ThemeViewModel,
    modifier: Modifier = Modifier
) {
    val previewConfiguration by viewModel.previewConfiguration.collectAsStateWithLifecycle()
    val hasChanges by viewModel.hasChanges.collectAsStateWithLifecycle()

    ThemePageImpl(
        previewTheme = {
            previewConfiguration.theme
        },
        themes = viewModel.themes,
        style = {
            previewConfiguration.style
        },
        dynamic = {
            previewConfiguration.dynamic
        },
        hasChanges = {
            hasChanges
        },
        navController = LocalNavController.current,
        modifier = modifier,
        setPreviewTheme = viewModel::setPreviewTheme,
        setStyle = viewModel::setStyle,
        setDynamic = viewModel::setDynamic,
        applyTheme = viewModel::applyThemeConfiguration
    )
}

@Composable
private fun ThemePageImpl(
    previewTheme: () -> LavenderThemes.Theme,
    themes: List<LavenderThemes.Theme>,
    style: () -> LavenderThemes.Style,
    dynamic: () -> Boolean,
    hasChanges: () -> Boolean,
    navController: NavController,
    modifier: Modifier = Modifier,
    setPreviewTheme: (theme: LavenderThemes.Theme) -> Unit,
    setStyle: (style: LavenderThemes.Style) -> Unit,
    setDynamic: (dynamic: Boolean) -> Unit,
    applyTheme: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.look_and_feel_colors),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.popBackStack()
                        },
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.back_arrow),
                            contentDescription = stringResource(id = R.string.return_to_previous_page),
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = applyTheme,
                        shapes = ButtonDefaults.shapesFor(48.dp),
                        enabled = hasChanges()
                    ) {
                        Text(
                            text = stringResource(id = R.string.apply),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(all = 8.dp),
            verticalArrangement = Arrangement.spacedBy(
                space = 4.dp,
                alignment = Alignment.Top
            ),
            horizontalAlignment = Alignment.Start
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape = RoundedCornerShape(size = 32.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(all = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(
                        space = 16.dp,
                        alignment = Alignment.CenterHorizontally
                    )
                ) {
                    ThemePreview(
                        previewTheme = previewTheme(),
                        style = style(),
                        dynamic = dynamic(),
                        modifier = Modifier
                            .weight(1f)
                    )

                    ThemePreviewAlt(
                        previewTheme = previewTheme(),
                        style = style(),
                        dynamic = dynamic(),
                        modifier = Modifier
                            .weight(1f)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            item {
                PreferencesStyleRow(
                    styles = LavenderThemes.styles,
                    selected = { it == style() },
                    position = RowPosition.Top,
                    onSelect = setStyle
                )
            }

            item {
                PreferencesSwitchRow(
                    title = stringResource(id = R.string.look_and_feel_style_dynamic),
                    summary = stringResource(id = R.string.look_and_feel_style_dynamic_desc),
                    position = RowPosition.Middle,
                    iconResID = R.drawable.wallpaper,
                    checked = dynamic(),
                    titleStyle = MaterialTheme.typography.titleLarge,
                    iconPadding = 8.dp,
                    onSwitchClick = setDynamic
                )
            }

            item {
                PreferencesThemeRow(
                    themes = themes,
                    selected = { it == previewTheme() },
                    style = style(),
                    position = RowPosition.Bottom,
                    enabled = !dynamic(),
                    onSelect = setPreviewTheme
                )
            }
        }
    }
}