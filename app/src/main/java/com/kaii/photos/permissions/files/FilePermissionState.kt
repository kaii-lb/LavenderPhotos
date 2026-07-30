package com.kaii.photos.permissions.files

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
import android.provider.MediaStore
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

class FilePermissionsState(
    private val context: Context,
    private val onGranted: () -> Unit
) {
    private var launcher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>? = null

    private fun createSenderRequest(uris: List<Uri>): IntentSenderRequest {
        val writeRequestIntent = MediaStore.createWriteRequest(
            context.contentResolver,
            uris
        )

        return IntentSenderRequest.Builder(writeRequestIntent.intentSender).build()
    }

    private fun checkGranted(uris: List<Uri>) = uris.all {
        context.checkUriPermission(
            it,
            Process.myPid(),
            Process.myUid(),
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun setLauncher(launcher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>?) {
        this.launcher = launcher
    }

    fun get(uris: List<Uri>) {
        if (uris.isEmpty() || checkGranted(uris = uris)) {
            onGranted()
            return
        }

        launcher!!.launch(createSenderRequest(uris = uris))
    }
}

@Composable
fun rememberFilePermissionManager(
    onGranted: () -> Unit,
    onRejected: () -> Unit = {}
): FilePermissionsState {
    val context = LocalContext.current
    val state = remember {
        FilePermissionsState(
            context = context,
            onGranted = onGranted
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK || result.resultCode == RESULT_CANCELED) {
            onGranted()
        } else {
            onRejected()
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