package com.kaii.photos.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.kaii.photos.LocalNavController
import com.kaii.photos.PhotosApplication
import com.kaii.photos.R
import com.kaii.photos.compose.widgets.PreferencesSeparatorText
import com.kaii.photos.compose.widgets.PreferencesSwitchRow
import com.kaii.photos.compose.widgets.PreferencesThreeStateSwitchRow
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.TextStylingConstants

@Composable
fun BehaviourSettingsPage(
    modifier: Modifier = Modifier
) {
    val settings = PhotosApplication.appModule.settings

    val shouldAutoPlay by settings.video.getShouldAutoPlay().collectAsStateWithLifecycle(initialValue = true)
    val muteOnStart by settings.video.getMuteOnStart().collectAsStateWithLifecycle(initialValue = false)
    val openVideosExternally by settings.behaviour.getOpenVideosExternally().collectAsStateWithLifecycle(initialValue = false)
    val overwriteByDefault by settings.editing.getOverwriteByDefault().collectAsStateWithLifecycle(initialValue = false)
    val exitOnSave by settings.editing.getExitOnSave().collectAsStateWithLifecycle(initialValue = false)
    val exitImmediately by settings.behaviour.getExitImmediately().collectAsStateWithLifecycle(initialValue = false)
    val loopVideos by settings.behaviour.getLoopVideos().collectAsStateWithLifecycle(initialValue = 0)

    BehaviourSettingsPageImpl(
        shouldAutoPlay = { shouldAutoPlay },
        muteOnStart = { muteOnStart },
        openVideosExternally = { openVideosExternally },
        overwriteByDefault = { overwriteByDefault },
        exitOnSave = { exitOnSave },
        exitImmediately = { exitImmediately },
        loopVideos = { loopVideos },
        navController = LocalNavController.current,
        modifier = modifier,
        setShouldAutoPlay = settings.video::setShouldAutoPlay,
        setMuteOnStart = settings.video::setMuteOnStart,
        setOpenVideosExternally = settings.behaviour::setOpenVideosExternally,
        setOverwriteByDefault = settings.editing::setOverwriteByDefault,
        setExitOnSave = settings.editing::setExitOnSave,
        setExitImmediately = settings.behaviour::setExitImmediately,
        setLoopVideos = settings.behaviour::setLoopVideos
    )
}

@Preview
@Composable
fun BehaviourSettingsPagePreview() {
    BehaviourSettingsPageImpl(
        shouldAutoPlay = { false },
        muteOnStart = { false },
        openVideosExternally = { false },
        overwriteByDefault = { false },
        exitOnSave = { false },
        exitImmediately = { false },
        loopVideos = { 0 },
        navController = rememberNavController(),
        modifier = Modifier,
        setShouldAutoPlay = {},
        setMuteOnStart = {},
        setOpenVideosExternally = {},
        setOverwriteByDefault = {},
        setExitOnSave = {},
        setExitImmediately = {},
        setLoopVideos = {}
    )
}

