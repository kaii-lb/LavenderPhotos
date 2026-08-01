package com.kaii.photos.compose.side_effects

import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import com.kaii.photos.R
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.domain.files.FileOperationUIEvent
import com.kaii.photos.permissions.files.DynamicActivityResultLauncher
import com.kaii.photos.presentation.effects.rememberFileOperationProgressState
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

@Composable
fun FileOperationProgressEffect(
    operationFlow: Flow<FileOperationProgress<Unit>>,
    dynamicActivityResultLauncher: DynamicActivityResultLauncher,
    runAction: (action: FileOperationAction) -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val state = rememberFileOperationProgressState()

    LaunchedEffect(operationFlow, state) {
        state.observe(operationFlow)
    }

    LaunchedEffect(state.events) {
        state.events.collect { event ->
            when (event) {
                is FileOperationUIEvent.ShowProgressSnackbar -> {
                    LavenderSnackbarController.pushEvent(
                        event = LavenderSnackbarEvent.ProgressEvent(
                            message = event.message,
                            icon = event.icon,
                            body = event.body,
                            percentage = event.progress
                        )
                    )
                }

                is FileOperationUIEvent.ShowFailureSnackbar -> {
                    LavenderSnackbarController.pushEvent(
                        event = LavenderSnackbarEvent.MessageEvent(
                            message = resources.getString(R.string.media_snackbar_operation_failed),
                            icon = R.drawable.error_2,
                            duration = SnackbarDuration.Short
                        )
                    )
                }

                is FileOperationUIEvent.RequestIntentSender -> {
                    context.startIntentSender(event.intentSender, null, 0, 0, 0)
                }

                is FileOperationUIEvent.LaunchDynamicResultIntent -> {
                    dynamicActivityResultLauncher.launch(
                        intentSender = event.intentSender,
                        action = event.action
                    )

                    val launcherResult = dynamicActivityResultLauncher.result.first { launcherResult ->
                        val successCase = launcherResult is Result.Success && launcherResult.data::class == event.action::class
                        val errorCase = launcherResult is Result.Error && launcherResult.error.action::class == event.action::class

                        successCase || errorCase
                    }

                    if (launcherResult is Result.Error) {
                        LavenderSnackbarController.pushEvent(
                            event = LavenderSnackbarEvent.MessageEvent(
                                message = resources.getString(R.string.media_snackbar_operation_failed),
                                icon = R.drawable.error_2,
                                duration = SnackbarDuration.Short
                            )
                        )
                    } else if (launcherResult is Result.Success) {
                        runAction(launcherResult.data)
                    }
                }
            }
        }
    }
}