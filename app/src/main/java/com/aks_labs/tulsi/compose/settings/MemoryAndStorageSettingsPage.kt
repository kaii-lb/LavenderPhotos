package com.aks_labs.tulsi.compose.settings

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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aks_labs.tulsi.LocalNavController
import com.aks_labs.tulsi.MainActivity.Companion.mainViewModel
import com.aks_labs.tulsi.R
import com.aks_labs.tulsi.compose.dialogs.ConfirmationDialogWithBody
import com.aks_labs.tulsi.compose.PreferencesRow
import com.aks_labs.tulsi.compose.PreferencesSeparatorText
import com.aks_labs.tulsi.compose.PreferencesSwitchRow
import com.aks_labs.tulsi.compose.dialogs.ThumbnailSizeDialog
import com.aks_labs.tulsi.compose.dialogs.DeleteIntervalDialog
import com.aks_labs.tulsi.datastore.Storage
import com.aks_labs.tulsi.datastore.TrashBin
import com.aks_labs.tulsi.helpers.RowPosition

@Composable
fun MemoryAndStorageSettingsPage() {
    Scaffold(
        topBar = {
            MemoryAndStorageSettingsTopBar()
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
                PreferencesSeparatorText(text = "Trash Bin")
            }

            item {
                val autoDeleteInterval by mainViewModel.settings.TrashBin.getAutoDeleteInterval().collectAsStateWithLifecycle(initialValue = 0)
                val showDeleteIntervalDialog = remember { mutableStateOf(false) }

                val summary by remember {
                    derivedStateOf {
                        if (autoDeleteInterval != 0) {
                            "Auto delete items in trash bin after $autoDeleteInterval days"
                        } else {
                            "Items in trash bin won't be auto deleted"
                        }
                    }
                }

                PreferencesSwitchRow(
                    title = "Auto delete interval",
                    iconResID = R.drawable.auto_delete,
                    summary = summary,
                    position = RowPosition.Single,
                    showBackground = false,
                    checked = autoDeleteInterval != 0,
                    onSwitchClick = { isChecked ->
                        mainViewModel.settings.TrashBin.setAutoDeleteInterval(
                            if (isChecked) 30 else 0
                        )
                    },
                    onRowClick = { _ ->
                        showDeleteIntervalDialog.value = true
                    }
                )

                if (showDeleteIntervalDialog.value) {
                    DeleteIntervalDialog(
                        showDialog = showDeleteIntervalDialog,
                        initialValue = autoDeleteInterval
                    )
                }
            }

            item {
                PreferencesSeparatorText(text = "Storage")
            }

            item {
                val showThumbnailSizeDialog = remember { mutableStateOf(false) }
                val thumbnailSize by mainViewModel.settings.Storage.getThumbnailSize().collectAsStateWithLifecycle(initialValue = 0)
                val cacheThumbnails by mainViewModel.settings.Storage.getCacheThumbnails().collectAsStateWithLifecycle(initialValue = true)

                val memoryOrStorage by remember {
                    derivedStateOf {
                        if (cacheThumbnails) "storage" else "memory"
                    }
                }
                val summary by remember {
                    derivedStateOf {
                        if (thumbnailSize != 0) {
                            "Thumbnails are currently ${thumbnailSize}x${thumbnailSize} pixels. Higher values use more $memoryOrStorage"
                        } else {
                            "Thumbnail are shown at max possible resolution. This uses the most $memoryOrStorage"
                        }
                    }
                }

                PreferencesSwitchRow(
                    title = "Cache Thumbnails",
                    iconResID = R.drawable.storage,
                    summary = "Allows for faster loading at the cost of storage usage",
                    position = RowPosition.Single,
                    showBackground = false,
                    checked = cacheThumbnails
                ) { isChecked ->
                    mainViewModel.settings.Storage.setCacheThumbnails(isChecked)
                }

                PreferencesSwitchRow(
                    title = "Thumbnail Resolution",
                    iconResID = R.drawable.resolution,
                    summary = summary,
                    position = RowPosition.Single,
                    showBackground = false,
                    checked = thumbnailSize != 0,
                    onSwitchClick = { isChecked ->
                        mainViewModel.settings.Storage.setThumbnailSize(
                            if (isChecked) 256 else 0
                        )
                    },
                    onRowClick = { _ ->
                        showThumbnailSizeDialog.value = true
                    },
                )

                if (showThumbnailSizeDialog.value) {
                    ThumbnailSizeDialog(
                        showDialog = showThumbnailSizeDialog,
                        initialValue = thumbnailSize
                    )
                }
            }

            item {
                val showConfirmationDialog = remember { mutableStateOf(false) }

                PreferencesRow(
                    title = "Clear thumbnail cache",
                    iconResID = R.drawable.close,
                    position = RowPosition.Single,
                    showBackground = false,
                    summary = "Erases thumbnail caches to free up storage"
                ) {
                    showConfirmationDialog.value = true
                }

                ConfirmationDialogWithBody(
                    showDialog = showConfirmationDialog,
                    confirmButtonLabel = "Clear",
                    dialogTitle = "Clear Thumbnail Caches?",
                    dialogBody = "This will erase all the thumbnail caches on this device, freeing up storage. Thumbnail loading will take longer on the next startup."
                ) {
                    mainViewModel.settings.Storage.clearThumbnailCache()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemoryAndStorageSettingsTopBar() {
    val navController = LocalNavController.current

    TopAppBar(
        title = {
            Text(
                text = "Memory & Storage",
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


