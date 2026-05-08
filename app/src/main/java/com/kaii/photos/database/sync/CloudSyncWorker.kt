package com.kaii.photos.database.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.di.appModule
import com.kaii.photos.file_management.managers.CloudFileManager
import com.kaii.photos.file_management.managers.CustomFileManager
import com.kaii.photos.file_management.managers.LocalFileManager
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class CloudSyncWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext = context, params = params) {
    companion object {
        fun enqueue(context: Context) {
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniquePeriodicWork(
                    CloudSyncWorker::class.java.name,
                    ExistingPeriodicWorkPolicy.KEEP,
                    PeriodicWorkRequest
                        .Builder(CloudSyncWorker::class, 1, TimeUnit.HOURS)
                        .setConstraints(
                            Constraints(
                                requiredNetworkType = NetworkType.UNMETERED,
                                requiresBatteryNotLow = true,
                                requiresStorageNotLow = true
                            )
                        )
                        .setBackoffCriteria(
                            backoffDelay = 20,
                            backoffPolicy = BackoffPolicy.EXPONENTIAL,
                            timeUnit = TimeUnit.SECONDS
                        )
                        .build()
                )
        }

        fun immediateEnqueue(context: Context) {
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(
                    uniqueWorkName = CloudSyncWorker::class.java.name + "-immediate",
                    existingWorkPolicy = ExistingWorkPolicy.APPEND_OR_REPLACE,
                    request = OneTimeWorkRequest.Builder(CloudSyncWorker::class)
                        .setConstraints(
                            Constraints(
                                requiredNetworkType = NetworkType.UNMETERED,
                                requiresBatteryNotLow = true,
                                requiresStorageNotLow = true
                            )
                        )
                        .build()
                )
        }
    }

    override suspend fun doWork(): Result {
        val db = MediaDatabase.getInstance(context.applicationContext)
        val info = context.appModule.settings.immich.getImmichBasicInfo().first()

        val assetsClient = AssetsClient(
            baseUrl = info.endpoint,
            client = context.appModule.apiClient
        )
        val albumsClient = AlbumsClient(
            baseUrl = info.endpoint,
            client = context.appModule.apiClient
        )

        val manager = CloudSyncManager(
            taskDao = db.taskDao(),
            context = context.applicationContext,
            cloudFileManager = CloudFileManager(
                mediaDao = db.mediaDao(),
                customDao = db.customDao(),
                syncTaskDao = db.taskDao(),
                assetClient = assetsClient,
                albumsClient = albumsClient,
                info = info
            ),
            localFileManager = LocalFileManager(
                mediaDao = db.mediaDao(),
                customDao = db.customDao(),
                syncTaskDao = db.taskDao(),
                assetClient = assetsClient,
                albumsClient = albumsClient,
                info = info
            ),
            customFileManager = CustomFileManager(
                mediaDao = db.mediaDao(),
                customDao = db.customDao(),
                syncTaskDao = db.taskDao(),
                assetClient = assetsClient,
                albumsClient = albumsClient,
                info = info
            )
        )

        delay(1000)

        manager.syncUploads()

        return Result.success()
    }
}