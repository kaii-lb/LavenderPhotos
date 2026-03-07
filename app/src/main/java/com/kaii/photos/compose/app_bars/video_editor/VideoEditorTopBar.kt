package com.kaii.photos.compose.app_bars.video_editor

import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.media3.common.util.UnstableApi
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.user_action.ConfirmationDialog
import com.kaii.photos.compose.widgets.SelectableDropDownMenuItem
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.CustomItem
import com.kaii.photos.di.appModule
import com.kaii.photos.helpers.PhotoGridConstants
import com.kaii.photos.helpers.editing.BasicVideoData
import com.kaii.photos.helpers.editing.DrawingPaintState
import com.kaii.photos.helpers.editing.VideoEditingState
import com.kaii.photos.helpers.editing.VideoModification
import com.kaii.photos.helpers.editing.saveVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VideoEditorTopBar(
    uri: Uri,
    absolutePath: String,
    modifications: SnapshotStateList<VideoModification>,
    basicVideoData: BasicVideoData,
    videoEditingState: VideoEditingState,
    drawingPaintState: DrawingPaintState,
    lastSavedModCount: MutableIntState,
    containerDimens: Size,
    canvasSize: Size,
    isFromOpenWithView: Boolean,
    customAlbumId: String?,
    exitOnSave: () -> Boolean,
    overwriteByDefault: () -> Boolean
) {
    val navController = LocalNavController.current
    var navMediaId by remember { mutableLongStateOf(-1L) }

    TopAppBar(
        title = {},
        navigationIcon = {
            Box(
                modifier = Modifier
                    .padding(8.dp, 0.dp, 0.dp, 0.dp)
            ) {
                val showDialog = remember { mutableStateOf(false) }

                if (showDialog.value) {
                    ConfirmationDialog(
                        showDialog = showDialog,
                        dialogTitle = stringResource(id = R.string.editing_discard_desc),
                        confirmButtonLabel = stringResource(id = R.string.editing_discard)
                    ) {
                        navController.popBackStack()
                    }
                }

                val coroutineScope = rememberCoroutineScope()
                FilledTonalIconButton(
                    onClick = {
                        if (lastSavedModCount.intValue < modifications.size) {
                            showDialog.value = true
                        } else if (!isFromOpenWithView && navMediaId != -1L) coroutineScope.launch(Dispatchers.Main) {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("editIndex", 0)

                            navController.popBackStack()
                        } else {
                            navController.popBackStack()
                        }
                    },
                    enabled = true,
                    modifier = Modifier
                        .height(40.dp)
                        .width(56.dp)
                        .align(Alignment.Center)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.close),
                        contentDescription = stringResource(id = R.string.editing_close_desc),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .size(24.dp)
                    )
                }
            }
        },
        actions = {
            var showDropDown by remember { mutableStateOf(false) }
            var overwrite by remember(overwriteByDefault()) { mutableStateOf(overwriteByDefault()) }

            DropdownMenu(
                expanded = showDropDown,
                onDismissRequest = {
                    showDropDown = false
                },
                shape = RoundedCornerShape(24.dp),
                properties = PopupProperties(
                    dismissOnClickOutside = true,
                    dismissOnBackPress = true
                ),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 8.dp
            ) {
                SelectableDropDownMenuItem(
                    text = stringResource(id = R.string.editing_overwrite_desc),
                    iconResId = R.drawable.checkmark_thin,
                    isSelected = overwrite
                ) {
                    overwrite = true
                    showDropDown = false
                }

                SelectableDropDownMenuItem(
                    text = stringResource(id = R.string.editing_save),
                    iconResId = R.drawable.checkmark_thin,
                    isSelected = !overwrite
                ) {
                    overwrite = false
                    showDropDown = false
                }
            }

            SplitButtonLayout(
                leadingButton = {
                    val context = LocalContext.current
                    val resources = LocalResources.current
                    val coroutineScope = rememberCoroutineScope()
                    val textMeasurer = rememberTextMeasurer()

                    SplitButtonDefaults.LeadingButton(
                        onClick = {
                            lastSavedModCount.intValue = modifications.size

                            // mainViewModel so it doesn't die if user exits before video is saved
                            // not using Dispatchers.IO since transformer needs to be on main thread
                            context.appModule.scope.launch {
                                navMediaId = saveVideo(
                                    context = context,
                                    modifications = modifications + drawingPaintState.modifications.map {
                                        it as VideoModification
                                    },
                                    videoEditingState = videoEditingState,
                                    basicVideoData = basicVideoData,
                                    uri = uri,
                                    absolutePath = absolutePath,
                                    overwrite = overwrite,
                                    containerDimens = containerDimens,
                                    canvasSize = canvasSize,
                                    textMeasurer = textMeasurer,
                                    isFromOpenWithView = isFromOpenWithView
                                ) {
                                    coroutineScope.launch {
                                        LavenderSnackbarController.pushEvent(
                                            event = LavenderSnackbarEvents.MessageEvent(
                                                message = resources.getString(R.string.editing_export_video_failed),
                                                icon = R.drawable.error_2,
                                                duration = SnackbarDuration.Short
                                            )
                                        )
                                    }
                                }

                                delay(PhotoGridConstants.UPDATE_TIME * 2)
                                if (customAlbumId != null && navMediaId != -1L) coroutineScope.launch(Dispatchers.IO) {
                                    MediaDatabase.getInstance(context)
                                        .customDao()
                                        .upsertAll(
                                            listOf(
                                                CustomItem(
                                                    id = navMediaId,
                                                    album = customAlbumId
                                                )
                                            )
                                        )
                                }

                                if (exitOnSave() && navMediaId != -1L && !isFromOpenWithView) coroutineScope.launch(Dispatchers.Main) {
                                    navController.previousBackStackEntry
                                        ?.savedStateHandle
                                        ?.set("editIndex", 0)

                                    navController.popBackStack()
                                }
                            }
                        }
                    ) {
                        Text(
                            text =
                                if (overwrite) stringResource(id = R.string.editing_overwrite)
                                else stringResource(id = R.string.editing_save)
                        )
                    }
                },
                trailingButton = {
                    // TODO: remove when material expressive is not broken like this
                    // HACKY workaround for a random trigger by onCheckedChange of TrailingButton
                    var openedTimes by remember { mutableIntStateOf(0) }

                    SplitButtonDefaults.TrailingButton(
                        checked = showDropDown,
                        onCheckedChange = {
                            openedTimes += 1
                            if (openedTimes % 2 != 0) {
                                showDropDown = !showDropDown
                            }
                        },
                        enabled = !isFromOpenWithView
                    ) {
                        val rotation: Float by animateFloatAsState(
                            targetValue = if (showDropDown) 180f else 0f
                        )

                        Icon(
                            painter = painterResource(id = R.drawable.drop_down_arrow),
                            modifier = Modifier
                                .size(SplitButtonDefaults.TrailingIconSize)
                                .graphicsLayer {
                                    rotationZ = rotation
                                },
                            contentDescription = "Dropdown icon"
                        )
                    }
                }
            )
        }
    )
}