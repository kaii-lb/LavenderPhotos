package com.kaii.photos.models.traits

import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.file_management.managers.traits.Delete
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

interface DeleteImpl {
    fun <T : Delete> T.deleteFiles(
        files: List<FileOperationItemMetadata>,
        album: AlbumType,
        progressChannel: Channel<FileOperationProgress<Unit>>,
        appScope: CoroutineScope
    ) {
        appScope.launch {
            deleteFiles(
                files = files,
                albumId = album.id,
                immichId = album.immichId,
                existingTaskId = null
            ).collect { progress ->
                progressChannel.send(
                    element = progress.toGenericProgress()
                )
            }
        }
    }
}