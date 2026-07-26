package com.kaii.photos.database.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kaii.photos.domain.immich.SyncOutcome
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.UUID
import java.util.concurrent.TimeUnit

@HiltWorker
class CloudSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncManager: CloudSyncManager
) : CoroutineWorker(context, params) {
    companion object {
        const val ALBUM_ID = "ALBUM_ID"

        fun enqueue(context: Context) {
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                CloudSyncWorker::class.java.name,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequest.Builder(CloudSyncWorker::class, 1, TimeUnit.HOURS)
                    .setConstraints(Constraints(requiredNetworkType = NetworkType.UNMETERED, requiresBatteryNotLow = true, requiresStorageNotLow = true))
                    .setBackoffCriteria(backoffDelay = 20, backoffPolicy = BackoffPolicy.EXPONENTIAL, timeUnit = TimeUnit.SECONDS)
                    .build()
            )
        }

        fun immediateEnqueue(context: Context, albumId: String?): UUID {
            val request = OneTimeWorkRequest.Builder(CloudSyncWorker::class)
                .setConstraints(Constraints(requiredNetworkType = NetworkType.UNMETERED, requiresBatteryNotLow = true, requiresStorageNotLow = true))
                .setInputData(Data.Builder().putString(ALBUM_ID, albumId).build())
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                CloudSyncWorker::class.java.name + "-immediate",
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request
            )
            return request.id
        }
    }

    override suspend fun doWork(): Result {
        val albumId = inputData.getString(ALBUM_ID)
        val outcome = if (albumId != null) syncManager.syncFor(albumId) else syncManager.syncUploads()

        return when (outcome) {
            SyncOutcome.Success -> Result.success()
            SyncOutcome.TransientFailure -> Result.retry()
            SyncOutcome.PermanentFailure -> Result.failure()
        }
    }
}