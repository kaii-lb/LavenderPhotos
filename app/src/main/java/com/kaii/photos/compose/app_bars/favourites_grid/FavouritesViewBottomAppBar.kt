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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastMap
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.R
import com.kaii.photos.compose.MediaPickerConfirmButton
import com.kaii.photos.compose.app_bars.IsSelectingBottomAppBar
import com.kaii.photos.compose.dialogs.user_action.ConfirmationDialog
import com.kaii.photos.compose.grids.MoveCopyAlbumListView
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.di.appModule
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.permanentlyDeletePhotoList
import com.kaii.photos.helpers.setTrashedOnPhotoList
import com.kaii.photos.helpers.shareMultipleImages
import com.kaii.photos.permissions.favourites.rememberListFavouritesState
import com.kaii.photos.permissions.files.rememberFilePermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun FavouritesViewBottomAppBar(
    selectionManager: SelectionManager,
    incomingIntent: Intent?,
    confirmToDelete: () -> Boolean,
    doNotTrash: () -> Boolean,
    preserveDate: () -> Boolean
) {
    if (incomingIntent == null) {
        IsSelectingBottomAppBar {
            FavouritesBottomAppBarItems(
                selectionManager = selectionManager,
                confirmToDelete = confirmToDelete,
                doNotTrash = doNotTrash,
                preserveDate = preserveDate
            )
        }
    } else {
        val context = LocalContext.current
        val selectedItemsList by selectionManager.selection.collectAsStateWithLifecycle(initialValue = emptyList())

        // TODO: move to file manager
        MediaPickerConfirmButton(
            incomingIntent = incomingIntent,
            uris = selectedItemsList.fastMap { it.uri.toUri() },
            contentResolver = context.contentResolver
        )
    }
}

@Composable
fun FavouritesBottomAppBarItems(
    selectionManager: SelectionManager,
    confirmToDelete: () -> Boolean,
    doNotTrash: () -> Boolean,
    preserveDate: () -> Boolean
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val selectedItemsList by selectionManager.selection.collectAsStateWithLifecycle(initialValue = emptyList())

    IconButton(
        onClick = {
            coroutineScope.launch {
                // TODO: move to file manager
                shareMultipleImages(
                    uris = selectedItemsList.fastMap { it.uri.toUri() },
                    context = context,
                    hasVideos = selectedItemsList.fastAny { !it.isImage }
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
        allowedAlbumsFor = { emptyList() }, // TODO
        onClick = {}
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

    val showUnFavDialog = remember { mutableStateOf(false) }
    val favState = rememberListFavouritesState {
        selectionManager.clear()
    }

    ConfirmationDialog(
        showDialog = showUnFavDialog,
        dialogTitle = stringResource(id = R.string.favourites_remove_this),
        confirmButtonLabel = stringResource(id = R.string.custom_album_remove_media)
    ) {
        coroutineScope.launch {
            // TODO: move to file manager
            favState.setFavourite(
                favourite = false,
                list = selectedItemsList.fastMap { it.uri.toUri() }
            )
        }
    }

    IconButton(
        onClick = {
            showUnFavDialog.value = true
        },
        enabled = selectedItemsList.isNotEmpty()
    ) {
        Icon(
            painter = painterResource(id = R.drawable.unfavourite),
            contentDescription = stringResource(id = R.string.custom_album_remove_media)
        )
    }

    val showDeleteDialog = remember { mutableStateOf(false) }
    val permissionState = rememberFilePermissionManager(
        onGranted = {
            context.appModule.scope.launch(Dispatchers.IO) {
                // TODO: move to file manager
                if (doNotTrash()) {
                    permanentlyDeletePhotoList(
                        context = context,
                        list = selectedItemsList.fastMap { it.uri.toUri() }
                    )
                } else {
                    setTrashedOnPhotoList(
                        context = context,
                        list = selectedItemsList.fastMap { it.uri.toUri() },
                        trashed = true
                    )
                }

                selectionManager.clear()
            }
        }
    )

    ConfirmationDialog(
        showDialog = showDeleteDialog,
        dialogTitle = stringResource(id = if (doNotTrash()) R.string.media_delete_permanently_confirm else R.string.media_trash_confirm),
        confirmButtonLabel = stringResource(id = R.string.media_delete)
    ) {
        coroutineScope.launch {
            // TODO: move to file manager
            permissionState.get(
                uris = selectedItemsList.fastMap { it.uri.toUri() }
            )
        }
    }

    IconButton(
        onClick = {
            if (confirmToDelete()) {
                showDeleteDialog.value = true
            } else {
                coroutineScope.launch {
                    // TODO: move to file manager
                    permissionState.get(
                        uris = selectedItemsList.fastMap { it.uri.toUri() }
                    )
                }
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