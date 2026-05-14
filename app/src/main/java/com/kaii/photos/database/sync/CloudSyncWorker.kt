package com.kaii.photos.database.sync

import android.content.Context
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
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.di.appModule
import com.kaii.photos.file_management.managers.CloudFileManager
import com.kaii.photos.file_management.managers.CustomFileManager
import com.kaii.photos.file_management.managers.LocalFileManager
import com.kaii.photos.file_management.secure.LocalSecureManager
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.util.UUID
import java.util.concurrent.TimeUnit

class CloudSyncWorker(
    private val context: Context,
    private val params: WorkerParameters
) : CoroutineWorker(appContext = context, params = params) {
    companion object {
        const val ALBUM_ID = "ALBUM_ID"

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

        /** @param albumId if null syncs everything otherwise syncs that album only */
        fun immediateEnqueue(context: Context, albumId: String?): UUID {
            val request = OneTimeWorkRequest.Builder(CloudSyncWorker::class)
                .setConstraints(
                    Constraints(
                        requiredNetworkType = NetworkType.UNMETERED,
                        requiresBatteryNotLow = true,
                        requiresStorageNotLow = true
                    )
                )
                .apply {
                    setInputData(
                        Data.Builder()
                            .putString(ALBUM_ID, albumId)
                            .build()
                    )
                }
                .build()

            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(
                    uniqueWorkName = CloudSyncWorker::class.java.name + "-immediate",
                    existingWorkPolicy = ExistingWorkPolicy.APPEND_OR_REPLACE,
                    request = request
                )

            return request.id
        }
    }

    override suspend fun doWork(): Result {
        val db = MediaDatabase.getInstance(context.applicationContext)
        val info = context.appModule.settings.immich.getImmichBasicInfo().first()

        val assetsClient = AssetsClient(
            endpoint = info.endpoint,
            auth = info.auth,
            client = context.appModule.apiClient
        )
        val albumsClient = AlbumsClient(
            endpoint = info.endpoint,
            auth = info.auth,
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
                albumsClient = albumsClient
            ),
            localFileManager = LocalFileManager(
                mediaDao = db.mediaDao(),
                customDao = db.customDao(),
                syncTaskDao = db.taskDao(),
                assetClient = assetsClient,
                albumsClient = albumsClient,
                secureManager = LocalSecureManager(
                    secureDao = db.securedItemEntityDao()
                )
            ),
            customFileManager = CustomFileManager(
                mediaDao = db.mediaDao(),
                customDao = db.customDao(),
                syncTaskDao = db.taskDao(),
                assetClient = assetsClient,
                albumsClient = albumsClient,
                secureManager = LocalSecureManager(
                    secureDao = db.securedItemEntityDao()
                )
            )
        )

        delay(1000)

        val albumId = params.inputData.getString(ALBUM_ID)
        if (albumId != null) {
            manager.syncFor(albumId = albumId)
        } else {
            manager.syncUploads()
        }

        return Result.success()
    }
}