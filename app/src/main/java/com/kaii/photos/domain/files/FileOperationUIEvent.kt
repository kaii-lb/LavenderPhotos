package com.kaii.photos.domain.files

import android.content.IntentSender
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState

sealed interface FileOperationUIEvent {
    data class ShowProgressSnackbar(
        val message: String,
        val icon: Int,
        val body: MutableState<String>,
        val progress: MutableFloatState,
    ) : FileOperationUIEvent

    data class RequestIntentSender(
        val intentSender: IntentSender
    ) : FileOperationUIEvent

    data object ShowFailureSnackbar : FileOperationUIEvent
}