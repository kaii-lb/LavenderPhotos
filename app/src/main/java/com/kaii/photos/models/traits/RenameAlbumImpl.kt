package com.kaii.photos.models.traits

import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.file_management.managers.traits.RenameAlbum
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

interface RenameAlbumImpl {
    fun <T : RenameAlbum> T.renameAlbum(
        album: AlbumType,
        newName: String,
        progressChannel: Channel<FileOperationProgress<Unit>>,
        appScope: CoroutineScope
    ) {
        appScope.launch {
            val result = renameAlbum(
                album = album,
                newName = newName,
                existingTaskId = null
            )

            progressChannel.send(
                element = FileOperationProgress.Finished(result = result)
            )
        }
    }
}