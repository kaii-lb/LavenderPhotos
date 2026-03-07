package com.kaii.photos.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.settings.DateFormatDialog
import com.kaii.photos.compose.dialogs.settings.TopBarDetailsFormatDialog
import com.kaii.photos.compose.widgets.PreferenceRowWithCustomBody
import com.kaii.photos.compose.widgets.PreferencesRow
import com.kaii.photos.compose.widgets.PreferencesSeparatorText
import com.kaii.photos.compose.widgets.PreferencesSwitchRow
import com.kaii.photos.compose.widgets.PreferencesThreeStateSwitchRow
import com.kaii.photos.di.appModule
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.helpers.TopBarDetailsFormat
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Composable
fun LookAndFeelSettingsPage(modifier: Modifier = Modifier) {
    val settings = LocalContext.current.appModule.settings.lookAndFeel

    val followDarkMode by settings.getFollowDarkMode().collectAsStateWithLifecycle(initialValue = 0)
    val displayDateFormat by settings.getDisplayDateFormat().collectAsStateWithLifecycle(initialValue = DisplayDateFormat.Default)
    val useRoundedCorners by settings.getUseRoundedCorners().collectAsStateWithLifecycle(initialValue = false)
    val showExtraSecureEntry by settings.getShowExtraSecureNav().collectAsStateWithLifecycle(initialValue = false)
    val columnSize by settings.getColumnSize().collectAsStateWithLifecycle(initialValue = 3)
    val albumColumnSize by settings.getAlbumColumnSize().collectAsStateWithLifecycle(initialValue = 2)
    val topBarDetailsFormat by settings.getTopBarDetailsFormat().collectAsStateWithLifecycle(initialValue = TopBarDetailsFormat.FileName)
    val useBlackBackground by settings.getUseBlackBackgroundForViews().collectAsStateWithLifecycle(initialValue = false)
    val blurViews by settings.getBlurViews().collectAsStateWithLifecycle(initialValue = false)

    LookAndFeelSettingsPageImpl(
        followDarkMode = followDarkMode,
        displayDateFormat = displayDateFormat,
        useRoundedCorners = useRoundedCorners,
        showExtraSecureEntry = showExtraSecureEntry,
        columnSize = columnSize,
        albumColumnSize = albumColumnSize,
        topBarDetailsFormat = topBarDetailsFormat,
        useBlackBackground = useBlackBackground,
        blurViews = blurViews,
        modifier = modifier,
        setFollowDarkMode = settings::setFollowDarkMode,
        setDisplayDateFormat = settings::setDisplayDateFormat,
        setUseRoundedCorners = settings::setUseRoundedCorners,
        setShowExtraSecureNav = settings::setShowExtraSecureNav,
        setColumnSize = settings::setColumnSize,
        setAlbumColumnSize = settings::setAlbumColumnSize,
        setTopBarDetailsFormat = settings::setTopBarDetailsFormat,
        setUseBlackBackground = settings::setUseBlackBackgroundForViews,
        setBlurViews = settings::setBlurViews
    )
}

