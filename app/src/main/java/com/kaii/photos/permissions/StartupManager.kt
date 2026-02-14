package com.kaii.photos.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.sync.FirstTimeSyncWorker
import com.kaii.photos.database.sync.SyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

private const val TAG = "com.kaii.photos.permissions.StartupState"

class StartupManager(
    private val context: Context
) {
    enum class State {
        MissingPermissions,
        NeedsIndexing,
        Successful
    }

    private var launchedFirstTimSyncWorker = false

    // READ_MEDIA_VIDEO isn't necessary as its bundled with READ_MEDIA_IMAGES
    private val permList =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.MANAGE_MEDIA
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.MANAGE_MEDIA
            )
        } else {
            listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }

    private val permissionQueue = mutableStateListOf<String>()

    private val _state = MutableStateFlow(State.MissingPermissions)
    val state = _state.asStateFlow()

    init {
        updatePermissionState()
    }

    private fun updatePermissionState() {
        permList.forEach { perm ->
            val granted = when (perm) {
                Manifest.permission.MANAGE_EXTERNAL_STORAGE -> {
                    Environment.isExternalStorageManager()
                }

                Manifest.permission.MANAGE_MEDIA -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaStore.canManageMedia(context)
                    else false
                }

                else -> {
                    context.checkSelfPermission(
                        perm
                    ) == PackageManager.PERMISSION_GRANTED
                }
            }

            if (!granted && !permissionQueue.contains(perm)) permissionQueue.add(perm)
            else permissionQueue.remove(perm)

            Log.d(TAG, "Permission $perm has been granted $granted")
        }
    }

    fun permissionGranted(permission: String) = !permissionQueue.contains(permission)

    fun onPermissionResult(
        permission: String,
        isGranted: Boolean
    ) {
        if (!isGranted && !permissionQueue.contains(permission)) permissionQueue.add(permission)
        else if (isGranted) permissionQueue.remove(permission)

        permissionQueue.forEach { Log.d(TAG, "PERMISSION DENIED $it") }
    }

    fun checkPermissions(): Boolean {
        val manageMedia = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                permissionQueue.all { it == Manifest.permission.MANAGE_MEDIA }

        return permissionQueue.isEmpty() || manageMedia
    }

    suspend fun checkState() = withContext(Dispatchers.IO) {
        val permsGranted = checkPermissions()
        val needsIndexing = MediaDatabase.getInstance(context).mediaDao().isEmpty()

        when {
            permsGranted && !needsIndexing -> {
                WorkManager.getInstance(context)
                    .enqueueUniqueWork(
                        SyncWorker::class.java.name,
                        ExistingWorkPolicy.REPLACE,
                        OneTimeWorkRequest.Builder(SyncWorker::class).build()
                    )

                _state.value = State.Successful
            }

            permsGranted && needsIndexing -> _state.value = State.NeedsIndexing

            else -> _state.value = State.MissingPermissions
        }
    }

    suspend fun launchFirstTimeSyncWorker(
        onProgress: (itemCount: Int, progress: Float) -> Unit
    ) {
        if (launchedFirstTimSyncWorker) return
        launchedFirstTimSyncWorker = true

        val request = OneTimeWorkRequest.Builder(FirstTimeSyncWorker::class.java).build()
        WorkManager.getInstance(context.applicationContext)
            .beginUniqueWork(
                FirstTimeSyncWorker::class.java.name,
                ExistingWorkPolicy.REPLACE,
                request
            )
            .enqueue()

        var itemCount = -1
        var progress = 0f

        WorkManager.getInstance(context.applicationContext)
            .getWorkInfoByIdFlow(request.id)
            .collect { workInfo ->
                if (workInfo != null) {
                    workInfo.progress.getInt(FirstTimeSyncWorker.COUNT, -1).let {
                        if (it != -1) itemCount = it
                    }

                    workInfo.progress.getFloat(FirstTimeSyncWorker.PROGRESS, -1f).let {
                        if (it != -1f) progress = it
                    }

                    onProgress(itemCount, progress)
                }
            }
    }
}