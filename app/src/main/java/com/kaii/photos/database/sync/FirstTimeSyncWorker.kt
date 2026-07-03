package com.kaii.photos.database.sync

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.room.concurrent.AtomicInt
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.kaii.photos.R
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.mediastore.chunkLoadMediaData
import com.kaii.photos.mediastore.getAllMediaStoreIds
import com.kaii.photos.mediastore.updateLatestGen
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "com.kaii.photos.database.sync.FirstTimeSyncWorker"

class FirstTimeSyncWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext = context, params = params) {
    companion object {
        const val PROGRESS = "PROGRESS"
        const val COUNT = "COUNT"

        fun start(context: Context): OneTimeWorkRequest {
            val request = OneTimeWorkRequest.Builder(FirstTimeSyncWorker::class.java).build()

            WorkManager.getInstance(context.applicationContext)
                .beginUniqueWork(
                    FirstTimeSyncWorker::class.java.name,
                    ExistingWorkPolicy.KEEP,
                    request
                )
                .enqueue()

            return request
        }

        suspend fun startWithSnackbar(context: Context) {
            val isLoading = mutableStateOf(true)

            LavenderSnackbarController.pushEvent(
                LavenderSnackbarEvent.LoadingEvent(
                    message = context.resources.getString(R.string.debugging_reset_scan_generation_loading),
                    icon = R.drawable.photogrid,
                    isLoading = isLoading
                )
            )

            val request = start(context)

            WorkManager.getInstance(context.applicationContext)
                .getWorkInfoByIdFlow(request.id)
                .collect { workInfo ->
                    isLoading.value = workInfo?.state != WorkInfo.State.SUCCEEDED
                }
        }
    }

    override suspend fun doWork(): Result {
        val startTime = Clock.System.now()
        val dao = MediaDatabase.getInstance(context).mediaDao()

        val mediaStoreIds = getAllMediaStoreIds(context)
        val inAppIds = dao.getAllMediaIds().toSet()

        val progress = AtomicInt(0)
        val added = mediaStoreIds - inAppIds

        setProgress(
            workDataOf(
                PROGRESS to 0f,
                COUNT to added.size
            )
        )

        if (added.isNotEmpty()) withContext(Dispatchers.IO) {
            val newGen = chunkLoadMediaData(
                ids = added,
                context = context,
                onProgress = { size ->
                    progress.addAndGet(size)
                },
                onLoadChunk = { chunk ->
                    setProgress(
                        workDataOf(
                            PROGRESS to progress.get().toFloat() / added.size,
                            COUNT to added.size
                        )
                    )

                    dao.insertAll(chunk)
                }
            )

            SyncManager(context).setGeneration(gen = newGen)
        }

        val removed = inAppIds - mediaStoreIds
        if (removed.isNotEmpty()) dao.deleteAll(removed)

        updateLatestGen(context)

        val endTime = Clock.System.now()

        Log.d(
            TAG,
            "First Time Sync Worker has finished running. Out of ${mediaStoreIds.size} items there was ${added.size} inserted and ${removed.size} removed. Total time was ${endTime - startTime}"
        )

        delay(1000.milliseconds)

        setProgress(
            workDataOf(
                PROGRESS to 1f,
                COUNT to added.size
            )
        )

        return Result.success(
            workDataOf(
                PROGRESS to 1f,
                COUNT to added.size
            )
        )
    }
}