package com.kaii.photos.compose.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.R
import com.kaii.photos.LocalNavController
import com.kaii.photos.compose.PreferencesSwitchRow
import com.kaii.photos.helpers.CustomMaterialTheme
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.MainActivity.Companion.mainViewModel
import com.kaii.photos.compose.PreferencesSeparatorText
import com.kaii.photos.datastore.Permissions
import com.kaii.photos.datastore.Video
import com.kaii.photos.datastore.Editing

@Composable
fun GeneralSettingsPage() {
	val context = LocalContext.current

	Scaffold (
		topBar = {
			GeneralSettingsTopBar()
		}
	) { innerPadding ->
        LazyColumn (
            modifier = Modifier
                .padding(innerPadding)
                .background(CustomMaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    PreferencesSeparatorText("Permissions")
                }

                item {
                	val isMediaManager by mainViewModel.settings.Permissions.getIsMediaManager().collectAsStateWithLifecycle(initialValue = false)

                    val manageMediaLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { _ ->
                        val granted = MediaStore.canManageMedia(context)

                        mainViewModel.onPermissionResult(
                            permission = Manifest.permission.MANAGE_MEDIA,
                            isGranted = granted
                        )

                        mainViewModel.settings.Permissions.setIsMediaManager(granted)
                    }

                    PreferencesSwitchRow(
                        title = "Media Manager",
                        summary = "Better and faster trash/delete/copy/move",
                        iconResID = R.drawable.movie_edit,
                        checked = isMediaManager,
                        position = RowPosition.Single,
                        showBackground = false
                    ) {
                        val intent = Intent(Settings.ACTION_REQUEST_MANAGE_MEDIA)
                        manageMediaLauncher.launch(intent)
                    }
                }

                item {
                    PreferencesSeparatorText("Video")
                }

                item {
                	val shouldAutoPlay by mainViewModel.settings.Video.getShouldAutoPlay().collectAsStateWithLifecycle(initialValue = true)
                	val muteOnStart by mainViewModel.settings.Video.getMuteOnStart().collectAsStateWithLifecycle(initialValue = false)

                    PreferencesSwitchRow(
                        title = "Auto Play Videos",
                        summary = "Start playing videos as soon as they appear on screen",
                        iconResID = R.drawable.auto_play,
                        checked = shouldAutoPlay,
                        position = RowPosition.Single,
                        showBackground = false,
                        onRowClick = null,
                        onSwitchClick = { checked ->
                        	mainViewModel.settings.Video.setShouldAutoPlay(checked)
                        }
                    )

                    PreferencesSwitchRow(
                        title = "Videos Start Muted",
                        summary = "Don't play audio when first starting video playback",
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
                    PreferencesSeparatorText("Editing")
                }

                item {
                	val overwriteByDefault by mainViewModel.settings.Editing.getOverwriteByDefault().collectAsStateWithLifecycle(initialValue = false)

                	PreferencesSwitchRow(
                	    title = "Overwrite on save",
                	    summary = "Default to overwriting instead of saving a copy when editing media.",
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
            }
        }
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GeneralSettingsTopBar() {
	val navController = LocalNavController.current

	TopAppBar(
        title = {
            Text(
                text = "General",
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
                    tint = CustomMaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(24.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = CustomMaterialTheme.colorScheme.background
        )
    )
}
