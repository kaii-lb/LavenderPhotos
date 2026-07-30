package com.kaii.photos.compose.side_effects

import android.content.Intent
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import com.kaii.photos.R
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FilePermissionAction
import com.kaii.photos.permissions.files.DynamicActivityResultLauncher
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import kotlinx.coroutines.flow.Flow

@Composable
fun SharePhotoEffect(
    shareFlow: Flow<Result<Intent, FileOperationError>>,
    dynamicActivityResultLauncher: DynamicActivityResultLauncher,
    reShare: () -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current

    LaunchedEffect(shareFlow) {
        shareFlow.collect { result ->
            var isError = false

            when (result) {
                is Result.Error -> when (val error = result.error) {
                    FileOperationError.Failed -> isError = true

                    is FileOperationError.MediaStoreRequest -> {
                        context.startIntentSender(error.intentSender, null, 0, 0, 0)
                    }

                    is FileOperationError.RecoverableException -> {
                        dynamicActivityResultLauncher.launch(error.intentSender, FilePermissionAction.Share)

                        dynamicActivityResultLauncher.result.collect {
                            if (it is Result.Error) {
                                isError = true
                            } else if ((it as Result.Success).data == FilePermissionAction.Share) {
                                reShare()
                            }
                        }
                    }
                }

                is Result.Success -> {
                    context.startActivity(result.data)
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