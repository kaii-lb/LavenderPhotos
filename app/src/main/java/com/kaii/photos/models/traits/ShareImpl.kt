package com.kaii.photos.models.traits

import android.content.Intent
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.file_management.managers.traits.Share
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

interface ShareImpl {
    fun <T : Share> T.shareFiles(
        files: List<FileOperationItemMetadata>,
        shareChannel: Channel<Result<Intent, FileOperationError>>,
        appScope: CoroutineScope
    ) {
        appScope.launch {
            val result = shareFiles(files)

            shareChannel.send(
                element = result
            )
        }
    }
}