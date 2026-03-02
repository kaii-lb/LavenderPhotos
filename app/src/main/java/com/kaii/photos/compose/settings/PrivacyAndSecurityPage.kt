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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.widgets.PreferencesSeparatorText
import com.kaii.photos.compose.widgets.PreferencesSwitchRow
import com.kaii.photos.di.appModule
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.permissions.StartupManager

@Composable
fun PrivacyAndSecurityPage(
    startupManager: StartupManager,
    modifier: Modifier = Modifier
) {
    val settings = LocalContext.current.appModule.settings.permissions

    val isMediaManager by settings.getIsMediaManager().collectAsStateWithLifecycle(initialValue = false)
    val confirmToDelete by settings.getConfirmToDelete().collectAsStateWithLifecycle(initialValue = true)
    val preserveDate by settings.getPreserveDateOnMove().collectAsStateWithLifecycle(initialValue = true)
    val doNotTrash by settings.getDoNotTrash().collectAsStateWithLifecycle(initialValue = true)

    PrivacyAndSecurityPageImpl(
        isMediaManager = isMediaManager,
        confirmToDelete = confirmToDelete,
        preserveDate = preserveDate,
        doNotTrash = doNotTrash,
        modifier = modifier,
        setIsMediaManager = settings::setIsMediaManager,
        setConfirmToDelete = settings::setConfirmToDelete,
        setPreserveDate = settings::setPreserveDateOnMove,
        setDoNotTrash = settings::setDoNotTrash,
        onPermissionResult = {
            startupManager.onPermissionResult(
                permission = Manifest.permission.MANAGE_MEDIA,
                isGranted = it
            )
        }
    )
}

@Preview
@Composable
fun PrivacyAndSecurityPagePreview() {
    PrivacyAndSecurityPageImpl(
        isMediaManager = false,
        confirmToDelete = false,
        preserveDate = false,
        doNotTrash = false,
        modifier = Modifier,
        setIsMediaManager = {},
        setConfirmToDelete = {},
        setPreserveDate = {},
        setDoNotTrash = {},
        onPermissionResult = {}
    )
}

@Composable
private fun PrivacyAndSecurityPageImpl(
    isMediaManager: Boolean,
    confirmToDelete: Boolean,
    preserveDate: Boolean,
    doNotTrash: Boolean,
    modifier: Modifier,
    setIsMediaManager: (value: Boolean) -> Unit,
    setConfirmToDelete: (value: Boolean) -> Unit,
    setPreserveDate: (value: Boolean) -> Unit,
    setDoNotTrash: (value: Boolean) -> Unit,
    onPermissionResult: (granted: Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            PrivacyAndSecuritySettingsTopBar()
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    PreferencesSeparatorText(stringResource(id = R.string.permissions))
                }

                item {
                    val context = LocalContext.current

                    val manageMediaLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { _ ->
                        val granted = MediaStore.canManageMedia(context)

                        onPermissionResult(granted)

                        setIsMediaManager(granted)
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
                PreferencesSwitchRow(
                    title = stringResource(id = R.string.permissions_confirm_to_delete),
                    summary = stringResource(id = R.string.permissions_confirm_too_delete_desc),
                    iconResID = R.drawable.confirm_action,
                    position = RowPosition.Single,
                    showBackground = false,
                    checked = confirmToDelete,
                    onSwitchClick = setConfirmToDelete
                )
            }

            item {
                PreferencesSwitchRow(
                    title = stringResource(id = R.string.permissions_overwrite_date_on_move),
                    summary = stringResource(id = R.string.permissions_overwrite_date_on_move_desc),
                    iconResID = R.drawable.clock,
                    position = RowPosition.Single,
                    showBackground = false,
                    checked = preserveDate,
                    onSwitchClick = setPreserveDate
                )
            }

            item {
                PreferencesSwitchRow(
                    title = stringResource(id = R.string.permissions_do_not_trash),
                    summary = stringResource(id = R.string.permissions_do_not_trash_desc),
                    iconResID = R.drawable.delete_forever,
                    position = RowPosition.Single,
                    showBackground = false,
                    checked = doNotTrash,
                    onSwitchClick = setDoNotTrash
                )
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
