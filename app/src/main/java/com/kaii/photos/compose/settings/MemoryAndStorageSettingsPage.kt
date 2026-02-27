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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.ConfirmationDialogWithBody
import com.kaii.photos.compose.dialogs.ThumbnailSizeDialog
import com.kaii.photos.compose.widgets.PreferencesRow
import com.kaii.photos.compose.widgets.PreferencesSeparatorText
import com.kaii.photos.compose.widgets.PreferencesSwitchRow
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.TextStylingConstants

@Composable
fun MemoryAndStorageSettingsPage() {
    val mainViewModel = LocalMainViewModel.current

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
                PreferencesSeparatorText(text = stringResource(id = R.string.settings_storage))
            }

            item {
                val resources = LocalResources.current
                val showThumbnailSizeDialog = remember { mutableStateOf(false) }
                val thumbnailSize by mainViewModel.settings.storage.getThumbnailSize()
                    .collectAsStateWithLifecycle(initialValue = 0)
                val cacheThumbnails by mainViewModel.settings.storage.getCacheThumbnails()
                    .collectAsStateWithLifecycle(initialValue = true)

                val memoryOrStorage by remember {
                    derivedStateOf {
                        if (cacheThumbnails) resources.getString(R.string.settings_storage)
                            .lowercase() else resources.getString(R.string.settings_memory)
                            .lowercase()
                    }
                }
                val summary by remember {
                    derivedStateOf {
                        if (thumbnailSize != 0) {
                            resources.getString(
                                R.string.settings_storage_thumbnails_size,
                                "${thumbnailSize}x${thumbnailSize}",
                                memoryOrStorage
                            )
                        } else {
                            resources.getString(
                                R.string.settings_storage_thumbnails_max,
                                memoryOrStorage
                            )
                        }
                    }
                }

                PreferencesSwitchRow(
                    title = stringResource(id = R.string.settings_storage_thumbnails_cache),
                    iconResID = R.drawable.storage,
                    summary = stringResource(id = R.string.settings_storage_thumbnails_cache_desc),
                    position = RowPosition.Single,
                    showBackground = false,
                    checked = cacheThumbnails
                ) { isChecked ->
                    mainViewModel.settings.storage.setCacheThumbnails(isChecked)
                }

                PreferencesSwitchRow(
                    title = stringResource(id = R.string.settings_storage_thumbnails_resolution),
                    iconResID = R.drawable.resolution,
                    summary = summary,
                    position = RowPosition.Single,
                    showBackground = false,
                    checked = thumbnailSize != 0,
                    onSwitchClick = { isChecked ->
                        mainViewModel.settings.storage.setThumbnailSize(
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
                    title = stringResource(id = R.string.settings_storage_thumbnails_clear_cache),
                    iconResID = R.drawable.close,
                    position = RowPosition.Single,
                    showBackground = false,
                    summary = stringResource(id = R.string.settings_storage_thumbnails_clear_cache_desc)
                ) {
                    showConfirmationDialog.value = true
                }

                ConfirmationDialogWithBody(
                    showDialog = showConfirmationDialog,
                    confirmButtonLabel = stringResource(id = R.string.settings_clear),
                    dialogTitle = stringResource(id = R.string.settings_storage_thumbnails_clear_cache) + "?",
                    dialogBody = stringResource(id = R.string.settings_clear_cache_desc)
                ) {
                    mainViewModel.settings.storage.clearThumbnailCache()
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
                text = stringResource(id = R.string.settings_memory_storage),
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
