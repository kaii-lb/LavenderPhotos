package com.kaii.photos.permissions.files

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
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

    private fun getDirPermission(directory: String) {
        val launcher = launcher

        if (launcher == null) {
            Log.e(DirectoryPermissionManager::class.qualifiedName, "getDirPermission: launcher is null, aborting")
            running = false
            onFailed()
            return
        }

        val uri = context.getExternalStorageContentUriFromAbsolutePath(absolutePath = directory, trimDoc = false)
        launcher.launch(uri)
    }

    private fun fetch() {
        if (!running) return

        val next = directories.firstOrNull() ?: run {
            running = false
            onGranted()
            return
        }

        getDirPermission(directory = next)
    }

    internal fun onLauncherResult(success: Boolean) {
        if (!running || directories.isEmpty()) {
            running = false
            onFailed()
            return
        }

        if (!success) {
            running = false
            onFailed()
            return
        }

        directories.removeAt(0)

        if (directories.isEmpty()) {
            running = false
            onGranted()
        } else {
            fetch()
        }
    }

    fun start(directories: Set<String>) {
        if (running) throw IllegalStateException("Cannot get directory permissions while another permission request is running!")

        // cloud/custom albums
        if (directories.isEmpty()) {
            running = false
            onGranted()
            return
        }

        if (directories.all { it.isBlank() }) throw IllegalArgumentException("Cannot get directory permissions for directories with blank paths!")

        val directories = directories.filter { it.isNotEmpty() }

        val previous = context.contentResolver.persistedUriPermissions
        val persisted = directories.all { path ->
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
        onGranted = { _ -> state.onLauncherResult(success = true) },
        onFailure = { state.onLauncherResult(success = false) }
    )

    DisposableEffect(launcher, state) {
        state.launcher = launcher
        onDispose { state.launcher = null }
    }

    return state
}