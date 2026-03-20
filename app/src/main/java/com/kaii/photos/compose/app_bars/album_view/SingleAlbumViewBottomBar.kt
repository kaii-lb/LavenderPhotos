package com.kaii.photos.compose.app_bars.album_view

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.compose.MediaPickerConfirmButton
import com.kaii.photos.compose.app_bars.IsSelectingBottomAppBar
import com.kaii.photos.compose.app_bars.SelectingBottomBarItems
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.helpers.file_management.GenericFileManager
import com.kaii.photos.helpers.grid_management.SelectionManager
import kotlin.reflect.KClass

@Composable
fun SingleAlbumViewBottomBar(
    albumInfo: () -> AlbumType,
    selectionManager: SelectionManager,
    incomingIntent: Intent? = null,
    confirmToDelete: () -> Boolean,
    doNotTrash: () -> Boolean,
    allowedAlbumsFor: (action: GenericFileManager.Action) -> List<KClass<out AlbumType>>,
    process: (list: List<SelectionManager.SelectedItem>, album: AlbumType, isMoving: Boolean) -> Unit
) {
    if (incomingIntent == null) {
        IsSelectingBottomAppBar {
            SelectingBottomBarItems(
                albumInfo = albumInfo(),
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

        MediaPickerConfirmButton(
            incomingIntent = incomingIntent,
            uris = selectedItemsList.fastMap { it.toUri() }, // TODO
            contentResolver = context.contentResolver
        )
    }
}