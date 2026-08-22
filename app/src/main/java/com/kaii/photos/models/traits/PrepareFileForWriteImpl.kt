package com.kaii.photos.models.traits

import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.file_management.managers.traits.PrepareFileForWrite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

interface PrepareFileForWriteImpl {
    fun <T : PrepareFileForWrite> T.prepareFileForWrite(
        files: List<FileOperationItemMetadata>,
        followUpAction: FileOperationAction,
        progressChannel: Channel<FileOperationProgress<Unit>>,
        appScope: CoroutineScope
    ) {
        appScope.launch {
            progressChannel.send(
                element = FileOperationProgress.Finished(
                    result = prepareFileForWrite(files, followUpAction)
                )
            )
        }
    }
}