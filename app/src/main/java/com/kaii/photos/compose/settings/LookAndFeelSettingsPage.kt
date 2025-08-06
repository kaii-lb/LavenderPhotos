package com.kaii.photos.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.PreferenceRowWithCustomBody
import com.kaii.photos.compose.PreferencesRow
import com.kaii.photos.compose.PreferencesSeparatorText
import com.kaii.photos.compose.PreferencesSwitchRow
import com.kaii.photos.compose.PreferencesThreeStateSwitchRow
import com.kaii.photos.compose.dialogs.DateFormatDialog
import com.kaii.photos.datastore.LookAndFeel
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.models.multi_album.DisplayDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LookAndFeelSettingsPage() {
    val mainViewModel = LocalMainViewModel.current

    Scaffold(
        topBar = {
            DebuggingSettingsTopBar()
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            item {
                PreferencesSeparatorText(stringResource(id = R.string.settings_theme))
            }

            item {
                val followDarkMode by mainViewModel.settings.LookAndFeel.getFollowDarkMode()
                    .collectAsStateWithLifecycle(initialValue = 0)

                PreferencesThreeStateSwitchRow(
                    title =
                        if (followDarkMode == 0) stringResource(id = R.string.settings_Auto_theme)
                        else if (followDarkMode == 1 || followDarkMode == 3) stringResource(id = R.string.settings_dark_theme)
                        else stringResource(id = R.string.settings_light_theme),
                    summary = stringResource(id = DarkThemeSetting.entries[if (followDarkMode == 3) 1 else followDarkMode].descriptionId),
                    iconResID = R.drawable.palette,
                    currentPosition = if (followDarkMode == 3) 1 else followDarkMode,
                    trackIcons = listOf(
                        R.drawable.theme_auto,
                        R.drawable.theme_dark,
                        R.drawable.theme_light
                    ),
                    position = RowPosition.Single,
                    showBackground = false
                ) {
                    mainViewModel.settings.LookAndFeel.setFollowDarkMode(it)
                }

                PreferencesSwitchRow(
                    title = stringResource(id = R.string.look_and_feel_theme_amoled),
                    summary = stringResource(id = R.string.look_and_feel_theme_amoled_desc),
                    position = RowPosition.Single,
                    iconResID = R.drawable.light_off,
                    showBackground = false,
                    checked = followDarkMode == 3,
                    enabled = followDarkMode == 3 || followDarkMode == 1
                ) { checked ->
                    mainViewModel.settings.LookAndFeel.setFollowDarkMode(if (checked) 3 else 1)
                }
            }

            item {
                PreferencesSeparatorText(stringResource(id = R.string.look_and_feel_styling_separator))
            }

            item {
                var showDateFormatDialog by remember { mutableStateOf(false) }
                val displayDateFormat by mainViewModel.settings.LookAndFeel.getDisplayDateFormat()
                    .collectAsStateWithLifecycle(initialValue = DisplayDateFormat.Default)
                var currentDate by remember {
                    mutableStateOf(
                        Instant.now()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime()
                            .format(DateTimeFormatter.ofPattern(displayDateFormat.format))
                    )
                }

                LaunchedEffect(displayDateFormat) {
                    currentDate = Instant.now()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime()
                        .format(DateTimeFormatter.ofPattern(displayDateFormat.format))
                }

                if (showDateFormatDialog) {
                    DateFormatDialog {
                        showDateFormatDialog = false
                    }
                }

                PreferencesRow(
                    title = stringResource(id = R.string.look_and_feel_date_format),
                    summary = currentDate,
                    iconResID = R.drawable.calendar_month,
                    position = RowPosition.Single,
                    showBackground = false,
                    goesToOtherPage = true
                ) {
                    showDateFormatDialog = true
                }
            }

            item {
                PreferencesSeparatorText(
                    text = stringResource(id = R.string.look_and_feel_grids)
                )
            }

            item {
                val columnSize by mainViewModel.columnSize.collectAsStateWithLifecycle()
                var currentState by remember { mutableIntStateOf(columnSize) }

                PreferenceRowWithCustomBody(
                    icon = R.drawable.dashboard,
                    title = stringResource(id = R.string.look_and_feel_column_size, currentState)
                ) {
                    Slider(
                        value = currentState.toFloat(),
                        onValueChange = {
                            currentState = it.roundToInt()
                            mainViewModel.settings.LookAndFeel.setColumnSize(it.roundToInt())
                        },
                        steps = 3,
                        valueRange = 2f..6f,
                        thumb = {
                            Box(
                                modifier = Modifier
                                    .height(16.dp)
                                    .width(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                    )
                }
            }

            item {
                val columnSize by mainViewModel.albumColumnSize.collectAsStateWithLifecycle()
                var currentState by remember { mutableIntStateOf(columnSize) }

                PreferenceRowWithCustomBody(
                    icon = R.drawable.gallery_thumbnail,
                    title = stringResource(id = R.string.look_and_feel_album_column_size, currentState)
                ) {
                    Slider(
                        value = currentState.toFloat(),
                        onValueChange = {
                            currentState = it.roundToInt()
                            mainViewModel.settings.LookAndFeel.setAlbumColumnSize(it.roundToInt())
                        },
                        steps = 1,
                        valueRange = 2f..4f,
                        thumb = {
                            Box(
                                modifier = Modifier
                                    .height(16.dp)
                                    .width(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                    )
                }
            }

            item {
                val useBlackBackground by mainViewModel.useBlackViewBackgroundColor.collectAsStateWithLifecycle()

                PreferencesSwitchRow(
                    title = stringResource(id = R.string.look_and_feel_use_black_background),
                    summary = stringResource(id = R.string.look_and_feel_use_black_background_desc),
                    position = RowPosition.Single,
                    iconResID = R.drawable.texture,
                    showBackground = false,
                    checked = useBlackBackground
                ) { checked ->
                    mainViewModel.settings.LookAndFeel.setUseBlackBackgroundForViews(checked)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebuggingSettingsTopBar() {
    val navController = LocalNavController.current

    TopAppBar(
        title = {
            Text(
                text = stringResource(id = R.string.settings_look_and_feel),
                fontSize = TextUnit(22f, TextUnitType.Sp)
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
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

enum class DarkThemeSetting(val descriptionId: Int) {
    FollowSystem(R.string.settings_theme_follow),
    ForceDark(R.string.settings_theme_dark),
    ForceLight(R.string.settings_theme_light)
}
