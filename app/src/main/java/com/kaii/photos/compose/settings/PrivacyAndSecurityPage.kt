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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.widgets.PreferencesSeparatorText
import com.kaii.photos.compose.widgets.PreferencesSwitchRow
import com.kaii.photos.datastore.Permissions
import com.kaii.photos.datastore.PhotoGrid
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.TextStylingConstants

@Composable
fun PrivacyAndSecurityPage() {
    val context = LocalContext.current
    val mainViewModel = LocalMainViewModel.current

    Scaffold(
        topBar = {
            PrivacyAndSecuritySettingsTopBar()
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    PreferencesSeparatorText(stringResource(id = R.string.permissions))
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
                        title = stringResource(id = R.string.permissions_media_manager),
                        summary = stringResource(id = R.string.permissions_media_manager_desc),
                        iconResID = R.drawable.movie_edit,
                        checked = isMediaManager,
                        position = RowPosition.Single,
                        showBackground = false
                    ) {
                        val intent = Intent(Settings.ACTION_REQUEST_MANAGE_MEDIA)
                        manageMediaLauncher.launch(intent)
                    }
                }
            }

            item {
                PreferencesSeparatorText(
                    text = stringResource(id = R.string.permissions_media_management)
                )
            }

            item {
                val confirmToDelete by mainViewModel.settings.Permissions.getConfirmToDelete().collectAsStateWithLifecycle(initialValue = true)

                PreferencesSwitchRow(
                    title = stringResource(id = R.string.permissions_confirm_to_delete),
                    summary = stringResource(id = R.string.permissions_confirm_too_delete_desc),
                    iconResID = R.drawable.confirm_action,
                    position = RowPosition.Single,
                    showBackground = false,
                    checked = confirmToDelete
                ) {
                    mainViewModel.settings.Permissions.setConfirmToDelete(it)
                }
            }

            item {
                val overwriteOnMove by mainViewModel.settings.Permissions.getOverwriteDateOnMove().collectAsStateWithLifecycle(initialValue = true)

                PreferencesSwitchRow(
                    title = stringResource(id = R.string.permissions_overwrite_date_on_move),
                    summary = stringResource(id = R.string.permissions_overwrite_date_on_move_desc),
                    iconResID = R.drawable.clock,
                    position = RowPosition.Single,
                    showBackground = false,
                    checked = !overwriteOnMove
                ) {
                    if (!it) {
                        mainViewModel.settings.PhotoGrid.setSortMode(mode = MediaItemSortMode.LastModified)
                    }
                    mainViewModel.settings.Permissions.setOverwriteDateOnMove(!it)
                }
            }

            item {
                val doNotTrash by mainViewModel.settings.Permissions.getDoNotTrash().collectAsStateWithLifecycle(initialValue = true)

                PreferencesSwitchRow(
                    title = stringResource(id = R.string.permissions_do_not_trash),
                    summary = stringResource(id = R.string.permissions_do_not_trash_desc),
                    iconResID = R.drawable.delete_forever,
                    position = RowPosition.Single,
                    showBackground = false,
                    checked = doNotTrash
                ) {
                    mainViewModel.settings.Permissions.setDoNotTrash(it)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrivacyAndSecuritySettingsTopBar() {
    val navController = LocalNavController.current

    TopAppBar(
        title = {
            Text(
                text = stringResource(id = R.string.settings_privacy),
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
