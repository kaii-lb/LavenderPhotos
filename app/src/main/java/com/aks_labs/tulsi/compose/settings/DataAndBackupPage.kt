package com.aks_labs.tulsi.compose.settings

import android.content.Intent
import android.util.Log
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
import androidx.core.content.FileProvider
import com.kaii.lavender_snackbars.LavenderSnackbarController
import com.kaii.lavender_snackbars.LavenderSnackbarEvents
import com.aks_labs.tulsi.LocalNavController
import com.aks_labs.tulsi.MainActivity.Companion.mainViewModel
import com.aks_labs.tulsi.R
import com.aks_labs.tulsi.compose.PreferencesRow
import com.aks_labs.tulsi.compose.PreferencesSeparatorText
import com.aks_labs.tulsi.datastore.AlbumInfo
import com.aks_labs.tulsi.datastore.AlbumsList
import com.aks_labs.tulsi.helpers.DataAndBackupHelper
import com.aks_labs.tulsi.helpers.RowPosition
import com.aks_labs.tulsi.helpers.relativePath
import com.aks_labs.tulsi.mediastore.LAVENDER_FILE_PROVIDER_AUTHORITY
import kotlinx.coroutines.Dispatchers

private const val TAG = "DATA_AND_BACKUP_PAGE"

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
                    iconResID = R.drawable.key_remove,
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

                        val albumFile = backupHelper.getUnencryptedExportDir(context = context)
                        mainViewModel.settings.AlbumsList.addToAlbumsList(
                            AlbumInfo(
                                name = albumFile.name,
                                paths = listOf(albumFile.relativePath),
                                id = albumFile.hashCode()
                            )
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

                        val albumFile = backupHelper.getUnencryptedExportDir(context = context)
                        mainViewModel.settings.AlbumsList.addToAlbumsList(
                            AlbumInfo(
                                name = albumFile.name,
                                paths = listOf(albumFile.relativePath),
                                id = albumFile.hashCode()
                            )
                        )

                        isLoading.value = false
                    }
                }

                PreferencesRow(
                    title = "Export Backup To Zip",
                    iconResID = R.drawable.folder_zip,
                    summary = "Exports a zip file of all your secured items",
                    position = RowPosition.Middle,
                    showBackground = false
                ) {
                    mainViewModel.launch(Dispatchers.IO) {
                        val backupHelper = DataAndBackupHelper()

                        isLoading.value = true
                        LavenderSnackbarController.pushEvent(
                            LavenderSnackbarEvents.LoadingEvent(
                                message = "Compressing backup...",
                                iconResId = R.drawable.folder_export,
                                isLoading = isLoading
                            )
                        )

                        backupHelper.exportSecureFolderToZipFile(context = context)

                        val intent = Intent().apply {
                            action = Intent.ACTION_VIEW
                            data = FileProvider.getUriForFile(
                                context,
                                LAVENDER_FILE_PROVIDER_AUTHORITY,
                                backupHelper.getZipFile(context = context)
                            )

                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        }

                        context.startActivity(intent)

                        isLoading.value = false
                    }
                }
            }

            item {
                PreferencesSeparatorText(
                    text = "Favourites"
                )
            }

            item {
                val context = LocalContext.current
                val isLoading = remember { mutableStateOf(false) }

                PreferencesRow(
                    title = "Export Backup To Folder",
                    iconResID = R.drawable.folder_favourites,
                    summary = "Exports of all your favourited items to a folder",
                    position = RowPosition.Middle,
                    showBackground = false
                ) {
                    mainViewModel.launch(Dispatchers.IO) {
                        val helper = DataAndBackupHelper()

                        isLoading.value = true
                        LavenderSnackbarController.pushEvent(
                            LavenderSnackbarEvents.LoadingEvent(
                                message = "Exporting backup...",
                                iconResId = R.drawable.folder_export,
                                isLoading = isLoading
                            )
                        )

                        helper.exportFavourites(context = context)

                        val favExportDir = helper.getFavExportDir(context = context)
                        mainViewModel.settings.AlbumsList.addToAlbumsList(
                            AlbumInfo(
                                name = favExportDir.name,
                                paths = listOf(favExportDir.relativePath),
                                id = favExportDir.hashCode()
                            )
                        )

                        isLoading.value = false
                    }
                }

                PreferencesRow(
                    title = "Export Backup To Zip",
                    iconResID = R.drawable.folder_zip,
                    summary = "Exports a zip file of all your favourited items",
                    position = RowPosition.Middle,
                    showBackground = false
                ) {
                    mainViewModel.launch(Dispatchers.IO) {
                        val helper = DataAndBackupHelper()

                        isLoading.value = true
                        LavenderSnackbarController.pushEvent(
                            LavenderSnackbarEvents.LoadingEvent(
                                message = "Compressing backup...",
                                iconResId = R.drawable.folder_export,
                                isLoading = isLoading
                            )
                        )

                        helper.exportFavouritesToZipFile(context = context) { progress ->
                            Log.d(TAG, "Progress ${progress * 100}")
                        }

                        val intent = Intent().apply {
                            action = Intent.ACTION_VIEW
                            data = FileProvider.getUriForFile(
                                context,
                                LAVENDER_FILE_PROVIDER_AUTHORITY,
                                helper.getFavZipFile(context = context)
                            )

                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        }

                        context.startActivity(intent)
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




