package com.kaii.photos.database.sync

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.mediastore.getAllMediaStoreIds
import com.kaii.photos.mediastore.loadMediaDataDelta
import kotlin.time.Clock

private const val TAG = "com.kaii.photos.database.sync.SyncWorker"

class SyncWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext = context, params = params) {
    override suspend fun doWork(): Result {
        val startTime = Clock.System.now()

        val db = MediaDatabase.getInstance(context)
        val dao = db.mediaDao()

        val mediaStoreIds = getAllMediaStoreIds(context)
        val inAppIds = dao.getAllMediaIds().toSet()

        val removed = inAppIds - mediaStoreIds
        val added = loadMediaDataDelta(context = context)
        db.withTransaction {
            if (removed.isNotEmpty()) dao.deleteAll(removed)

            if (added.isNotEmpty()) dao.upsertAll(items = added)
        }

        val endTime = Clock.System.now()

        Log.d(
            TAG,
            "Sync worker has finished running. " +
                    "Out of ${mediaStoreIds.size} items there was ${added.size} inserted and ${removed.size} removed. " +
                    "Total time was ${endTime - startTime}"
        )

        return Result.success()
    }
}