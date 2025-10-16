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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.widgets.PreferencesSeparatorText
import com.kaii.photos.compose.widgets.PreferencesSwitchRow
import com.kaii.photos.datastore.Behaviour
import com.kaii.photos.datastore.Editing
import com.kaii.photos.datastore.Video
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.TextStylingConstants

@Composable
fun BehaviourSettingsPage() {
    val mainViewModel = LocalMainViewModel.current

    Scaffold(
        topBar = {
            BehaviourSettingsTopBar()
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
                PreferencesSeparatorText(stringResource(id = R.string.settings_views))
            }

            item {
                val rotateInViews by mainViewModel.settings.Behaviour.getRotateInViews()
                    .collectAsStateWithLifecycle(initialValue = true)

                PreferencesSwitchRow(
                    title = stringResource(id = R.string.settings_rotate_in_views),
                    summary = stringResource(id = R.string.settings_rotate_in_views_desc),
                    iconResID = R.drawable.rotate_ccw,
                    checked = rotateInViews,
                    position = RowPosition.Single,
                    showBackground = false,
                    onRowClick = null,
                    onSwitchClick = { checked ->
                        mainViewModel.settings.Behaviour.setRotateInViews(checked)
                    }
                )
            }

            item {
                PreferencesSeparatorText(stringResource(id = R.string.video))
            }

            item {
                val shouldAutoPlay by mainViewModel.settings.Video.getShouldAutoPlay()
                    .collectAsStateWithLifecycle(initialValue = true)

                PreferencesSwitchRow(
                    title = stringResource(id = R.string.video_auto_play),
                    summary = stringResource(id = R.string.video_auto_play_desc),
                    iconResID = R.drawable.auto_play,
                    checked = shouldAutoPlay,
                    position = RowPosition.Single,
                    showBackground = false,
                    onRowClick = null,
                    onSwitchClick = { checked ->
                        mainViewModel.settings.Video.setShouldAutoPlay(checked)
                    }
                )
            }

            item {
                val muteOnStart by mainViewModel.settings.Video.getMuteOnStart()
                    .collectAsStateWithLifecycle(initialValue = false)

                PreferencesSwitchRow(
                    title = stringResource(id = R.string.video_start_muted),
                    summary = stringResource(id = R.string.video_start_muted_desc),
                    iconResID = R.drawable.volume_mute,
                    checked = muteOnStart,
                    position = RowPosition.Single,
                    showBackground = false,
                    onRowClick = null,
                    onSwitchClick = { checked ->
                        mainViewModel.settings.Video.setMuteOnStart(checked)
                    }
                )
            }

            item {
                val openVideosExternally by mainViewModel.settings.Behaviour.getOpenVideosExternally()
                    .collectAsStateWithLifecycle(initialValue = false)

                PreferencesSwitchRow(
                    title = stringResource(id = R.string.video_open_externally),
                    summary = stringResource(id = R.string.video_open_externally_desc),
                    iconResID = R.drawable.new_window,
                    checked = openVideosExternally,
                    position = RowPosition.Single,
                    showBackground = false,
                    onRowClick = null,
                    onSwitchClick = { checked ->
                        mainViewModel.settings.Behaviour.setOpenVideosExternally(checked)
                    }
                )
            }


            item {
                PreferencesSeparatorText(stringResource(id = R.string.editing))
            }

            item {
                val overwriteByDefault by mainViewModel.settings.Editing.getOverwriteByDefault()
                    .collectAsStateWithLifecycle(initialValue = false)

                PreferencesSwitchRow(
                    title = stringResource(id = R.string.editing_overwrite_on_save),
                    summary = stringResource(id = R.string.editing_overwrite_on_save_desc),
                    iconResID = R.drawable.storage,
                    checked = overwriteByDefault,
                    position = RowPosition.Single,
                    showBackground = false,
                    onRowClick = null,
                    onSwitchClick = { checked ->
                        mainViewModel.settings.Editing.setOverwriteByDefault(checked)
                    }
                )
            }

            item {
                val exitOnSave by mainViewModel.settings.Editing.getExitOnSave()
                    .collectAsStateWithLifecycle(initialValue = false)

                PreferencesSwitchRow(
                    title = stringResource(id = R.string.editing_exit_on_save),
                    summary = stringResource(id = R.string.editing_exit_on_save_desc),
                    iconResID = R.drawable.exit,
                    checked = exitOnSave,
                    position = RowPosition.Single,
                    showBackground = false,
                    onRowClick = null,
                    onSwitchClick = { checked ->
                        mainViewModel.settings.Editing.setExitOnSave(checked)
                    }
                )
            }

            item {
                PreferencesSeparatorText(stringResource(id = R.string.navigation))
            }

            item {
                val exitImmediately by mainViewModel.settings.Behaviour.getExitImmediately()
                    .collectAsStateWithLifecycle(initialValue = false)

                PreferencesSwitchRow(
                    title = stringResource(id = R.string.settings_navigation_exit_immediately),
                    summary = stringResource(id = R.string.settings_navigation_exit_immediately_desc),
                    iconResID = R.drawable.exit,
                    checked = exitImmediately,
                    position = RowPosition.Single,
                    showBackground = false,
                    onRowClick = null,
                    onSwitchClick = { checked ->
                        mainViewModel.settings.Behaviour.setExitImmediately(checked)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BehaviourSettingsTopBar() {
    val navController = LocalNavController.current

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