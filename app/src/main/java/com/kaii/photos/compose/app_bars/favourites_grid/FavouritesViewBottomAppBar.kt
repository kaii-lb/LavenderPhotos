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
import androidx.compose.ui.util.fastMap
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.R
import com.kaii.photos.compose.MediaPickerConfirmButton
import com.kaii.photos.compose.app_bars.IsSelectingBottomAppBar
import com.kaii.photos.compose.dialogs.user_action.ConfirmationDialog
import com.kaii.photos.compose.grids.MoveCopyAlbumListView
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.file_management.managers.GenericFileManager
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.permissions.files.rememberFilePermissionManager
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

@Composable
fun FavouritesViewBottomAppBar(
    selectionManager: SelectionManager,
    incomingIntent: Intent?,
    confirmToDelete: () -> Boolean,
    doNotTrash: () -> Boolean,
    allowedAlbumsFor: (moving: Boolean) -> List<KClass<out AlbumType>>,
    process: (action: GenericFileManager.Action) -> Unit
) {
    if (incomingIntent == null) {
        IsSelectingBottomAppBar {
            FavouritesBottomAppBarItems(
                selectionManager = selectionManager,
                confirmToDelete = confirmToDelete,
                doNotTrash = doNotTrash,
                allowedAlbumsFor = allowedAlbumsFor,
                process = process
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
    allowedAlbumsFor: (moving: Boolean) -> List<KClass<out AlbumType>>,
    process: (action: GenericFileManager.Action) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    val selectedItemsList by selectionManager.selection.collectAsStateWithLifecycle(initialValue = emptyList())

    IconButton(
        onClick = {
            coroutineScope.launch {
                process(
                    GenericFileManager.Action.Share(
                        list = selectedItemsList
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
        allowedAlbumsFor = { allowedAlbumsFor(false) },
        onClick = { album ->
            process(
                GenericFileManager.Action.Copy(
                    list = selectedItemsList,
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


    val showUnFavDialog = remember { mutableStateOf(false) }
    ConfirmationDialog(
        showDialog = showUnFavDialog,
        dialogTitle = stringResource(id = R.string.favourites_remove_this),
        confirmButtonLabel = stringResource(id = R.string.custom_album_remove_media)
    ) {
        process(
            GenericFileManager.Action.Favourite(
                list = selectedItemsList,
                favourite = false
            )
        )

        selectionManager.clear()
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
            if (doNotTrash()) {
                GenericFileManager.Action.Delete(
                    list = selectedItemsList
                )
            } else {
                GenericFileManager.Action.Trash(
                    list = selectedItemsList,
                    trashed = true
                )
            }

            selectionManager.clear()
        }
    )

    ConfirmationDialog(
        showDialog = showDeleteDialog,
        dialogTitle = stringResource(id = if (doNotTrash()) R.string.media_delete_permanently_confirm else R.string.media_trash_confirm),
        confirmButtonLabel = stringResource(id = R.string.media_delete)
    ) {
        permissionState.get(
            uris = selectedItemsList.fastMap { it.uri.toUri() }
        )
    }

    IconButton(
        onClick = {
            if (confirmToDelete()) {
                showDeleteDialog.value = true
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