@Preview
@Composable
private fun LookAndFeelSettingsPagePreview() {
    LookAndFeelSettingsPageImpl(
        followDarkMode = 0,
        displayDateFormat = DisplayDateFormat.Default,
        useRoundedCorners = false,
        showExtraSecureEntry = false,
        columnSize = 3,
        albumColumnSize = 2,
        topBarDetailsFormat = TopBarDetailsFormat.FileName,
        useBlackBackground = false,
        blurViews = false,
        modifier = Modifier,
        setFollowDarkMode = { },
        setDisplayDateFormat = {},
        setUseRoundedCorners = {},
        setShowExtraSecureNav = {},
        setColumnSize = {},
        setAlbumColumnSize = {},
        setTopBarDetailsFormat = {},
        setUseBlackBackground = {},
        setBlurViews = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
private fun LookAndFeelSettingsPageImpl(
    followDarkMode: Int,
    displayDateFormat: DisplayDateFormat,
    useRoundedCorners: Boolean,
    showExtraSecureEntry: Boolean,
    columnSize: Int,
    albumColumnSize: Int,
    topBarDetailsFormat: TopBarDetailsFormat,
    useBlackBackground: Boolean,
    blurViews: Boolean,
    modifier: Modifier,
    setFollowDarkMode: (value: Int) -> Unit,
    setDisplayDateFormat: (value: DisplayDateFormat) -> Unit,
    setUseRoundedCorners: (value: Boolean) -> Unit,
    setShowExtraSecureNav: (value: Boolean) -> Unit,
    setColumnSize: (value: Int) -> Unit,
    setAlbumColumnSize: (value: Int) -> Unit,
    setTopBarDetailsFormat: (value: TopBarDetailsFormat) -> Unit,
    setUseBlackBackground: (value: Boolean) -> Unit,
    setBlurViews: (value: Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            DebuggingSettingsTopBar()
        },
        modifier = modifier
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
                PreferencesThreeStateSwitchRow(
                    title =
                        when (followDarkMode) {
                            0 -> stringResource(id = R.string.settings_Auto_theme)
                            1, 3 -> stringResource(id = R.string.settings_dark_theme)
                            else -> stringResource(id = R.string.settings_light_theme)
                        },
                    summary = stringResource(id = DarkThemeSetting.entries[if (followDarkMode == 3) 1 else followDarkMode].descriptionId),
                    iconResID = R.drawable.palette,
                    currentPosition = if (followDarkMode == 3) 1 else followDarkMode,
                    trackIcons = listOf(
                        R.drawable.theme_auto,
                        R.drawable.theme_dark,
                        R.drawable.theme_light
                    ),
                    position = RowPosition.Single,
                    showBackground = false,
                    onStateChange = setFollowDarkMode
                )

                PreferencesSwitchRow(
                    title = stringResource(id = R.string.look_and_feel_theme_amoled),
                    summary = stringResource(id = R.string.look_and_feel_theme_amoled_desc),
                    position = RowPosition.Single,
                    iconResID = R.drawable.light_off,
                    showBackground = false,
                    checked = followDarkMode == 3,
                    enabled = followDarkMode == 3 || followDarkMode == 1 || (followDarkMode == 0 && isSystemInDarkTheme())
                ) { checked ->
                    setFollowDarkMode(if (checked) 3 else 1)
                }
            }

            item {
                PreferencesSeparatorText(stringResource(id = R.string.look_and_feel_styling_separator))
            }

            item {
                var showDateFormatDialog by remember { mutableStateOf(false) }

                var currentDate by remember {
                    mutableStateOf(
                        value = Clock.System.now()
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                            .date
                            .toJavaLocalDate()
                            .format(displayDateFormat.format)
                    )
                }

                LaunchedEffect(displayDateFormat) {
                    currentDate = Clock.System.now()
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .date
                        .toJavaLocalDate()
                        .format(displayDateFormat.format)
                }

                if (showDateFormatDialog) {
                    DateFormatDialog(
                        setDisplayDateFormat = setDisplayDateFormat,
                        onDismiss = { showDateFormatDialog = false }
                    )
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
                val context = LocalContext.current
                var showDialog by remember { mutableStateOf(false) }

                val format by rememberUpdatedState(topBarDetailsFormat)
                var currentDate by remember {
                    mutableStateOf(
                        format.format(context, "Screenshot.png", Clock.System.now().epochSeconds)
                    )
                }

                LaunchedEffect(format) {
                    currentDate = format.format(context, "Screenshot.png", Clock.System.now().epochSeconds)
                }

                if (showDialog) {
                    TopBarDetailsFormatDialog(
                        setTopBarDetailsFormat = setTopBarDetailsFormat,
                        onDismiss = { showDialog = false }
                    )
                }

                PreferencesRow(
                    title = stringResource(id = R.string.look_and_feel_image_details_format),
                    summary = currentDate,
                    iconResID = R.drawable.maybe_megapixel,
                    position = RowPosition.Single,
                    showBackground = false,
                    goesToOtherPage = true
                ) {
                    showDialog = true
                }
            }

            item {
                PreferencesSwitchRow(
                    title = stringResource(id = R.string.look_and_feel_rounded_corners),
                    summary = stringResource(id = R.string.look_and_feel_rounded_corners_desc),
                    position = RowPosition.Single,
                    iconResID = R.drawable.rounded_corner,
                    showBackground = false,
                    checked = useRoundedCorners,
                    onSwitchClick = setUseRoundedCorners
                )
            }

            item {
                PreferencesSeparatorText(
                    text = stringResource(id = R.string.look_and_feel_nav)
                )
            }

            item {
                PreferencesSwitchRow(
                    title = stringResource(id = R.string.look_and_feel_extra_secure_nav),
                    summary = stringResource(id = R.string.look_and_feel_extra_secure_nav_desc),
                    position = RowPosition.Single,
                    iconResID = R.drawable.door_open,
                    showBackground = false,
                    checked = showExtraSecureEntry,
                    onSwitchClick = setShowExtraSecureNav
                )
            }

            item {
                PreferencesSeparatorText(
                    text = stringResource(id = R.string.look_and_feel_grids)
                )
            }

            item {
                var currentState by remember { mutableIntStateOf(columnSize) }

                PreferenceRowWithCustomBody(
                    icon = R.drawable.grid_view,
                    title = stringResource(id = R.string.look_and_feel_column_size, currentState)
                ) {
                    Slider(
                        value = currentState.toFloat(),
                        onValueChange = {
                            currentState = it.roundToInt()
                            setColumnSize(it.roundToInt())
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
                var currentState by remember { mutableIntStateOf(albumColumnSize) }

                PreferenceRowWithCustomBody(
                    icon = R.drawable.gallery_thumbnail,
                    title = stringResource(id = R.string.look_and_feel_album_column_size, currentState)
                ) {
                    Slider(
                        value = currentState.toFloat(),
                        onValueChange = {
                            currentState = it.roundToInt()
                            setAlbumColumnSize(it.roundToInt())
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
                PreferencesSeparatorText(
                    text = stringResource(id = R.string.look_and_feel_views)
                )
            }

            item {
                PreferencesSwitchRow(
                    title = stringResource(id = R.string.look_and_feel_use_black_background),
                    summary = stringResource(id = R.string.look_and_feel_use_black_background_desc),
                    position = RowPosition.Single,
                    iconResID = R.drawable.texture,
                    showBackground = false,
                    checked = useBlackBackground,
                    onSwitchClick = setUseBlackBackground
                )
            }

            item {
                PreferencesSwitchRow(
                    title = stringResource(id = R.string.look_and_feel_views_blur),
                    summary = stringResource(id = R.string.look_and_feel_views_blur_desc),
                    position = RowPosition.Single,
                    iconResID = R.drawable.lens_blur,
                    showBackground = false,
                    checked = blurViews,
                    onSwitchClick = setBlurViews
                )
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
                fontSize = TextUnit(TextStylingConstants.EXTRA_EXTRA_LARGE_TEXT_SIZE, TextUnitType.Sp)
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
