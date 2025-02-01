package com.kaii.photos.compose.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.LocalNavController
import com.kaii.photos.MainActivity.Companion.mainViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.PreferencesSeparatorText
import com.kaii.photos.compose.PreferencesRow
import com.kaii.photos.compose.PreferencesSwitchRow
import com.kaii.photos.compose.TextEntryDialog
import com.kaii.photos.datastore.Debugging
import com.kaii.photos.datastore.AlbumsList
import com.kaii.photos.helpers.appStorageDir
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.baseInternalStorageDirectory
import com.kaii.photos.helpers.shareSecuredImage
import com.kaii.photos.mediastore.LAVENDER_FILE_PROVIDER_AUTHORITY
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
        		val logPath = "${context.appStorageDir}/log.txt"
                val shouldRecordLogs by mainViewModel.settings.Debugging.getRecordLogs().collectAsStateWithLifecycle(initialValue = false)

                PreferencesSwitchRow(
                    title = "Record Logs",
                    summary = "Store logs for debugging. Tap to view.",
                    iconResID = R.drawable.logs,
                    checked = shouldRecordLogs,
                    position = RowPosition.Single,
                    showBackground = false,
                    onRowClick = {
                    	val file = File(logPath)

                    	if (file.exists()) {
                    		shareSecuredImage(logPath, context)
                    	} else {
                    		Toast.makeText(context, "No log file is recorded as of yet.", Toast.LENGTH_LONG).show()
                    	}
                    },
                    onSwitchClick = {
                    	mainViewModel.settings.Debugging.setRecordLogs(it)
                    }
                )
        	}

        	item {
                val alternativePickAlbums by mainViewModel.settings.Debugging.getAlternativePickAlbums().collectAsStateWithLifecycle(initialValue = false)

                PreferencesSwitchRow(
                    title = "Alternative Choose Albums",
                    summary = "In case add an album only shows empty folders, use this",
                    iconResID = R.drawable.albums,
                    checked = alternativePickAlbums,
                    position = RowPosition.Single,
                    showBackground = false
                ) {
					mainViewModel.settings.Debugging.setAlternativePickAlbums(it)
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
							val absolutePath = path.trim()
							val file = File(absolutePath)

							if (!file.exists() || absolutePath.replace(baseInternalStorageDirectory, "") == "") {
								false
							} else {
								mainViewModel.settings.AlbumsList.addToAlbumsList(
									file.absolutePath.replace(baseInternalStorageDirectory, "")
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
