package com.kaii.photos.helpers.permissions

import android.app.Activity
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
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaPermissionsState(
    private val context: Context,
    private val launcher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>,
    private val onGranted: () -> Unit
) {
    suspend fun getPermissionsFor(
        uris: List<Uri>,
    ) = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext

        val allGranted = uris.all {
            context.checkUriPermission(
                it,
                Process.myPid(),
                Process.myUid(),
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            ) == PackageManager.PERMISSION_GRANTED
        }

        val senderRequest =
            MediaStore.createWriteRequest(
                context.contentResolver,
                uris
            ).let {
                IntentSenderRequest.Builder(it).build()
            }

        if (allGranted) {
            onGranted()
        } else {
            launcher.launch(senderRequest)
        }
    }
}

@Composable
fun rememberMediaPermissionsState(
    onFailed: () -> Unit = {},
    onGranted: () -> Unit
): MediaPermissionsState {
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK || result.resultCode == Activity.RESULT_CANCELED) {
                onGranted()
            } else {
                onFailed()
            }
        }

    val context = LocalContext.current
    return remember {
        MediaPermissionsState(
            context = context,
            launcher = launcher,
            onGranted = onGranted
        )
    }
}