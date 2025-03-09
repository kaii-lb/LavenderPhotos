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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.kaii.lavender_snackbars.LavenderSnackbarController
import com.kaii.lavender_snackbars.LavenderSnackbarEvents
import com.kaii.photos.LocalNavController
import com.kaii.photos.MainActivity.Companion.mainViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.PreferencesRow
import com.kaii.photos.compose.PreferencesSeparatorText
import com.kaii.photos.datastore.AlbumsList
import com.kaii.photos.helpers.DataAndBackupHelper
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.baseInternalStorageDirectory
import kotlinx.coroutines.Dispatchers

@Composable
fun DataAndBackupPage() {
    Scaffold(
        topBar = {
            DataAndBackupTopBar()
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
                PreferencesSeparatorText(
                    text = "Secure Folder"
                )
            }

            item {
                val context = LocalContext.current
                val isLoading = remember { mutableStateOf(false) }

                PreferencesRow(
                    title = "Export Unencrypted Backup",
                    iconResID = R.drawable.folder_export,
                    summary = "Exports a folder of all your secured items, unencrypted",
                    position = RowPosition.Middle,
                    showBackground = false
                ) {
                    mainViewModel.launch(Dispatchers.IO) {
                        val backupHelper = DataAndBackupHelper()

                        isLoading.value = true
                        LavenderSnackbarController.pushEvent(
                            LavenderSnackbarEvents.LoadingEvent(
                                message = "Exporting backup...",
                                iconResId = R.drawable.folder_export,
                                isLoading = isLoading
                            )
                        )

                        backupHelper.exportUnencryptedSecureFolderItems(context = context)

                        mainViewModel.settings.AlbumsList.addToAlbumsList(
                            backupHelper.getUnencryptedExportDir(context = context)
                                .absolutePath
                                .replace(baseInternalStorageDirectory, "")
                        )

                        isLoading.value = false
                    }
                }

                PreferencesRow(
                    title = "Export Raw Backup",
                    iconResID = R.drawable.folder_export,
                    summary = "Exports a folder of all your secured items, raw (encrypted or not)",
                    position = RowPosition.Middle,
                    showBackground = false
                ) {
                	mainViewModel.launch(Dispatchers.IO) {
                		val backupHelper = DataAndBackupHelper()

                        isLoading.value = true
                        LavenderSnackbarController.pushEvent(
                            LavenderSnackbarEvents.LoadingEvent(
                                message = "Exporting backup...",
                                iconResId = R.drawable.folder_export,
                                isLoading = isLoading
                            )
                        )

                		backupHelper.exportRawSecureFolderItems(context = context)

                        mainViewModel.settings.AlbumsList.addToAlbumsList(
                            backupHelper.getRawExportDir(context = context)
                                .absolutePath
                                .replace(baseInternalStorageDirectory, "")
                        )

                        isLoading.value = false
                	}
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DataAndBackupTopBar() {
    val navController = LocalNavController.current

    TopAppBar(
        title = {
            Text(
                text = "Data & Backup",
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
