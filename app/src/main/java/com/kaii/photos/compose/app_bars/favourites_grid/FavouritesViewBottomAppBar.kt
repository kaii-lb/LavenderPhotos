package com.kaii.photos.compose.app_bars.favourites_grid

import android.content.Intent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastMap
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.R
import com.kaii.photos.compose.MediaPickerConfirmButton
import com.kaii.photos.compose.app_bars.IsSelectingBottomAppBar
import com.kaii.photos.compose.dialogs.user_action.ConfirmationDialog
import com.kaii.photos.compose.grids.albums.MoveCopyAlbumListView
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.toFileOperationMetadataItems
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.permissions.files.rememberFilePermissionManager
import kotlinx.coroutines.launch

@Composable
fun FavouritesViewBottomAppBar(
    selectionManager: SelectionManager,
    incomingIntent: Intent?,
    confirmToDelete: () -> Boolean,
    doNotTrash: () -> Boolean,
    runAction: (action: FileOperationAction) -> Unit
) {
    if (incomingIntent == null) {
        IsSelectingBottomAppBar {
            FavouritesBottomAppBarItems(
                selectionManager = selectionManager,
                confirmToDelete = confirmToDelete,
                doNotTrash = doNotTrash,
                process = runAction
            )
        }
    } else {
        val selectedItemsList by selectionManager.selection.collectAsStateWithLifecycle(initialValue = emptyList())

        MediaPickerConfirmButton(
            incomingIntent = incomingIntent,
            items = { selectedItemsList }
        )
    }
}

@Composable
fun FavouritesBottomAppBarItems(
    selectionManager: SelectionManager,
    confirmToDelete: () -> Boolean,
    doNotTrash: () -> Boolean,
    process: (action: FileOperationAction) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    val selectedItemsList by selectionManager.selection.collectAsStateWithLifecycle(initialValue = emptyList())

    IconButton(
        onClick = {
            coroutineScope.launch {
                process(
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
            contentDescription = stringResource(id = R.string.media_share)
        )
    }

    val show = remember { mutableStateOf(false) }

    MoveCopyAlbumListView(
        show = show,
        selectedItemsList = selectedItemsList,
        insetsPadding = WindowInsets.statusBars,
        clear = selectionManager::clear,
        isMoving = { false },
        currentAlbum = { AlbumType.PlaceHolder },
        onClick = { album ->
            process(
                FileOperationAction.Copy(
                    files = selectedItemsList.toFileOperationMetadataItems(),
                    destination = album
                )
            )
        }
    )

    IconButton(
        onClick = {
            show.value = true
        },
        enabled = selectedItemsList.isNotEmpty()
    ) {
        Icon(
            painter = painterResource(id = R.drawable.copy),
            contentDescription = stringResource(id = R.string.media_copy)
        )
    }


    var showUnFavDialog by remember { mutableStateOf(false) }

    if (showUnFavDialog) {
        ConfirmationDialog(
            title = stringResource(id = R.string.favourites_remove_this),
            confirmButtonLabel = stringResource(id = R.string.custom_album_remove_media),
            action = {
                process(
                    FileOperationAction.Favourite(
                        files = selectedItemsList.toFileOperationMetadataItems(),
                        isFavourite = false,
                        album = AlbumType.PlaceHolder
                    )
                )

                selectionManager.clear()
            },
            onDismiss = {
                showUnFavDialog = false
            }
        )
    }

    IconButton(
        onClick = {
            showUnFavDialog = true
        },
        enabled = selectedItemsList.isNotEmpty()
    ) {
        Icon(
            painter = painterResource(id = R.drawable.unfavourite),
            contentDescription = stringResource(id = R.string.custom_album_remove_media)
        )
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    val permissionState = rememberFilePermissionManager(
        onGranted = {
            if (doNotTrash()) {
                FileOperationAction.Delete(
                    files = selectedItemsList.toFileOperationMetadataItems(),
                    album = AlbumType.PlaceHolder
                )
            } else {
                FileOperationAction.Trash(
                    files = selectedItemsList.toFileOperationMetadataItems(),
                    isTrashed = true,
                    album = AlbumType.PlaceHolder
                )
            }

            selectionManager.clear()
        }
    )

    if (showDeleteDialog) {
        ConfirmationDialog(
            title = stringResource(id = if (doNotTrash()) R.string.media_delete_permanently_confirm else R.string.media_trash_confirm),
            confirmButtonLabel = stringResource(id = R.string.media_delete),
            action = {
                permissionState.get(
                    uris = selectedItemsList.fastMap { it.uri.toUri() }
                )
            },
            onDismiss = {
                showDeleteDialog = false
            }
        )
    }

    IconButton(
        onClick = {
            if (confirmToDelete()) {
                showDeleteDialog = true
            } else {
                permissionState.get(
                    uris = selectedItemsList.fastMap { it.uri.toUri() }
                )
            }
        },
        enabled = selectedItemsList.isNotEmpty()
    ) {
        Icon(
            painter = painterResource(id = R.drawable.delete),
            contentDescription = stringResource(id = R.string.media_delete)
        )
    }
}