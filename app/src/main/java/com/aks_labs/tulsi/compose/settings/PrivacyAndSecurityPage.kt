package com.aks_labs.tulsi.compose.settings

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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aks_labs.tulsi.LocalNavController
import com.aks_labs.tulsi.MainActivity.Companion.mainViewModel
import com.aks_labs.tulsi.R
import com.aks_labs.tulsi.compose.PreferencesSeparatorText
import com.aks_labs.tulsi.compose.PreferencesSwitchRow
import com.aks_labs.tulsi.datastore.Permissions
import com.aks_labs.tulsi.helpers.RowPosition

@Composable
fun PrivacyAndSecurityPage() {
    val context = LocalContext.current

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
            }

            item {
                PreferencesSeparatorText(
                    text = "Media Management"
                )
            }

            item {
                val confirmToDelete by mainViewModel.settings.Permissions.getConfirmToDelete().collectAsStateWithLifecycle(initialValue = true)

                PreferencesSwitchRow(
                    title = "Confirm to Delete",
                    summary = "Ask for confirmation before deleting any media",
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
                    title = "Overwrite Date on Move",
                    summary = "When enabled, copied/moved media shows at the top of the list",
                    iconResID = R.drawable.clock,
                    position = RowPosition.Single,
                    showBackground = false,
                    checked = overwriteOnMove
                ) {
                    mainViewModel.settings.Permissions.setOverwriteDateOnMove(it)
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
                text = "Privacy & Security",
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


