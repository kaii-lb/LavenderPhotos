package com.kaii.photos.compose.dialogs.single_photo_info_dialog

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.domain.files.FileOperationAction

@Composable
fun RowScope.IconContent(
    mediaItem: () -> MediaStoreData,
    showMoveCopyOptions: Boolean,
    privacyMode: () -> Boolean,
    album: () -> AlbumType,
    dismiss: () -> Unit,
    runAction: (action: FileOperationAction) -> Any?
) {
    IconContentImpl(
        mediaItem = mediaItem,
        showMoveCopyOptions = showMoveCopyOptions,
        privacyMode = privacyMode,
        album = album,
        modifier = Modifier.weight(1f),
        dismiss = dismiss,
        runAction = runAction
    )
}

@Composable
fun ColumnScope.IconContent(
    mediaItem: () -> MediaStoreData,
    showMoveCopyOptions: Boolean,
    privacyMode: () -> Boolean,
    album: () -> AlbumType,
    dismiss: () -> Unit,
    runAction: (action: FileOperationAction) -> Any?
) {
    IconContentImpl(
        mediaItem = mediaItem,
        showMoveCopyOptions = showMoveCopyOptions,
        privacyMode = privacyMode,
        album = album,
        modifier = Modifier.weight(1f),
        dismiss = dismiss,
        runAction = runAction
    )
}