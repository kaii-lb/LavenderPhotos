package com.kaii.photos.compose.settings

import android.Manifest
import android.content.Intent
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.kaii.photos.compose.PreferencesRow
import com.kaii.photos.compose.PreferencesSwitchRow
import com.kaii.photos.helpers.CustomMaterialTheme
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.MainActivity.Companion.mainViewModel

@Composable
fun GeneralSettingsPage() {
	val isMediaManager = mainViewModel.settingsPermissions.isMediaManager.collectAsStateWithLifecycle(initialValue = false)
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
        	item {
        		Text(
        			text = "Permissions",
        			fontSize = TextUnit(16f, TextUnitType.Sp),
        			color = CustomMaterialTheme.colorScheme.primary,
        			modifier = Modifier
        				.padding(12.dp)
        		)
        	}

        	item {
                val manageMediaLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { _ ->
                    val granted = MediaStore.canManageMedia(context)

                    mainViewModel.onPermissionResult(
                        permission = Manifest.permission.MANAGE_MEDIA,
                        isGranted = granted
                    )

                    mainViewModel.settingsPermissions.setIsMediaManager(granted)
                }

                PreferencesSwitchRow(
                    title = "Media Manager",
                    summary = "Better and faster trash/delete/copy/move",
                    iconResID = R.drawable.movie_edit,
                    checked = isMediaManager,
                    position = RowPosition.Single,
                    showBackground = false,
                    height = 80.dp,
                ) {
                    val intent = Intent(Settings.ACTION_REQUEST_MANAGE_MEDIA)
                    manageMediaLauncher.launch(intent)
                }
        	}
        }
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GeneralSettingsTopBar() {
	val navController = LocalNavController.current ?: return

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
        scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = CustomMaterialTheme.colorScheme.background
        ),
    )
}