package com.aks_labs.tulsi.compose.settings

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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aks_labs.tulsi.LocalNavController
import com.aks_labs.tulsi.MainActivity.Companion.mainViewModel
import com.aks_labs.tulsi.R
import com.aks_labs.tulsi.compose.CheckBoxButtonRow
import com.aks_labs.tulsi.compose.PreferencesSeparatorText
import com.aks_labs.tulsi.compose.PreferencesRow
import com.aks_labs.tulsi.compose.PreferencesSwitchRow
import com.aks_labs.tulsi.compose.dialogs.TextEntryDialog
import com.aks_labs.tulsi.datastore.Debugging
import com.aks_labs.tulsi.datastore.AlbumsList
import com.aks_labs.tulsi.helpers.RowPosition
import com.aks_labs.tulsi.helpers.baseInternalStorageDirectory
import com.aks_labs.tulsi.helpers.LogManager
import com.aks_labs.tulsi.mediastore.LAVENDER_FILE_PROVIDER_AUTHORITY
import com.kaii.lavender_snackbars.LavenderSnackbarController
import com.kaii.lavender_snackbars.LavenderSnackbarEvents
import com.aks_labs.tulsi.compose.dialogs.SelectableButtonListDialog
import com.aks_labs.tulsi.datastore.AlbumInfo
import com.aks_labs.tulsi.helpers.relativePath
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun DebuggingSettingsPage() {
	Scaffold (
		topBar = {
			DebuggingSettingsTopBar()
		}
	) { innerPadding ->
        LazyColumn (
            modifier = Modifier
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
        	item {
        		PreferencesSeparatorText("Logs")
        	}

        	item {
        		val context = LocalContext.current
                val shouldRecordLogs by mainViewModel.settings.Debugging.getRecordLogs().collectAsStateWithLifecycle(initialValue = false)

                val coroutineScope = rememberCoroutineScope()
                val showLogTypeDialog = remember { mutableStateOf(false) }

                PreferencesSwitchRow(
                    title = "Record Logs",
                    summary = "Store logs for debugging. Tap to view.",
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

                	SelectableButtonListDialog(
                		title = "Choose Logs",
                		showDialog = showLogTypeDialog,
                		buttons = {
                			CheckBoxButtonRow(
                				text = "Previous run's logs",
                				checked = logManager.previousLogPath in chosenPaths
                			) {
                				if (logManager.previousLogPath !in chosenPaths) chosenPaths.add(logManager.previousLogPath)
                				else chosenPaths.remove(logManager.previousLogPath)
                			}

                			CheckBoxButtonRow(
                				text = "Current run's logs",
                				checked = logManager.currentLogPath in chosenPaths
                			) {
                				if (logManager.currentLogPath !in chosenPaths) chosenPaths.add(logManager.currentLogPath)
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
                								message = "No log file is recorded as of yet.",
                								iconResId = R.drawable.no_log,
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
                				        FileProvider.getUriForFile(context, LAVENDER_FILE_PROVIDER_AUTHORITY, File(it))
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
               	    title = "Add an album",
               	    summary = "Use a direct path to add an album",
               	    iconResID = R.drawable.albums,
               	    position = RowPosition.Single,
               	    showBackground = false
               	) {
               		showAddAlbumsDialog = true
               	}

				if (showAddAlbumsDialog) {
	               	TextEntryDialog(
	               		title = "Add Albums Path",
	               		placeholder = "Download/Movies",
	               		onDismiss = {
	               			showAddAlbumsDialog = false
	               		},
	               		onConfirm = { path ->
							val absolutePath = baseInternalStorageDirectory + path.trim()
							val file = File(absolutePath)

							if (!file.exists() || absolutePath.replace(baseInternalStorageDirectory, "") == "") {
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

				PreferencesRow(
					title = "Spawn Loading snackbar",
					iconResID = R.drawable.progress_activity,
					summary = "Debug lavender snackbars whenever compose decides to do something stupid",
					position = RowPosition.Single,
					showBackground = false
				) {
					isLoading.value = true

					coroutineScope.launch {
						LavenderSnackbarController.pushEvent(
							LavenderSnackbarEvents.LoadingEvent(
								message = "Debugging...",
								isLoading = isLoading,
								iconResId = R.drawable.logs
							)
						)

						delay(5000)
						isLoading.value = false
					}
				}

				PreferencesRow(
					title = "Spawn Message snackbar",
					iconResID = R.drawable.progress_activity,
					summary = "Like deprecate a feature and not add an equivalent :D3",
					position = RowPosition.Single,
					showBackground = false
				) {
					coroutineScope.launch {
						LavenderSnackbarController.pushEvent(
							LavenderSnackbarEvents.MessageEvent(
								message = "Debugging...",
								iconResId = R.drawable.logs,
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
                text = "Debugging",
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
                    contentDescription = "Go back to previous page",
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




