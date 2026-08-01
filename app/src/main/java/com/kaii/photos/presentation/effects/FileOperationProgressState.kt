package com.kaii.photos.presentation.effects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalResources
import com.kaii.photos.data.providers.FileOperationSnackbarInfoProvider
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.domain.files.FileOperationUIEvent
import com.kaii.photos.permissions.files.DynamicActivityResultLauncher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

@Stable
class FileOperationState(
    private val infoProvider: FileOperationSnackbarInfoProvider,
    private val dynamicActivityResultLauncher: DynamicActivityResultLauncher,
    private val rerunAction: State<(FileOperationAction) -> Unit>,
) {
    private val eventsChannel = Channel<FileOperationUIEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    suspend fun observe(operationFlow: Flow<FileOperationProgress<Unit>>) {
        var totalFileCount = 0
        var currentFileCount = 0
        var action: FileOperationAction.LongOperationType? = null
        val snackbarBody = mutableStateOf("")
        val snackbarProgress = mutableFloatStateOf(0f)

        operationFlow.collect { progress ->
            var isError = false

            when (progress) {
                is FileOperationProgress.Started -> {
                    currentFileCount = 0
                    totalFileCount = progress.fileCount
                    action = progress.action

                    snackbarBody.value = infoProvider.getBodyFor(progress.action, currentFileCount, totalFileCount)
                    snackbarProgress.floatValue = 0f

                    eventsChannel.send(
                        element = FileOperationUIEvent.ShowProgressSnackbar(
                            message = infoProvider.getMessageFor(action),
                            icon = infoProvider.getIconFor(action),
                            body = snackbarBody,
                            progress = snackbarProgress
                        )
                    )
                }

                is FileOperationProgress.ItemDone -> {
                    currentFileCount += 1
                    snackbarProgress.floatValue = currentFileCount.toFloat() / totalFileCount
                    snackbarBody.value = infoProvider.getBodyFor(action!!, currentFileCount, totalFileCount)
                }

                is FileOperationProgress.Finished -> when (val result = progress.result) {
                    is Result.Error -> when (val error = result.error) {
                        FileOperationError.Failed -> isError = true

                        is FileOperationError.MediaStoreRequest -> {
                            eventsChannel.send(
                                element = FileOperationUIEvent.RequestIntentSender(
                                    intentSender = error.intentSender
                                )
                            )
                        }

                        is FileOperationError.RecoverableException -> {
                            dynamicActivityResultLauncher.launch(
                                intentSender = error.intentSender,
                                action = result.error.action
                            )

                            dynamicActivityResultLauncher.result.collect { launcherResult ->
                                if (launcherResult is Result.Error) {
                                    isError = true
                                } else if (launcherResult is Result.Success && launcherResult.data is FileOperationAction.Share) {
                                    rerunAction.value(launcherResult.data)
                                }
                            }
                        }
                    }

                    is Result.Success -> {
                        currentFileCount = totalFileCount

                        snackbarProgress.floatValue = 1f
                        snackbarBody.value = infoProvider.getBodyFor(action!!, currentFileCount, totalFileCount)
                    }
                }
            }

            if (isError) {
                eventsChannel.send(
                    element = FileOperationUIEvent.ShowFailureSnackbar
                )
            }
        }
    }
}

@Composable
fun rememberFileOperationProgressState(
    dynamicActivityResultLauncher: DynamicActivityResultLauncher,
    rerunAction: (action: FileOperationAction) -> Unit
): FileOperationState {
    val resources = LocalResources.current
    val updatedRerunAction = rememberUpdatedState(rerunAction)

    return remember(resources, dynamicActivityResultLauncher) {
        FileOperationState(
            infoProvider = FileOperationSnackbarInfoProvider(resources),
            dynamicActivityResultLauncher = dynamicActivityResultLauncher,
            rerunAction = updatedRerunAction
        )
    }
}