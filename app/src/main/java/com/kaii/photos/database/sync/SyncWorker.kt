package com.kaii.photos.database.sync

import android.content.Context
import android.provider.MediaStore
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.mediastore.getAllMediaStoreIds
import com.kaii.photos.mediastore.loadMediaDataDelta
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.time.Clock

private const val TAG = "com.kaii.photos.database.sync.SyncWorker"

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(appContext = context, params = params) {
    companion object {
        fun start(
            context: Context,
            workPolicy: ExistingWorkPolicy = ExistingWorkPolicy.REPLACE
        ) {
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(
                    SyncWorker::class.java.name,
                    workPolicy,
                    OneTimeWorkRequest.Builder(SyncWorker::class).build()
                )
        }
    }

    override suspend fun doWork(): Result {
        val startTime = Clock.System.now()

        val db = MediaDatabase.getInstance(context)
        val dao = db.mediaDao()
        val syncManager = SyncManager(context)

        val volumesBefore = MediaStore.getExternalVolumeNames(context)
        val mediaStoreIds = getAllMediaStoreIds(context) ?: return Result.retry()

        if (MediaStore.getExternalVolumeNames(context) != volumesBefore) return Result.retry()

        val inAppIds = dao.getAllMediaIds().toSet()

        val removed = RemovalGuard.filterSafeRemovals(
            context = context,
            dao = dao,
            candidates = (inAppIds - mediaStoreIds).filter { it >= 0 }
        )
        val (added, newGens) = loadMediaDataDelta(context = context)

        db.withTransaction {
            if (removed.isNotEmpty()) {
                removed.chunked(500).forEach { chunk ->
                    dao.deleteAll(chunk)
                }
            }

            if (added.isNotEmpty()) dao.upsertIgnoringImmich(items = added)
        }

        newGens.forEach { (volume, gen) ->
            syncManager.setVolumeGeneration(volumeName = volume, gen = gen)
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