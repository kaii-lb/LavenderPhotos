package com.kaii.photos.domain.files

import android.content.IntentSender
import com.kaii.photos.domain.Error

sealed interface FileOperationError : Error {
    object Failed : FileOperationError

    sealed interface RecoverableException : FileOperationError {
        val intentSender: IntentSender
        val action: FileOperationAction

        data class RequiresConsentOnly(
            override val intentSender: IntentSender,
            override val action: FileOperationAction
        ) : RecoverableException

        data class RequiresFollowUp(
            override val intentSender: IntentSender,
            override val action: FileOperationAction,
            val followUpAction: FileOperationAction
        ) : RecoverableException
    }
}