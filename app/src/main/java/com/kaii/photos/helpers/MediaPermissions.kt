package com.kaii.photos.helpers

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.kaii.lavender_snackbars.LavenderSnackbarController
import com.kaii.lavender_snackbars.LavenderSnackbarEvents
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.ConfirmationDialogWithBody
import com.kaii.photos.mediastore.getExternalStorageContentUriFromAbsolutePath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
                Log.d(TAG, "Gotten permissions for all items? $allGranted")

                if (allGranted) {
                    onGranted()
                } else {
                    launcher.launch(senderRequest)
                }

                shouldRun.value = false
            }
        }
    }
}

/** [onGranted] return the list of permission granted absolutePaths from [absoluteDirPaths] */
@Throws(IllegalStateException::class)
@Composable
fun GetDirectoryPermissionAndRun(
    absoluteDirPaths: List<String>,
    shouldRun: MutableState<Boolean>,
    onGranted: (grantedPaths: List<String>) -> Unit,
    onRejected: () -> Unit
) {
    val showNoPermissionForDirDialog = remember { mutableStateOf(false) }
    val context = LocalContext.current

    var currentIndex by remember { mutableIntStateOf(0) }
    val grantedList = remember { mutableStateListOf<String>() }

    val launcher = createPersistablePermissionLauncher(
        onGranted = { _ ->
            grantedList.add(absoluteDirPaths[currentIndex])
            currentIndex += 1
        },

        onFailure = {
            currentIndex += 1
        }
    )

    ConfirmationDialogWithBody(
        showDialog = showNoPermissionForDirDialog,
        dialogTitle = "Permission Needed",
        dialogBody = "Lavender Photos needs permission to access this album. Please grant it the permission by selecting \"Use This Folder\" on the next screen.\n This is a one-time permission.",
        confirmButtonLabel = "Grant"
    ) {
        val uri = context.getExternalStorageContentUriFromAbsolutePath(absoluteDirPaths[currentIndex], false)
        Log.d(TAG, "Content URI for directory ${absoluteDirPaths[currentIndex]} is $uri")

        launcher.launch(uri)
    }

    LaunchedEffect(currentIndex) {
        if (currentIndex >= absoluteDirPaths.size - 1 && grantedList.isNotEmpty()) onGranted(grantedList.toList())
        else if (currentIndex >= absoluteDirPaths.size - 1) onRejected() // grantedList IS empty
    }

    LaunchedEffect(shouldRun.value, absoluteDirPaths) {
        if (shouldRun.value) {
        	if (absoluteDirPaths.all { it == "" }) {
        	    Log.e(TAG, "Cannot get permissions for empty directory list!")
        	    return@LaunchedEffect
        	}

            absoluteDirPaths.forEachIndexed { index, absolutePath ->
            	Log.d(TAG, "getting permission for $absolutePath")
                val alreadyPersisted =
                    context.contentResolver.persistedUriPermissions.any {
                        val externalContentUri = context.getExternalStorageContentUriFromAbsolutePath(absolutePath, true)

                        it.uri == externalContentUri && it.isReadPermission && it.isWritePermission
                    }

				Log.d(TAG, "already have permission for $absolutePath? $alreadyPersisted")

                if (!alreadyPersisted && !absolutePath.checkPathIsDownloads()) {
                    showNoPermissionForDirDialog.value = true
                } else {
                    grantedList.add(absolutePath)
                    currentIndex += 1
                }

                while (currentIndex == index) {
                    delay(100)
                    Log.d(TAG, "delaying execution")
                }
            }

            Log.d(TAG, "Finished granting permissions for $absoluteDirPaths")

            shouldRun.value = false
        }
    }
}

/** notifies user via a snackbar if adding the directory fails */
@Composable
fun createPersistablePermissionLauncher(
    onGranted: (uri: Uri) -> Unit,
    onFailure: () -> Unit
): ManagedActivityResultLauncher<Uri?, Uri?> {
    val context = LocalContext.current

    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        try {
            if (uri != null && uri.path != null) {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

                Log.d(TAG, "Got persistent permission to access parent and child directory with uri $uri and path ${uri.path}")

                onGranted(uri)
            } else {
                throw Exception("Requested permission has a null URI, cannot proceed.")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed granting persistable permission")
            Log.e(TAG, e.toString())
            e.printStackTrace()

            onFailure()
        }
    }
}

/** notifies user via a snackbar if adding the directory fails */
@Composable
fun createDirectoryPicker(
    onGetDir: (albumPath: String?) -> Unit
): ManagedActivityResultLauncher<Uri?, Uri?> {
    val coroutineScope = rememberCoroutineScope()

    return createPersistablePermissionLauncher(
        onGranted = { uri ->
            uri.path?.let {
                val dir = File(it)

                val pathSections = dir.absolutePath.replace(baseInternalStorageDirectory, "").split(":")
                val path = pathSections[pathSections.size - 1]

                Log.d(TAG, "Chosen directory is $path")

                onGetDir(path)
            } ?: run {
                Log.e(TAG, "Path for $uri does not exist, cannot add!")
                onGetDir(null)
            }
        },
        onFailure = {
            Log.e(TAG, "Path for album does not exist, cannot add!")
            onGetDir(null)

            coroutineScope.launch {
                LavenderSnackbarController.pushEvent(
                    LavenderSnackbarEvents.MessageEvent(
                        message = "Failed to add album :<",
                        iconResId = R.drawable.error_2,
                        duration = SnackbarDuration.Short
                    )
                )
            }
        }
    )
}

