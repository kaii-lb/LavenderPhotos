package com.kaii.photos.compose.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.LocalAppDatabase
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.widgets.PreferencesRow
import com.kaii.photos.compose.widgets.PreferencesSeparatorText
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.AlbumsList
import com.kaii.photos.helpers.DataAndBackupHelper
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.helpers.relativePath
import com.kaii.photos.mediastore.LAVENDER_FILE_PROVIDER_AUTHORITY
import kotlinx.coroutines.Dispatchers
import java.io.File

private const val TAG = "com.kaii.photos.compose.settings.DataAndBackupPage"

@Composable
fun DataAndBackupPage() {
    val mainViewModel = LocalMainViewModel.current
    val appDatabase = LocalAppDatabase.current

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
                    text = stringResource(id = R.string.immich)
                )
            }

            item {
                val navController = LocalNavController.current

                PreferencesRow(
                    title = stringResource(id = R.string.immich_title),
                    iconResID = R.drawable.cloud_upload,
                    summary = stringResource(id = R.string.immich_desc),
                    position = RowPosition.Middle,
                    showBackground = false,
                    goesToOtherPage = true
                ) {
                    navController.navigate(MultiScreenViewType.ImmichMainPage.name)
                }
            }

            item {
                PreferencesSeparatorText(
                    text = stringResource(id = R.string.secure_folder)
                )
            }

            item {
                val context = LocalContext.current
                val isLoading = remember { mutableStateOf(false) }
                val exportingBackup = stringResource(id = R.string.exporting_backup)

                PreferencesRow(
                    title = stringResource(id = R.string.export_unenc_backup),
                    iconResID = R.drawable.key_remove,
                    summary = stringResource(id = R.string.export_unenc_backup_desc),
                    position = RowPosition.Middle,
                    showBackground = false
                ) {
                    mainViewModel.launch(Dispatchers.IO) {
                        val backupHelper = DataAndBackupHelper(appDatabase)

                        isLoading.value = true
                        LavenderSnackbarController.pushEvent(
                            LavenderSnackbarEvents.LoadingEvent(
                                message = exportingBackup,
                                icon = R.drawable.folder_export,
                                isLoading = isLoading
                            )
                        )

                        backupHelper.exportUnencryptedSecureFolderItems(context = context)

                        val albumFile = backupHelper.getUnencryptedExportDir(context = context)
                        mainViewModel.settings.AlbumsList.add(
                            listOf(
                                AlbumInfo(
                                    name = albumFile.name,
                                    paths = listOf(albumFile.relativePath),
                                    id = albumFile.hashCode()
                                )
                            )
                        )

                        isLoading.value = false
                    }
                }

                PreferencesRow(
                    title = stringResource(id = R.string.export_raw_backup),
                    iconResID = R.drawable.folder_export,
                    summary = stringResource(id = R.string.export_raw_backup_desc),
                    position = RowPosition.Middle,
                    showBackground = false
                ) {
                    mainViewModel.launch(Dispatchers.IO) {
                        val backupHelper = DataAndBackupHelper(appDatabase)

                        isLoading.value = true
                        LavenderSnackbarController.pushEvent(
                            LavenderSnackbarEvents.LoadingEvent(
                                message = exportingBackup,
                                icon = R.drawable.folder_export,
                                isLoading = isLoading
                            )
                        )

                        backupHelper.exportRawSecureFolderItems(context = context)

                        val albumFile = backupHelper.getUnencryptedExportDir(context = context)
                        mainViewModel.settings.AlbumsList.add(
                            listOf(
                                AlbumInfo(
                                    name = albumFile.name,
                                    paths = listOf(albumFile.relativePath),
                                    id = albumFile.hashCode()
                                )
                            )
                        )

                        isLoading.value = false
                    }
                }

                PreferencesRow(
                    title = stringResource(id = R.string.export_unenc_zip),
                    iconResID = R.drawable.folder_zip,
                    summary = stringResource(id = R.string.export_unenc_zip_desc),
                    position = RowPosition.Middle,
                    showBackground = false
                ) {
                    mainViewModel.launch(Dispatchers.IO) {
                        val backupHelper = DataAndBackupHelper(appDatabase)

                        isLoading.value = true
                        LavenderSnackbarController.pushEvent(
                            LavenderSnackbarEvents.LoadingEvent(
                                message = exportingBackup,
                                icon = R.drawable.folder_export,
                                isLoading = isLoading
                            )
                        )

                        val filePath = backupHelper.exportSecureFolderToZipFile(context = context)

                        if (filePath != null) {
                            val intent = Intent().apply {
                                action = Intent.ACTION_VIEW
                                data = FileProvider.getUriForFile(
                                    context,
                                    LAVENDER_FILE_PROVIDER_AUTHORITY,
                                    File(filePath)
                                )

                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                            }

                            context.startActivity(intent)
                        }

                        isLoading.value = false
                    }
                }
            }

            item {
                PreferencesSeparatorText(
                    text = stringResource(id = R.string.favourites)
                )
            }

            item {
                val context = LocalContext.current
                val isLoading = remember { mutableStateOf(false) }
                val exportingBackup = stringResource(id = R.string.exporting_backup)

                PreferencesRow(
                    title = stringResource(id = R.string.export_fav_to_folder),
                    iconResID = R.drawable.folder_favourites,
                    summary = stringResource(id = R.string.export_fav_to_folder_desc),
                    position = RowPosition.Middle,
                    showBackground = false
                ) {
                    mainViewModel.launch(Dispatchers.IO) {
                        val helper = DataAndBackupHelper(appDatabase)

                        isLoading.value = true
                        LavenderSnackbarController.pushEvent(
                            LavenderSnackbarEvents.LoadingEvent(
                                message = exportingBackup,
                                icon = R.drawable.folder_export,
                                isLoading = isLoading
                            )
                        )

                        helper.exportFavourites(context = context)

                        val favExportDir = helper.getFavExportDir(context = context)
                        mainViewModel.settings.AlbumsList.add(
                            listOf(
                                AlbumInfo(
                                    name = favExportDir.name,
                                    paths = listOf(favExportDir.relativePath),
                                    id = favExportDir.hashCode()
                                )
                            )
                        )

                        isLoading.value = false
                    }
                }

                PreferencesRow(
                    title = stringResource(id = R.string.export_unenc_zip),
                    iconResID = R.drawable.folder_zip,
                    summary = stringResource(id = R.string.export_fav_to_zip_desc),
                    position = RowPosition.Middle,
                    showBackground = false
                ) {
                    mainViewModel.launch(Dispatchers.IO) {
                        val helper = DataAndBackupHelper(appDatabase)

                        isLoading.value = true
                        LavenderSnackbarController.pushEvent(
                            LavenderSnackbarEvents.LoadingEvent(
                                message = exportingBackup,
                                icon = R.drawable.folder_export,
                                isLoading = isLoading
                            )
                        )

                        val filePath = helper.exportFavouritesToZipFile(context = context) { progress ->
                            Log.d(TAG, "Progress ${progress * 100}")
                        }
                        if (filePath != null) {
                            val intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                data = FileProvider.getUriForFile(
                                    context,
                                    LAVENDER_FILE_PROVIDER_AUTHORITY,
                                    File(filePath)
                                )

                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                            }

                            context.startActivity(intent)
                        }

                        isLoading.value = false
                    }
                }
            }

            item {
                PreferencesSeparatorText(
                    text = stringResource(id = R.string.help)
                )
            }

            item {
                val context = LocalContext.current
                val resources = LocalResources.current

                PreferencesRow(
                    title = stringResource(id = R.string.export_location),
                    summary = stringResource(id = R.string.export_location_desc),
                    iconResID = R.drawable.folder_open,
                    position = RowPosition.Single,
                    showBackground = false
                ) {
                    val clipboardManager =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

                    val clipData = ClipData.newPlainText(
                        resources.getString(R.string.export_location),
                        resources.getString(R.string.export_location_desc)
                    )

                    clipboardManager.setPrimaryClip(clipData)
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
                text = stringResource(id = R.string.data_and_backup),
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
