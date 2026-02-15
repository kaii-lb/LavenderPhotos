package com.kaii.photos.database.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.mediastore.chunkLoadMediaData
import com.kaii.photos.mediastore.getAllFavouritedMediaStoreIds
import com.kaii.photos.mediastore.getAllMediaStoreIds
import kotlin.time.Clock

private const val TAG = "com.kaii.photos.database.sync.SyncWorker"

class SyncWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext = context, params = params) {
    override suspend fun doWork(): Result {
        val startTime = Clock.System.now()
        val dao = MediaDatabase.getInstance(context).mediaDao()

        val mediaStoreIds = getAllMediaStoreIds(context)
        val inAppIds = dao.getAllMediaIds().toSet()

        val added = mediaStoreIds - inAppIds
        if (added.isNotEmpty()) {
            chunkLoadMediaData(added, context) { chunk ->
                dao.insertAll(chunk)
            }
        }

        val removed = inAppIds - mediaStoreIds
        if (removed.isNotEmpty()) dao.deleteAll(removed)

        val inAppFavIds = dao.getFavouritedIds().toSet()
        val favouritedIds = getAllFavouritedMediaStoreIds(context)
        val favAdded = favouritedIds - inAppFavIds
        if (favAdded.isNotEmpty()) {
            dao.setFavouriteOnMedia(favAdded, true)
        }

        val favRemoved = inAppFavIds - favouritedIds
        if (favRemoved.isNotEmpty()) dao.setFavouriteOnMedia(favRemoved, false)

        val endTime = Clock.System.now()

        Log.d(
            TAG,
            "Sync worker has finished running. " +
                    "Out of ${mediaStoreIds.size} items there was ${added.size} inserted and ${removed.size} removed. " +
                    "There was ${favAdded.size} favourites added and ${favRemoved.size} favourites removed. " +
                    "Total time was ${endTime - startTime}"
        )

        return Result.success()
    }
}