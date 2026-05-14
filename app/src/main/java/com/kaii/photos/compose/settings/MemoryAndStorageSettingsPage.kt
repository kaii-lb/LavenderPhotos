package com.kaii.photos.compose.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.settings.ThumbnailSizeDialog
import com.kaii.photos.compose.dialogs.user_action.ConfirmationDialogWithBody
import com.kaii.photos.compose.widgets.PreferenceRowWithCustomBody
import com.kaii.photos.compose.widgets.PreferencesRow
import com.kaii.photos.compose.widgets.PreferencesSeparatorText
import com.kaii.photos.compose.widgets.PreferencesSwitchRow
import com.kaii.photos.di.appModule
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.TextStylingConstants
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt

@Preview
@Composable
private fun MemoryAndStorageSettingsPagePreview() {
    MemoryAndStorageSettingsPageImpl(
        thumbnailSize = { 256 },
        cacheThumbnails = { true },
        exportQuality = { 8 },
        navController = rememberNavController(),
        modifier = Modifier,
        setCacheThumbnails = {},
        setExportQuality = {},
        setThumbnailSize = {},
        clearThumbnailCache = {}
    )
}

@Composable
fun MemoryAndStorageSettingsPage(modifier: Modifier = Modifier) {
    val settings = LocalContext.current.appModule.settings.storage

    val thumbnailSize by settings.getThumbnailSize().collectAsStateWithLifecycle(initialValue = 0)
    val cacheThumbnails by settings.getCacheThumbnails().collectAsStateWithLifecycle(initialValue = true)
    val exportQuality by settings.getExportQuality().collectAsStateWithLifecycle(initialValue = 8)

    MemoryAndStorageSettingsPageImpl(
        thumbnailSize = { thumbnailSize },
        cacheThumbnails = { cacheThumbnails },
        exportQuality = { exportQuality },
        navController = LocalNavController.current,
        modifier = modifier,
        setCacheThumbnails = settings::setCacheThumbnails,
        setThumbnailSize = settings::setThumbnailSize,
        setExportQuality = settings::setExportQuality,
        clearThumbnailCache = settings::clearThumbnailCache
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MemoryAndStorageSettingsPageImpl(
    thumbnailSize: () -> Int,
    cacheThumbnails: () -> Boolean,
    exportQuality: () -> Int,
    navController: NavController,
    modifier: Modifier,
    setCacheThumbnails: (value: Boolean) -> Unit,
    setThumbnailSize: (value: Int) -> Unit,
    setExportQuality: (value: Int) -> Unit,
    clearThumbnailCache: () -> Unit
) {
    Scaffold(
        topBar = {
            MemoryAndStorageSettingsTopBar(
                navController = navController
            )
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
            item {
                PreferencesSeparatorText(text = stringResource(id = R.string.settings_storage))
            }

            item {
                val resources = LocalResources.current
                val showThumbnailSizeDialog = remember { mutableStateOf(false) }

                val memoryOrStorage by remember {
                    derivedStateOf {
                        if (cacheThumbnails()) resources.getString(R.string.settings_storage)
                            .lowercase() else resources.getString(R.string.settings_memory)
                            .lowercase()
                    }
                }
                val summary by remember {
                    derivedStateOf {
                        if (thumbnailSize() != 0) {
                            resources.getString(
                                R.string.settings_storage_thumbnails_size,
                                "${thumbnailSize()}x${thumbnailSize()}",
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
                    checked = cacheThumbnails(),
                    onSwitchClick = setCacheThumbnails
                )

                PreferencesSwitchRow(
                    title = stringResource(id = R.string.settings_storage_thumbnails_resolution),
                    iconResID = R.drawable.resolution,
                    summary = summary,
                    position = RowPosition.Single,
                    showBackground = false,
                    checked = thumbnailSize() != 0,
                    onSwitchClick = { isChecked ->
                        setThumbnailSize(
                            if (isChecked) 256 else 0
                        )
                    },
                    onRowClick = { _ ->
                        showThumbnailSizeDialog.value = true
                    }
                )

                if (showThumbnailSizeDialog.value) {
                    ThumbnailSizeDialog(
                        showDialog = showThumbnailSizeDialog,
                        initialValue = thumbnailSize(),
                        setThumbnailSize = setThumbnailSize
                    )
                }
            }

            item {
                var showConfirmationDialog by remember { mutableStateOf(false) }

                PreferencesRow(
                    title = stringResource(id = R.string.settings_storage_thumbnails_clear_cache),
                    iconResID = R.drawable.close,
                    position = RowPosition.Single,
                    showBackground = false,
                    summary = stringResource(id = R.string.settings_storage_thumbnails_clear_cache_desc)
                ) {
                    showConfirmationDialog = true
                }

                if (showConfirmationDialog) {
                    ConfirmationDialogWithBody(
                        confirmButtonLabel = stringResource(id = R.string.settings_clear),
                        title = stringResource(id = R.string.settings_storage_thumbnails_clear_cache) + "?",
                        body = stringResource(id = R.string.settings_clear_cache_desc),
                        action = clearThumbnailCache,
                        onDismiss = {
                            showConfirmationDialog = false
                        }
                    )
                }
            }

            item {
                PreferencesSeparatorText(
                    text = stringResource(id = R.string.editing)
                )
            }

            item {
                var currentQuality by remember { mutableIntStateOf(exportQuality()) }

                val interactionSource = remember { MutableInteractionSource() }
                LaunchedEffect(interactionSource) {
                    delay(100)
                    currentQuality = exportQuality()
                    interactionSource.interactions.collectLatest {
                        if (it is PressInteraction.Release) {
                            currentQuality = exportQuality()
                        }
                    }
                }

                PreferenceRowWithCustomBody(
                    icon = R.drawable.hd,
                    title = stringResource(id = R.string.settings_storage_export_quality, currentQuality * 10)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.Start
                    ) {
                        AnimatedVisibility(
                            visible = currentQuality >= 8,
                            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
                        ) {
                            Text(
                                text = stringResource(id = R.string.settings_storage_export_quality_warn),
                                style = MaterialTheme.typography.bodySmallEmphasized,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .padding(top = 4.dp)
                            )
                        }

                        Slider(
                            value = currentQuality.toFloat(),
                            onValueChange = {
                                currentQuality = it.roundToInt()
                                setExportQuality(it.roundToInt())
                            },
                            steps = 6,
                            valueRange = 2f..10f,
                            thumb = {
                                Box(
                                    modifier = Modifier
                                        .height(16.dp)
                                        .width(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            },
                            interactionSource = interactionSource,
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemoryAndStorageSettingsTopBar(
    navController: NavController
) {
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
