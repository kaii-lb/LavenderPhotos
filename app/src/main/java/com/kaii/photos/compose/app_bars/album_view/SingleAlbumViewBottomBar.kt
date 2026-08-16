package com.kaii.photos.compose.app_bars.album_view

import android.content.Intent
import androidx.compose.runtime.Composable
import com.kaii.photos.compose.MediaPickerConfirmButton
import com.kaii.photos.compose.app_bars.IsSelectingBottomAppBar
import com.kaii.photos.compose.app_bars.SelectingBottomBarItems
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.helpers.grid_management.SelectionManager

@Composable
fun SingleAlbumViewBottomBar(
    albumInfo: () -> AlbumType,
    selectionManager: SelectionManager,
    incomingIntent: Intent? = null,
    confirmToDelete: () -> Boolean,
    doNotTrash: () -> Boolean,
    process: (action: FileOperationAction) -> Unit
) {
    if (incomingIntent == null) {
        IsSelectingBottomAppBar {
            SelectingBottomBarItems(
                albumInfo = albumInfo(),
                selectionManager = selectionManager,
                confirmToDelete = confirmToDelete,
                doNotTrash = doNotTrash,
                runAction = process
            )
        }
    } else {
        MediaPickerConfirmButton(
            incomingIntent = incomingIntent,
            selectionManager = selectionManager
        )
    }
}