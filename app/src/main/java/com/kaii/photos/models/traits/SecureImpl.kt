package com.kaii.photos.models.traits

import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.file_management.managers.traits.Secure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

interface SecureImpl {
    fun <T : Secure> T.encryptFiles(
        files: List<FileOperationItemMetadata>,
        progressChannel: Channel<FileOperationProgress<Unit>>,
        appScope: CoroutineScope
    ) {
        appScope.launch {
            encryptFiles(files).collect { progress ->
                progressChannel.send(
                    element = progress.toGenericProgress()
                )
            }
        }
    }
}