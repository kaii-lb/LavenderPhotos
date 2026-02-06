package com.kaii.photos.permissions.files

import android.content.Context
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.util.fastAll
import com.kaii.photos.helpers.checkPathIsDownloads
import com.kaii.photos.helpers.createPersistablePermissionLauncher
import com.kaii.photos.mediastore.getExternalStorageContentUriFromAbsolutePath

class DirectoryPermissionManager(
    private val context: Context,
    private val onGranted: () -> Unit,
    private val onFailed: () -> Unit
) {
    var launcher: ManagedActivityResultLauncher<Uri?, Uri?>? = null

    private var running = false
    private val directories = mutableListOf<String>()

    internal fun getDirPermission(
        directory: String
    ) {
        val uri = context.getExternalStorageContentUriFromAbsolutePath(absolutePath = directory, trimDoc = false)

        launcher!!.launch(uri)
    }

    internal fun fetch() {
        if (!running) return
        getDirPermission(directory = directories.first())
    }

    internal fun step(success: Boolean) {
        if (directories.isEmpty() && success) {
            running = false
            onGranted()
        } else if (!success) {
            running = false
            onFailed()
        }
    }

    internal fun success() {
        directories.removeAt(0)
    }

    fun start(directories: List<String>) {
        if (running) throw IllegalStateException("Cannot get directory permissions while another permission request is running!")
        if (directories.all { it.isBlank() }) throw IllegalArgumentException("Cannot get directory permissions for directories with blank paths!")

        val previous = context.contentResolver.persistedUriPermissions
        val persisted = directories.fastAll { path ->
            val uri = context.getExternalStorageContentUriFromAbsolutePath(absolutePath = path, trimDoc = true)

            previous.find { perm ->
                perm.uri == uri && perm.isReadPermission && perm.isWritePermission
            } != null || path.checkPathIsDownloads()
        }

        if (persisted) {
            running = false
            onGranted()
            return
        }

        this.directories.clear()
        this.directories.addAll(directories)
        running = true
        fetch()
    }
}

@Composable
fun rememberDirectoryPermissionManager(
    onGranted: () -> Unit,
    onRejected: () -> Unit = {}
): DirectoryPermissionManager {
    val context = LocalContext.current
    val state = remember(onGranted, onRejected, context) {
        DirectoryPermissionManager(
            context = context,
            onGranted = onGranted,
            onFailed = onRejected
        )
    }

    val launcher = createPersistablePermissionLauncher(
        onGranted = { _ ->
            state.success()
            state.step(success = true)
            state.fetch()
        },

        onFailure = {
            state.step(success = false)
        }
    )

    DisposableEffect(launcher, state) {
        state.launcher = launcher

        onDispose {
            state.launcher == null
        }
    }

    return state
}