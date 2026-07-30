package com.kaii.photos.domain.files

import android.content.IntentSender
import com.kaii.photos.domain.Error

sealed interface FileOperationError : Error {
    object Failed : FileOperationError

    data class RecoverableException(
        val intentSender: IntentSender
    ) : FileOperationError

    data class MediaStoreRequest(
        val intentSender: IntentSender
    ) : FileOperationError
}