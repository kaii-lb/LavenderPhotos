package com.kaii.photos.models.traits

import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.file_management.managers.traits.ClearExif
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

interface ClearExifImpl {
    fun <T : ClearExif> T.clearExifData(
        absolutePath: String,
        progressChannel: Channel<FileOperationProgress<Unit>>,
        appScope: CoroutineScope
    ) {
        appScope.launch {
            progressChannel.send(
                element = FileOperationProgress.Finished(
                    result = clearExifData(
                        absolutePath = absolutePath
                    )
                )
            )
        }
    }
}