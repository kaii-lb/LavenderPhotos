package com.kaii.photos.compose.settings

import android.content.Intent
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.widgets.CheckBoxButtonRow
import com.kaii.photos.compose.widgets.PreferencesRow
import com.kaii.photos.compose.widgets.PreferencesSeparatorText
import com.kaii.photos.compose.widgets.PreferencesSwitchRow
import com.kaii.photos.compose.dialogs.SelectableButtonListDialog
import com.kaii.photos.compose.dialogs.TextEntryDialog
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.AlbumsList
import com.kaii.photos.datastore.Debugging
import com.kaii.photos.helpers.LogManager
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.helpers.baseInternalStorageDirectory
import com.kaii.photos.helpers.relativePath
import com.kaii.photos.mediastore.LAVENDER_FILE_PROVIDER_AUTHORITY
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun DebuggingSettingsPage() {
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
                PreferencesSeparatorText(stringResource(id = R.string.logs))
            }

            item {
                val context = LocalContext.current
                val shouldRecordLogs by mainViewModel.settings.Debugging.getRecordLogs()
                    .collectAsStateWithLifecycle(initialValue = false)

                val coroutineScope = rememberCoroutineScope()
                val showLogTypeDialog = remember { mutableStateOf(false) }

                PreferencesSwitchRow(
                    title = stringResource(id = R.string.record_logs),
                    summary = stringResource(id = R.string.record_logs_desc),
                    iconResID = R.drawable.logs,
                    checked = shouldRecordLogs,
                    position = RowPosition.Single,
                    showBackground = false,
                    onRowClick = {
                        showLogTypeDialog.value = !showLogTypeDialog.value
                    },
                    onSwitchClick = {
                        mainViewModel.settings.Debugging.setRecordLogs(it)
                    }
                )

                if (showLogTypeDialog.value) {
                    val logManager = remember { LogManager(context = context) }
                    val chosenPaths = remember { mutableStateListOf(logManager.previousLogPath) }
                    val noLogFile = stringResource(id = R.string.log_file_non_existent)

                    SelectableButtonListDialog(
                        title = stringResource(id = R.string.choose_logs),
                        showDialog = showLogTypeDialog,
                        buttons = {
                            CheckBoxButtonRow(
                                text = stringResource(id = R.string.previous_run_logs),
                                checked = logManager.previousLogPath in chosenPaths
                            ) {
                                if (logManager.previousLogPath !in chosenPaths) chosenPaths.add(
                                    logManager.previousLogPath
                                )
                                else chosenPaths.remove(logManager.previousLogPath)
                            }

                            CheckBoxButtonRow(
                                text = stringResource(id = R.string.current_run_logs),
                                checked = logManager.currentLogPath in chosenPaths
                            ) {
                                if (logManager.currentLogPath !in chosenPaths) chosenPaths.add(
                                    logManager.currentLogPath
                                )
                                else chosenPaths.remove(logManager.currentLogPath)
                            }
                        },
                        onConfirm = {
                            val existing = chosenPaths.filter {
                                val exists = File(it).exists()

                                if (!exists) {
                                    coroutineScope.launch {
                                        LavenderSnackbarController.pushEvent(
                                            LavenderSnackbarEvents.MessageEvent(
                                                message = noLogFile,
                                                icon = R.drawable.no_log,
                                                duration = SnackbarDuration.Short
                                            )
                                        )
                                    }
                                }

                                exists
                            }

                            if (existing.isNotEmpty()) {
                                val intent = Intent().apply {
                                    action = Intent.ACTION_SEND_MULTIPLE
                                    type = "text/plain"
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }

                                val fileUris = ArrayList(
                                    existing.map {
                                        FileProvider.getUriForFile(
                                            context,
                                            LAVENDER_FILE_PROVIDER_AUTHORITY,
                                            File(it)
                                        )
                                    }
                                )

                                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris)

                                context.startActivity(Intent.createChooser(intent, null))
                            }
                        }
                    )
                }
            }

            item {
                var showAddAlbumsDialog by remember { mutableStateOf(false) }

                PreferencesRow(
                    title = stringResource(id = R.string.add_album),
                    summary = stringResource(id = R.string.direct_path_album),
                    iconResID = R.drawable.albums,
                    position = RowPosition.Single,
                    showBackground = false
                ) {
                    showAddAlbumsDialog = true
                }

                if (showAddAlbumsDialog) {
                    TextEntryDialog(
                        title = stringResource(id = R.string.album_add_path),
                        placeholder = stringResource(id = R.string.album_add_path_placeholder),
                        onDismiss = {
                            showAddAlbumsDialog = false
                        },
                        onConfirm = { path ->
                            val absolutePath = baseInternalStorageDirectory + path.trim()
                            val file = File(absolutePath)

                            if (!file.exists() || absolutePath.replace(
                                    baseInternalStorageDirectory,
                                    ""
                                ) == ""
                            ) {
                                false
                            } else {
                                mainViewModel.settings.AlbumsList.addToAlbumsList(
                                    AlbumInfo(
                                        id = file.hashCode(),
                                        name = file.name,
                                        paths = listOf(file.relativePath)
                                    )
                                )

                                showAddAlbumsDialog = false
                                true
                            }
                        },
                        onValueChange = { path ->
                            val relativePath = path.trim().replace(baseInternalStorageDirectory, "")
                            val absolutePath = baseInternalStorageDirectory + relativePath

                            !File(absolutePath).exists() || relativePath == ""
                        }
                    )
                }
            }

            item {
                val isLoading = remember { mutableStateOf(false) }
                val coroutineScope = rememberCoroutineScope()
                val debuggingLoading = stringResource(id = R.string.debugging_loading)

                PreferencesRow(
                    title = stringResource(id = R.string.debugging_spawn_loading_snackbar),
                    iconResID = R.drawable.progress_activity,
                    summary = stringResource(id = R.string.debugging_spawn_loading_snackbar_desc),
                    position = RowPosition.Single,
                    showBackground = false
                ) {
                    isLoading.value = true

                    coroutineScope.launch {
                        LavenderSnackbarController.pushEvent(
                            LavenderSnackbarEvents.LoadingEvent(
                                message = debuggingLoading,
                                isLoading = isLoading,
                                icon = R.drawable.logs
                            )
                        )

                        delay(5000)
                        isLoading.value = false
                    }
                }

                PreferencesRow(
                    title = stringResource(id = R.string.debugging_spawn_message_snackbar),
                    iconResID = R.drawable.progress_activity,
                    summary = stringResource(id = R.string.debugging_spawn_message_snackbar_desc),
                    position = RowPosition.Single,
                    showBackground = false
                ) {
                    coroutineScope.launch {
                        LavenderSnackbarController.pushEvent(
                            LavenderSnackbarEvents.MessageEvent(
                                message = debuggingLoading,
                                icon = R.drawable.logs,
                                duration = SnackbarDuration.Short
                            )
                        )
                    }
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
                text = stringResource(id = R.string.debugging),
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