@Composable
private fun BehaviourSettingsPageImpl(
    shouldAutoPlay: () -> Boolean,
    muteOnStart: () -> Boolean,
    openVideosExternally: () -> Boolean,
    overwriteByDefault: () -> Boolean,
    exitOnSave: () -> Boolean,
    exitImmediately: () -> Boolean,
    loopVideos: () -> Int,
    navController: NavController,
    modifier: Modifier,
    setShouldAutoPlay: (value: Boolean) -> Unit,
    setMuteOnStart: (value: Boolean) -> Unit,
    setOpenVideosExternally: (value: Boolean) -> Unit,
    setOverwriteByDefault: (value: Boolean) -> Unit,
    setExitOnSave: (value: Boolean) -> Unit,
    setExitImmediately: (value: Boolean) -> Unit,
    setLoopVideos: (value: Int) -> Unit
) {
    Scaffold(
        topBar = {
            BehaviourSettingsTopBar(navController = navController)
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
                PreferencesSeparatorText(stringResource(id = R.string.video))
            }

            item {
                PreferencesThreeStateSwitchRow(
                    title =
                        when (loopVideos()) {
                            0 -> stringResource(id = R.string.settings_behaviour_loop_never)
                            1 -> stringResource(id = R.string.settings_behaviour_loop_under_30)
                            else -> stringResource(id = R.string.settings_behaviour_loop_always)
                        },
                    summary = stringResource(
                        id = when (loopVideos()) {
                            0 -> R.string.settings_behaviour_loop_never_desc
                            1 -> R.string.settings_behaviour_loop_under_30_desc
                            else -> R.string.settings_behaviour_loop_always_desc
                        }
                    ),
                    iconResID = R.drawable.palette,
                    currentPosition = loopVideos(),
                    trackIcons = listOf(
                        R.drawable.close,
                        R.drawable.replay_30,
                        R.drawable.repeat
                    ),
                    position = RowPosition.Single,
                    showBackground = false,
                    onStateChange = setLoopVideos
                )
            }

            item {
                PreferencesSwitchRow(
                    title = stringResource(id = R.string.video_auto_play),
                    summary = stringResource(id = R.string.video_auto_play_desc),
                    iconResID = R.drawable.auto_play,
                    checked = shouldAutoPlay(),
                    position = RowPosition.Single,
                    showBackground = false,
                    onRowClick = null,
                    onSwitchClick = setShouldAutoPlay
                )
            }

            item {
                PreferencesSwitchRow(
                    title = stringResource(id = R.string.video_start_muted),
                    summary = stringResource(id = R.string.video_start_muted_desc),
                    iconResID = R.drawable.volume_mute,
                    checked = muteOnStart(),
                    position = RowPosition.Single,
                    showBackground = false,
                    onRowClick = null,
                    onSwitchClick = setMuteOnStart
                )
            }

            item {
                PreferencesSwitchRow(
                    title = stringResource(id = R.string.video_open_externally),
                    summary = stringResource(id = R.string.video_open_externally_desc),
                    iconResID = R.drawable.new_window,
                    checked = openVideosExternally(),
                    position = RowPosition.Single,
                    showBackground = false,
                    onRowClick = null,
                    onSwitchClick = setOpenVideosExternally
                )
            }

            item {
                PreferencesSeparatorText(stringResource(id = R.string.editing))
            }

            item {
                PreferencesSwitchRow(
                    title = stringResource(id = R.string.editing_overwrite_on_save),
                    summary = stringResource(id = R.string.editing_overwrite_on_save_desc),
                    iconResID = R.drawable.storage,
                    checked = overwriteByDefault(),
                    position = RowPosition.Single,
                    showBackground = false,
                    onRowClick = null,
                    onSwitchClick = setOverwriteByDefault
                )
            }

            item {
                PreferencesSwitchRow(
                    title = stringResource(id = R.string.editing_exit_on_save),
                    summary = stringResource(id = R.string.editing_exit_on_save_desc),
                    iconResID = R.drawable.exit,
                    checked = exitOnSave(),
                    position = RowPosition.Single,
                    showBackground = false,
                    onRowClick = null,
                    onSwitchClick = setExitOnSave
                )
            }

            item {
                PreferencesSeparatorText(stringResource(id = R.string.navigation))
            }

            item {
                PreferencesSwitchRow(
                    title = stringResource(id = R.string.settings_navigation_exit_immediately),
                    summary = stringResource(id = R.string.settings_navigation_exit_immediately_desc),
                    iconResID = R.drawable.exit,
                    checked = exitImmediately(),
                    position = RowPosition.Single,
                    showBackground = false,
                    onRowClick = null,
                    onSwitchClick = setExitImmediately
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BehaviourSettingsTopBar(
    navController: NavController
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(id = R.string.settings_behaviour),
                fontSize = TextStylingConstants.EXTRA_EXTRA_LARGE_TEXT_SIZE.sp
            )
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    navController.popBackStack()
                }
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