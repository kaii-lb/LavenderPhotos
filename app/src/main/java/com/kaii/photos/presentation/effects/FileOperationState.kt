package com.kaii.photos.presentation.effects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalResources
import com.kaii.photos.data.providers.FileOperationSnackbarInfoProvider
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.domain.files.FileOperationUIEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

@Stable
class FileOperationState(
    private val infoProvider: FileOperationSnackbarInfoProvider
) {
    private val eventsChannel = Channel<FileOperationUIEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    private var totalFileCount = 0
    private var currentFileCount = 0
    private var currentAction: FileOperationAction.LongOperationType? = null
    private val snackbarBody = mutableStateOf("")
    private val snackbarProgress = mutableFloatStateOf(0f)

    fun resetAction() {
        currentAction = null
    }

    fun markSucceeded() {
        currentFileCount = totalFileCount

        if (currentAction != null) {
            snackbarProgress.floatValue = 1f
            snackbarBody.value = infoProvider.getBodyFor(currentAction!!, currentFileCount, totalFileCount)
        }

        resetAction()
    }

    suspend fun observe(operationFlow: Flow<FileOperationProgress<Unit>>) {
        operationFlow.collect { progress ->
            when (progress) {
                is FileOperationProgress.Started -> {
                    currentFileCount = 0
                    totalFileCount = progress.fileCount

                    snackbarBody.value = infoProvider.getBodyFor(progress.action, currentFileCount, totalFileCount)
                    snackbarProgress.floatValue = 0f

                    // only show a new snackbar if the current one isn't of the same operation type
                    if (currentAction != progress.action) {
                        currentAction = progress.action

                        eventsChannel.send(
                            element = FileOperationUIEvent.ShowProgressSnackbar(
                                message = infoProvider.getMessageFor(progress.action),
                                icon = infoProvider.getIconFor(progress.action),
                                body = snackbarBody,
                                progress = snackbarProgress
                            )
                        )
                    }
                }

                is FileOperationProgress.ItemDone -> {
                    currentFileCount += 1
                    snackbarProgress.floatValue = currentFileCount.toFloat() / totalFileCount
                    snackbarBody.value = infoProvider.getBodyFor(currentAction!!, currentFileCount, totalFileCount)
                }

                is FileOperationProgress.Finished -> {
                    when (val result = progress.result) {
                        is Result.Error -> {
                            when (result.error) {
                                FileOperationError.Failed -> {
                                    eventsChannel.send(
                                        element = FileOperationUIEvent.ShowFailureSnackbar
                                    )

                                    resetAction()
                                }

                                is FileOperationError.RecoverableException -> {
                                    val element = when (val error = result.error) {
                                        is FileOperationError.RecoverableException.RequiresConsentOnly ->
                                            FileOperationUIEvent.LaunchDynamicResultIntent.IntentOnly(
                                                intentSender = error.intentSender,
                                                action = error.action
                                            )

                                        is FileOperationError.RecoverableException.RequiresFollowUp ->
                                            FileOperationUIEvent.LaunchDynamicResultIntent.IntentWithFollowUpAction(
                                                intentSender = error.intentSender,
                                                action = error.action,
                                                followUpAction = error.followUpAction
                                            )
                                    }

                                    eventsChannel.send(element = element)
                                }
                            }
                        }

                        is Result.Success -> markSucceeded()
                    }
                }
            }
        }
    }
}

@Composable
fun rememberFileOperationProgressState(): FileOperationState {
    val resources = LocalResources.current

    return remember(resources) {
        FileOperationState(
            infoProvider = FileOperationSnackbarInfoProvider(
                resources = resources
            )
        )
    }
}