package com.kaii.photos.compose.side_effects

import android.content.Intent
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import com.kaii.photos.R
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.permissions.files.DynamicActivityResultLauncher
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

@Composable
fun SharePhotoEffect(
    shareFlow: Flow<Result<Intent, FileOperationError>>,
    dynamicActivityResultLauncher: DynamicActivityResultLauncher,
    reShare: (files: List<FileOperationItemMetadata>) -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current

    LaunchedEffect(shareFlow) {
        shareFlow.collect { result ->
            var isError = false

            when (result) {
                is Result.Error -> when (val error = result.error) {
                    FileOperationError.Failed -> isError = true

                    is FileOperationError.RecoverableException -> {
                        dynamicActivityResultLauncher.launch(
                            intentSender = error.intentSender,
                            action = result.error.action
                        )

                        val launcherResult = dynamicActivityResultLauncher.result.first { launcherResult ->
                            val successCase = launcherResult is Result.Success && launcherResult.data is FileOperationAction.Share
                            val errorCase = launcherResult is Result.Error && launcherResult.error.action is FileOperationAction.Share

                            successCase || errorCase
                        }

                        if (launcherResult is Result.Error) {
                            isError = true
                        } else if (launcherResult is Result.Success) {
                            reShare((launcherResult.data as FileOperationAction.Share).files)
                        }
                    }
                }

                is Result.Success -> {
                    context.startActivity(
                        Intent.createChooser(
                            result.data,
                            resources.getString(R.string.media_share)
                        )
                    )
                }
            }

            if (isError) {
                LavenderSnackbarController.pushEvent(
                    event = LavenderSnackbarEvent.MessageEvent(
                        message = resources.getString(R.string.media_share_failed),
                        icon = R.drawable.error_2,
                        duration = SnackbarDuration.Short
                    )
                )
            }
        }
    }
}