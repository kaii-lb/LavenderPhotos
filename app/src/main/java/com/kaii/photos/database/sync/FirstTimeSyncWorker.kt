package com.kaii.photos.database.sync

import android.content.Context
import android.util.Log
import androidx.room.concurrent.AtomicInt
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.mediastore.chunkLoadMediaData
import com.kaii.photos.mediastore.getAllMediaStoreIds
import kotlin.time.Clock

private const val TAG = "com.kaii.photos.database.sync.FirstTimeSyncWorker"

class FirstTimeSyncWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext = context, params = params) {
    companion object {
        const val PROGRESS = "PROGRESS"
        const val COUNT = "COUNT"
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

        if (added.isNotEmpty()) {
            chunkLoadMediaData(
                ids = added,
                context = context,
                onProgress = { size ->
                    progress.set(progress.get() + size)
                },
                onLoadChunk = { chunk ->
                    setProgressAsync(
                        workDataOf(
                            PROGRESS to progress.get().toFloat() / added.size,
                            COUNT to added.size
                        )
                    )

                    dao.insertAll(chunk)
                }
            )
        }

        val removed = inAppIds - mediaStoreIds
        if (removed.isNotEmpty()) dao.deleteAll(removed)

        val endTime = Clock.System.now()

        Log.d(
            TAG,
            "First Time Sync Worker has finished running. Out of ${mediaStoreIds.size} items there was ${added.size} inserted and ${removed.size} removed. Total time was ${endTime - startTime}"
        )

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