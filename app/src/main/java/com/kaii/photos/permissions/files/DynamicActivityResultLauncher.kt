package com.kaii.photos.permissions.files

import android.app.Activity
import android.content.IntentSender
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FilePermissionError
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.LinkedList

class DynamicActivityResultLauncher {
    private var launcher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>? = null
    private val queue = LinkedList<FileOperationAction>()

    private val _resultChannel = Channel<Result<FileOperationAction, FilePermissionError>>()
    val result = _resultChannel.receiveAsFlow()

    fun setLauncher(launcher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>?) {
        this.launcher = launcher
    }

    fun launch(
        intentSender: IntentSender,
        action: FileOperationAction
    ) {
        queue.add(action)
        launcher?.launch(
            input = IntentSenderRequest.Builder(intentSender).build()
        )
    }

    fun onResult(resultCode: Int) {
        val action = queue.pop()

        if (resultCode == Activity.RESULT_OK || resultCode == Activity.RESULT_CANCELED) {
            _resultChannel.trySend(
                element = Result.Success(action)
            )
        } else {
            _resultChannel.trySend(
                element = Result.Error(FilePermissionError(action))
            )
        }
    }
}

@Composable
fun rememberDynamicActivityResultLauncher(): DynamicActivityResultLauncher {
    val state = remember { DynamicActivityResultLauncher() }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        state.onResult(result.resultCode)
    }

    DisposableEffect(launcher, state) {
        state.setLauncher(launcher)

        onDispose {
            state.setLauncher(null)
        }
    }

    return state
}