package com.kaii.photos.compose.app_bars

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarExitDirection
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastDistinctBy
import androidx.compose.ui.util.fastMapNotNull
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.user_action.ConfirmationDialog
import com.kaii.photos.compose.grids.albums.MoveCopyAlbumListView
import com.kaii.photos.compose.widgets.SelectViewTopBarLeftButtons
import com.kaii.photos.compose.widgets.SelectViewTopBarRightButtons
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.toFileOperationMetadataItems
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.parent
import com.kaii.photos.mediastore.getAbsolutePathFromUri
import com.kaii.photos.permissions.files.rememberDirectoryPermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IsSelectingTopBar(
    selectionManager: SelectionManager,
    showTags: Boolean,
    showTagDialog: () -> Boolean,
    setShowTagDialog: (show: Boolean) -> Unit
) {
    TopAppBar(
        title = {
            SelectViewTopBarLeftButtons(selectionManager = selectionManager)
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
        actions = {
            if (showTags) {
                SelectViewTopBarRightButtons(
                    showTagDialog = showTagDialog,
                    setShowTagDialog = setShowTagDialog
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IsSelectingBottomAppBar(
    items: @Composable (RowScope.() -> Unit)
) {
    Box(
        modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth(1f)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(all = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        HorizontalFloatingToolbar(
            expanded = false,
            collapsedShadowElevation = 12.dp,
            expandedShadowElevation = 12.dp,
            scrollBehavior = FloatingToolbarDefaults.exitAlwaysScrollBehavior(
                exitDirection = FloatingToolbarExitDirection.Bottom
            )
        ) {
            items()
        }
    }
}

@Composable
fun SelectingBottomBarItems(
    albumInfo: AlbumType,
    selectionManager: SelectionManager,
    confirmToDelete: () -> Boolean,
    doNotTrash: () -> Boolean,
    runAction: (action: FileOperationAction) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val selectedItemsList by selectionManager.selection.collectAsStateWithLifecycle(initialValue = emptyList())

    IconButton(
        onClick = {
            coroutineScope.launch(Dispatchers.IO) {
                runAction(
                    FileOperationAction.Share(
                        files = selectedItemsList.toFileOperationMetadataItems()
                    )
                )
            }
        },
        enabled = selectedItemsList.isNotEmpty()
    ) {
        Icon(
            painter = painterResource(id = R.drawable.share),
            contentDescription = "share this image"
        )
    }

    val show = remember { mutableStateOf(false) }
    var isMoving by remember { mutableStateOf(false) }
    MoveCopyAlbumListView(
        show = show,
        clear = selectionManager::clear,
        isMoving = { isMoving },
        currentAlbum = { albumInfo },
        insetsPadding = WindowInsets.statusBars,
        onClick = { album ->
            runAction(
                if (isMoving) FileOperationAction.Move(
                    files = selectedItemsList.toFileOperationMetadataItems(),
                    origin = albumInfo,
                    destination = album
                ) else FileOperationAction.Copy(
                    files = selectedItemsList.toFileOperationMetadataItems(),
                    destination = album
                )
            )
        }
    )

    IconButton(
        onClick = {
            isMoving = true
            show.value = true
        },
        enabled = !selectedItemsList.isEmpty()
    ) {
        Icon(
            painter = painterResource(id = R.drawable.cut),
            contentDescription = "move this image"
        )
    }

    IconButton(
        onClick = {
            isMoving = false
            show.value = true
        },
        enabled = selectedItemsList.isNotEmpty()
    ) {
        Icon(
            painter = painterResource(id = R.drawable.copy),
            contentDescription = "copy this image"
        )
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    if (albumInfo is AlbumType.Folder) {
        if (showDeleteDialog) {
            ConfirmationDialog(
                title = stringResource(id = if (doNotTrash()) R.string.media_delete_permanently_confirm else R.string.media_trash_confirm),
                confirmButtonLabel = stringResource(id = R.string.media_delete),
                action = {
                    runAction(
                        if (doNotTrash()) {
                            FileOperationAction.Delete(
                                files = selectedItemsList.toFileOperationMetadataItems(),
                                album = albumInfo
                            )
                        } else {
                            FileOperationAction.Trash(
                                files = selectedItemsList.toFileOperationMetadataItems(),
                                isTrashed = true,
                                album = albumInfo
                            )
                        }
                    )

                    selectionManager.clear()
                },
                onDismiss = {
                    showDeleteDialog = false
                }
            )
        }
    } else {
        if (showDeleteDialog) {
            ConfirmationDialog(
                title = stringResource(id = R.string.custom_album_remove_media_desc),
                confirmButtonLabel = stringResource(id = R.string.custom_album_remove_media),
                action = {
                    runAction(
                        FileOperationAction.Trash(
                            files = selectedItemsList.toFileOperationMetadataItems(),
                            isTrashed = true,
                            album = albumInfo
                        )
                    )

                    selectionManager.clear()
                },
                onDismiss = {
                    showDeleteDialog = false
                }
            )
        }
    }

    IconButton(
        onClick = {
            if (confirmToDelete()) {
                showDeleteDialog = true
            } else {
                runAction(
                    if (doNotTrash()) {
                        FileOperationAction.Delete(
                            files = selectedItemsList.toFileOperationMetadataItems(),
                            album = albumInfo
                        )
                    } else {
                        FileOperationAction.Trash(
                            files = selectedItemsList.toFileOperationMetadataItems(),
                            isTrashed = true,
                            album = albumInfo
                        )
                    }
                )

                selectionManager.clear()
            }
        },
        enabled = selectedItemsList.isNotEmpty()
    ) {
        Icon(
            painter = painterResource(id = R.drawable.delete),
            contentDescription = "Delete this image"
        )
    }

    val dirPermissionManager = rememberDirectoryPermissionManager(
        onGranted = {
            runAction(
                FileOperationAction.Secure(
                    files = selectedItemsList.toFileOperationMetadataItems()
                )
            )

            selectionManager.clear()
        }
    )

    var showMoveToSecureFolderDialog by remember { mutableStateOf(false) }
    if (showMoveToSecureFolderDialog) {
        ConfirmationDialog(
            title = stringResource(id = R.string.media_secure_confirm),
            confirmButtonLabel = stringResource(id = R.string.media_secure),
            action = {
                if (selectedItemsList.isNotEmpty()) {
                    dirPermissionManager.start(
                        directories = selectedItemsList.fastMapNotNull {
                            context.contentResolver.getAbsolutePathFromUri(it.uri.toUri())?.parent()
                        }.fastDistinctBy {
                            it
                        }.toSet()
                    )
                }
            },
            onDismiss = {
                showMoveToSecureFolderDialog = false
            }
        )
    }

    IconButton(
        onClick = {
            showMoveToSecureFolderDialog = true
        },
        enabled = selectedItemsList.isNotEmpty() && albumInfo !is AlbumType.Cloud
    ) {
        Icon(
            painter = painterResource(id = R.drawable.secure_folder),
            contentDescription = "Secure this media"
        )
    }
}