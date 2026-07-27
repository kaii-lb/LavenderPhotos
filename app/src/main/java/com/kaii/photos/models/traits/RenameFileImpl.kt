package com.kaii.photos.models.traits

import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.file_management.managers.traits.RenameFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

interface RenameFileImpl {
    fun <T : RenameFile> T.renameFile(
        file: FileOperationItemMetadata,
        newName: String,
        progressChannel: Channel<FileOperationProgress<Unit>>,
        appScope: CoroutineScope
    ) {
        appScope.launch {
            val result = renameFile(
                file = file,
                newName = newName
            )

            progressChannel.send(
                element = FileOperationProgress.Finished(result = result)
            )
        }
    }
}