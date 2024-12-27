package com.kaii.photos.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.LocalNavController
import com.kaii.photos.MainActivity.Companion.mainViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.ConfirmationDialogWithBody
import com.kaii.photos.compose.PreferencesRow
import com.kaii.photos.compose.PreferencesSeparatorText
import com.kaii.photos.compose.PreferencesSwitchRow
import com.kaii.photos.compose.RadioButtonRow
import com.kaii.photos.datastore.Storage
import com.kaii.photos.datastore.TrashBin
import com.kaii.photos.helpers.CustomMaterialTheme
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.brightenColor

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
                .background(CustomMaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            item {
                PreferencesSeparatorText(text = "Trash Bin")
            }

            item {
                val autoDeleteInterval by mainViewModel.settings.TrashBin.getAutoDeleteInterval().collectAsStateWithLifecycle(initialValue = 0)
                val showDeleteIntervalDialog = remember { mutableStateOf(false) }

				val summary by remember { derivedStateOf {
					if (autoDeleteInterval != 0) {
						"Auto delete items in trash bin after $autoDeleteInterval days"
					} else {
						"Items in trash bin won't be auto deleted"
					}
				}}

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

                val memoryOrStorage by remember { derivedStateOf {
                    if (cacheThumbnails) "storage" else "memory"
                }}
                val summary by remember { derivedStateOf {
                    if (thumbnailSize != 0) {
                        "Thumbnails are currently ${thumbnailSize}x${thumbnailSize} pixels. Higher values use more $memoryOrStorage"
                    } else {
                        "Thumbnail are shown at max possible resolution. This uses the most $memoryOrStorage"
                    }
                }}

                PreferencesSwitchRow(
                    title = "Cache Thumbnails",
                    iconResID = R.drawable.storage,
                    summary = "Allows for faster loading at the cost of storage usage",
                    position = RowPosition.Single,
                    showBackground = false,
                    checked = cacheThumbnails
                ) {isChecked ->
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
                    dialogBody = "This will erase all the thumbnail caches on this device, freeing up storage. Thumbnail loading will take longer on the next startup"
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

@Composable
fun RadioButtonListDialog(
    showDialog: MutableState<Boolean>,
    radioButtons: @Composable ColumnScope.() -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = {
            showDialog.value = false
        },
        properties = DialogProperties(
            dismissOnClickOutside = true,
            dismissOnBackPress = true
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(1f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(32.dp))
                .background(brightenColor(CustomMaterialTheme.colorScheme.surface, 0.1f))
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        	Spacer (modifier = Modifier.height(8.dp))

            Text(
                text = "Delete Interval",
                fontSize = TextUnit(18f, TextUnitType.Sp),
                color = CustomMaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .wrapContentSize()
            )

			Spacer (modifier = Modifier.height(8.dp))

            Text(
                text = "Photos in the trash bin older than this date will be permanently deleted",
                fontSize = TextUnit(14f, TextUnitType.Sp),
                color = CustomMaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier
                    .wrapContentSize()
                    .padding(12.dp, 0.dp)
            )

			Spacer (modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .wrapContentSize(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                radioButtons()
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .height(56.dp)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                FilledTonalButton(
                    onClick = {
                        onConfirm()
                        showDialog.value = false
                    }
                ) {
                    Text(text = "Confirm")
                }
            }
        }
    }
}

@Composable
fun DeleteIntervalDialog(
    showDialog: MutableState<Boolean>,
    initialValue: Int
) {
    var deleteInterval by remember { mutableIntStateOf(initialValue) }

    RadioButtonListDialog(
        showDialog = showDialog,
        radioButtons = {
            RadioButtonRow(
                text = "3 Days",
                checked = deleteInterval == 3
            ) {
                deleteInterval = 3
            }

            RadioButtonRow(
                text = "10 Days",
                checked = deleteInterval == 10
            ) {
                deleteInterval = 10
            }

            RadioButtonRow(
                text = "15 Days",
                checked = deleteInterval == 15
            ) {
                deleteInterval = 15
            }

            RadioButtonRow(
                text = "30 Days",
                checked = deleteInterval == 30
            ) {
                deleteInterval = 30
            }

            RadioButtonRow(
                text = "60 Days",
                checked = deleteInterval == 60
            ) {
                deleteInterval = 60
            }
        },
        onConfirm = {
            mainViewModel.settings.TrashBin.setAutoDeleteInterval(deleteInterval)
        }
    )
}

@Composable
fun ThumbnailSizeDialog(
    showDialog: MutableState<Boolean>,
    initialValue: Int
) {
    var thumbnailSize by remember { mutableIntStateOf(initialValue) }

    RadioButtonListDialog(
        showDialog = showDialog,
        radioButtons = {
            RadioButtonRow(
                text = "32x32 Pixels",
                checked = thumbnailSize == 32
            ) {
                thumbnailSize = 32
            }

            RadioButtonRow(
                text = "64x64 Pixel",
                checked = thumbnailSize == 64
            ) {
                thumbnailSize = 64
            }

            RadioButtonRow(
                text = "128x128 Pixels",
                checked = thumbnailSize == 128
            ) {
                thumbnailSize = 128
            }

            RadioButtonRow(
                text = "256x256 Pixels",
                checked = thumbnailSize == 256
            ) {
                thumbnailSize = 256
            }

            RadioButtonRow(
                text = "512x512 Pixels",
                checked = thumbnailSize == 512
            ) {
                thumbnailSize = 512
            }
        },
        onConfirm = {
            mainViewModel.settings.Storage.setThumbnailSize(thumbnailSize)
        }
    )
}
