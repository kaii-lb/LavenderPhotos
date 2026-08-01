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
import androidx.compose.runtime.rememberCoroutineScope
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FilePermissionError
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.LinkedList

class DynamicActivityResultLauncher {
    private var launcher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>? = null
    private val queue = LinkedList<FileOperationAction>()

    private val _resultChannel = Channel<Result<FileOperationAction, FilePermissionError>>(Channel.BUFFERED)
    val result = _resultChannel.receiveAsFlow()

    fun setLauncher(launcher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>?) {
        this.launcher = launcher
    }

    fun launch(
        intentSender: IntentSender,
        action: FileOperationAction
    ) {
        queue.add(action)
        launcher!!.launch(
            input = IntentSenderRequest.Builder(intentSender).build()
        )
    }

    suspend fun onResult(resultCode: Int) {
        val action = queue.remove()

        if (resultCode == Activity.RESULT_OK || resultCode == Activity.RESULT_CANCELED) {
            _resultChannel.send(
                element = Result.Success(action)
            )
        } else {
            _resultChannel.send(
                element = Result.Error(FilePermissionError(action))
            )
        }
    }
}

@Composable
fun rememberDynamicActivityResultLauncher(): DynamicActivityResultLauncher {
    val coroutineScope = rememberCoroutineScope()
    val state = remember { DynamicActivityResultLauncher() }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        coroutineScope.launch {
            state.onResult(result.resultCode)
        }
    }

    DisposableEffect(launcher, state) {
        state.setLauncher(launcher)

        onDispose {
            state.setLauncher(null)
        }
    }

    return state
}