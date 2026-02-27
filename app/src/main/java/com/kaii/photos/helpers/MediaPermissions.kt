package com.kaii.photos.helpers

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.R
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "com.kaii.photos.helpers.MediaPermissions"

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
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )

                Log.d(
                    TAG,
                    "Got persistent permission to access parent and child directory with uri $uri and path ${uri.path}"
                )

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
    onGetDir: (albumPath: String?, basePath: String?) -> Unit
): ManagedActivityResultLauncher<Uri?, Uri?> {
    val coroutineScope = rememberCoroutineScope()
    val resources = LocalResources.current

    return createPersistablePermissionLauncher(
        onGranted = { uri ->
            uri.path?.let { uriPath ->
                val dir = File(uriPath)

                val basePath = dir.absolutePath.split(":").first().toBasePath().let {
                    if (it.endsWith("primary/")) baseInternalStorageDirectory
                    else it
                }
                val pathSections =
                    dir.absolutePath.replace(basePath, "").split(":")
                val path = pathSections[pathSections.size - 1]

                Log.d(TAG, "Chosen directory is $path with base path $basePath")

                onGetDir(path, basePath)
            } ?: run {
                Log.e(TAG, "Path for $uri does not exist, cannot add!")
                onGetDir(null, null)
            }
        },
        onFailure = {
            Log.e(TAG, "Path for album does not exist, cannot add!")
            onGetDir(null, null)

            coroutineScope.launch {
                LavenderSnackbarController.pushEvent(
                    LavenderSnackbarEvents.MessageEvent(
                        message = resources.getString(R.string.albums_add_failed),
                        icon = R.drawable.error_2,
                        duration = SnackbarDuration.Short
                    )
                )
            }
        }
    )
}