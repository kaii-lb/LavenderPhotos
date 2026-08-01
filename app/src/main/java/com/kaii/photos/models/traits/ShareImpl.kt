package com.kaii.photos.models.traits

import android.content.Intent
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.file_management.managers.traits.Share
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

interface ShareImpl {
    fun <T : Share> T.shareFiles(
        files: List<FileOperationItemMetadata>,
        shareChannel: Channel<Result<Intent, FileOperationError>>,
        progressChannel: Channel<FileOperationProgress<Unit>>,
        appScope: CoroutineScope
    ) {
        appScope.launch {
            shareFiles(files).collect { progress ->
                progressChannel.send(
                    element = progress.toGenericProgress()
                )

                if (progress is FileOperationProgress.Finished) {
                    shareChannel.send(
                        element = progress.result
                    )
                }
            }
        }
    }
}