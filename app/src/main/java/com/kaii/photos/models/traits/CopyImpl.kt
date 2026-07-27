package com.kaii.photos.models.traits

import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.file_management.managers.traits.Copy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

interface CopyImpl {
    fun <T : Copy> T.copyFiles(
        files: List<FileOperationItemMetadata>,
        destination: AlbumType,
        progressChannel: Channel<FileOperationProgress<Unit>>,
        appScope: CoroutineScope
    ) {
        appScope.launch {
            copyFiles(
                files = files,
                destination = destination,
                existingTaskId = null
            ).collect { progress ->
                progressChannel.send(
                    element = progress.toGenericProgress()
                )
            }
        }
    }
}