package com.kaii.photos.permissions.files

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.kaii.photos.helpers.renameImage

class MediaRenamer(
    private val context: Context
) {
    private var uri: Uri? = null
    private var newName: String? = null
    internal var launcher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>? = null

    fun rename(newName: String, uri: Uri) {
        this.uri = uri
        this.newName = newName

        val intentSender = renameImage(
            context = context,
            uri = uri,
            newName = newName
        )

        if (intentSender != null) {
            val request = IntentSenderRequest.Builder(intentSender).build()
            launcher!!.launch(request)
        } else {
            this.uri = null
            this.newName = null
        }
    }

    internal fun retry() {
        rename(
            newName = newName!!,
            uri = uri!!
        )
    }
}

@Composable
fun rememberMediaRenamer(
    onFailure: () -> Unit
): MediaRenamer {
    val context = LocalContext.current
    val state = remember {
        MediaRenamer(context = context)
    }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK || result.resultCode == Activity.RESULT_CANCELED) {
                state.retry()
            } else {
                onFailure()
            }
        }

    DisposableEffect(launcher, state) {
        state.launcher = launcher

        onDispose {
            state.launcher == null
        }
    }

    return state
}