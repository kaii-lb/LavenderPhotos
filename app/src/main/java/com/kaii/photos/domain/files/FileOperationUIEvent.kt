package com.kaii.photos.domain.files

import android.content.IntentSender
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState

sealed interface FileOperationUIEvent {
    data object ShowFailureSnackbar : FileOperationUIEvent

    data class ShowProgressSnackbar(
        val message: String,
        val icon: Int,
        val body: MutableState<String>,
        val progress: MutableFloatState,
    ) : FileOperationUIEvent

    sealed interface LaunchDynamicResultIntent : FileOperationUIEvent {
        val intentSender: IntentSender
        val action: FileOperationAction

        data class IntentOnly(
            override val intentSender: IntentSender,
            override val action: FileOperationAction
        ) : LaunchDynamicResultIntent

        data class IntentWithFollowUpAction(
            override val intentSender: IntentSender,
            override val action: FileOperationAction,
            val followUpAction: FileOperationAction
        ) : LaunchDynamicResultIntent
    }
}