package com.kaii.photos.helpers

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.os.Process
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.kaii.photos.compose.ConfirmationDialogWithBody
import com.kaii.photos.mediastore.getExternalStorageContentUriFromAbsolutePath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs

private const val TAG = "MEDIA_PERMISSIONS"

@Composable
fun GetPermissionAndRun(
    uris: List<Uri>,
    shouldRun: MutableState<Boolean>,
    onGranted: () -> Unit,
    onRejected: () -> Unit = {}
) {
    if (uris.isEmpty() || uris.all { it == "".toUri() }) return

    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            onGranted()
        } else {
            onRejected()
        }
    }

    val senderRequest = run {
        val writeRequestIntent = MediaStore.createWriteRequest(
            context.contentResolver,
            uris
        )

        IntentSenderRequest.Builder(writeRequestIntent).build()
    }

    LaunchedEffect(shouldRun.value) {
        if (shouldRun.value) {
            withContext(Dispatchers.IO) {
                val allGranted = uris.all { context.checkUriPermission(it, Process.myPid(), Process.myUid(), Intent.FLAG_GRANT_WRITE_URI_PERMISSION) == PackageManager.PERMISSION_GRANTED }
                if (allGranted) {
                    onGranted()
                } else {
                    launcher.launch(senderRequest)
                }

                Log.d(TAG, "Gotten permissions for all items? $allGranted")

                shouldRun.value = false
            }
        }
    }
}

@Composable
fun GetDirectoryPermissionAndRun(
    absolutePath: String,
    shouldRun: MutableState<Boolean>,
    onGranted: () -> Unit
) {
    val showNoPermissionForDirDialog = remember { mutableStateOf(false) }
    val context = LocalContext.current

    val launcher = createPersistablePermissionLauncher { _ ->
        shouldRun.value = true
    	onGranted()
    }

    ConfirmationDialogWithBody(
        showDialog = showNoPermissionForDirDialog,
        dialogTitle = "Permission Needed",
        dialogBody = "Lavender Photos needs permission to access this album. Please grant it the permission by selecting \"Use This Folder\" on the next screen.\n This is a one-time permission.",
        confirmButtonLabel = "Grant"
    ) {
        val uri = getExternalStorageContentUriFromAbsolutePath(absolutePath, false)
        Log.d(TAG, "Content URI for directory $absolutePath is $uri")

        launcher.launch(uri)
    }

    LaunchedEffect(shouldRun.value) {
        if (shouldRun.value) {
        	val alreadyPersisted =
                context.contentResolver.persistedUriPermissions.any {
                    val externalContentUri = getExternalStorageContentUriFromAbsolutePath(absolutePath, true)
                    Log.d(TAG, "External document URI is $externalContentUri, in persisted permissions is ${it.uri}")

                    it.uri == externalContentUri && it.isReadPermission && it.isWritePermission
                }

			val relative = absolutePath.replace(baseInternalStorageDirectory, "")
            if (!alreadyPersisted &&
            	!(relative.startsWith(Environment.DIRECTORY_DOWNLOADS) && relative.removeSuffix("/").endsWith(Environment.DIRECTORY_DOWNLOADS))
           	) {
                showNoPermissionForDirDialog.value = true
            } else {
                onGranted()
            }

            shouldRun.value = false
        }
    }
}

@Composable
fun createPersistablePermissionLauncher(
    onGranted: (uri: Uri) -> Unit
): ManagedActivityResultLauncher<Uri?, Uri?> {
    val context = LocalContext.current

    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        val uriPath = uri?.path
        if (uri != null && uriPath != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

                Log.d(TAG, "Got persistent permission to access parent and child directory with uri $uri and path $uriPath")

                onGranted(uri)
            } catch (e: Throwable) {
                Log.e(TAG, "Could not get album path.")
                e.printStackTrace()
            }
        } else {
            Toast.makeText(context, "Failed to add album :<", Toast.LENGTH_LONG).show()
        }
    }
}

