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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.kaii.photos.LocalNavController
import com.kaii.photos.PhotosApplication
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.SelectableButtonListDialog
import com.kaii.photos.compose.dialogs.user_action.TextEntryDialog
import com.kaii.photos.compose.widgets.CheckBoxButtonRow
import com.kaii.photos.compose.widgets.PreferencesRow
import com.kaii.photos.compose.widgets.PreferencesSeparatorText
import com.kaii.photos.compose.widgets.PreferencesSwitchRow
import com.kaii.photos.database.sync.FirstTimeSyncWorker
import com.kaii.photos.database.sync.SyncManager
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.helpers.LogManager
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.helpers.baseInternalStorageDirectory
import com.kaii.photos.mediastore.LAVENDER_FILE_PROVIDER_AUTHORITY
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Composable
fun DebuggingSettingsPage(modifier: Modifier = Modifier) {
    val settings = PhotosApplication.appModule.settings
    val shouldRecordLogs by settings.debugging.getRecordLogs().collectAsStateWithLifecycle(initialValue = false)

    DebuggingSettingsPageImpl(
        shouldRecordLogs = { shouldRecordLogs },
        navController = LocalNavController.current,
        modifier = modifier,
        setRecordLogs = settings.debugging::setRecordLogs,
        addAlbum = { settings.albums.add(listOf(it)) }
    )
}

@Preview
@Composable
private fun DebuggingSettingsPagePreview() {
    DebuggingSettingsPageImpl(
        shouldRecordLogs = { false },
        navController = rememberNavController(),
        modifier = Modifier,
        setRecordLogs = {},
        addAlbum = {}
    )
}

@OptIn(ExperimentalUuidApi::class)
@Composable
private fun DebuggingSettingsPageImpl(
    shouldRecordLogs: () -> Boolean,
    navController: NavController,
    modifier: Modifier,
    setRecordLogs: (value: Boolean) -> Unit,
    addAlbum: (album: AlbumType) -> Unit
) {
    Scaffold(
        topBar = {
            DebuggingSettingsTopBar(navController = navController)
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
                PreferencesSeparatorText(stringResource(id = R.string.logs))
            }

            item {
                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope()
                val showLogTypeDialog = remember { mutableStateOf(false) }

                PreferencesSwitchRow(
                    title = stringResource(id = R.string.record_logs),
                    summary = stringResource(id = R.string.record_logs_desc),
                    iconResID = R.drawable.logs,
                    checked = shouldRecordLogs(),
                    position = RowPosition.Single,
                    showBackground = false,
                    onRowClick = {
                        showLogTypeDialog.value = !showLogTypeDialog.value
                    },
                    onSwitchClick = setRecordLogs
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
                                            LavenderSnackbarEvent.MessageEvent(
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
                                addAlbum(
                                    AlbumType.Folder(
                                        id = Uuid.random().toString(),
                                        name = file.name,
                                        paths = setOf(file.absolutePath),
                                        pinned = false,
                                        immichId = null
                                    )
                                )

                                showAddAlbumsDialog = false
                                true
                            }
                        },
                        onValueChange = { path ->
                            val relativePath = path.trim().replace(baseInternalStorageDirectory, "")
                            val absolutePath = baseInternalStorageDirectory + relativePath

                            File(absolutePath).exists() && relativePath.isNotBlank()
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
                            LavenderSnackbarEvent.LoadingEvent(
                                message = debuggingLoading,
                                isLoading = isLoading,
                                icon = R.drawable.logs
                            )
                        )

                        delay(5000.milliseconds)
                        isLoading.value = false
                    }
                }
            }

            item {
                val resources = LocalResources.current
                val coroutineScope = rememberCoroutineScope()

                PreferencesRow(
                    title = stringResource(id = R.string.debugging_spawn_message_snackbar),
                    iconResID = R.drawable.chat,
                    summary = stringResource(id = R.string.debugging_spawn_message_snackbar_desc),
                    position = RowPosition.Single,
                    showBackground = false
                ) {
                    coroutineScope.launch {
                        LavenderSnackbarController.pushEvent(
                            LavenderSnackbarEvent.MessageEvent(
                                message = resources.getString(R.string.debugging_loading),
                                icon = R.drawable.logs,
                                duration = SnackbarDuration.Short
                            )
                        )
                    }
                }
            }

            item {
                val resources = LocalResources.current
                val coroutineScope = rememberCoroutineScope()

                val percentage = remember { mutableFloatStateOf(0f) }
                val body = remember { mutableStateOf("Progress: 0%") }

                PreferencesRow(
                    title = stringResource(id = R.string.debugging_spawn_progress_snackbar),
                    iconResID = R.drawable.avg_pace,
                    summary = stringResource(id = R.string.debugging_spawn_progress_snackbar_desc),
                    position = RowPosition.Single,
                    showBackground = false
                ) {
                    coroutineScope.launch {
                        percentage.floatValue = 0f
                        body.value = "Progress: 0%"

                        LavenderSnackbarController.pushEvent(
                            LavenderSnackbarEvent.ProgressEvent(
                                message = resources.getString(R.string.debugging_loading),
                                body = body,
                                percentage = percentage,
                                icon = R.drawable.logs
                            )
                        )

                        delay(3000.milliseconds)

                        val count = 5
                        repeat(count) {
                            percentage.floatValue += 1f / count
                            body.value = "Progress: ${(percentage.floatValue * 100f).toString().substring(0, 3).removeSuffix(".")}%"

                            delay(3000.milliseconds)
                        }
                    }
                }
            }

            item {
                PreferencesSeparatorText(
                    text = stringResource(id = R.string.debugging_internals)
                )
            }

            item {
                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope()

                PreferencesRow(
                    title = stringResource(id = R.string.debugging_reset_scan_generation),
                    iconResID = R.drawable.reset,
                    summary = stringResource(id = R.string.debugging_reset_scan_generation_desc),
                    position = RowPosition.Single,
                    showBackground = false
                ) {
                    coroutineScope.launch {
                        SyncManager(context).setGeneration(0)
                        FirstTimeSyncWorker.startWithSnackbar(context)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebuggingSettingsTopBar(
    navController: NavController
) {
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
