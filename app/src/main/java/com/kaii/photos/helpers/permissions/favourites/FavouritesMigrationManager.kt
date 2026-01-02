package com.kaii.photos.helpers.permissions.favourites

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kaii.photos.database.MediaDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID

private const val TAG = "com.kaii.photos.helpers.permissions.favourites.FavouritesMigrationManager"

class FavouritesMigrationManager(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    companion object {
        /** number of items that was migrated successfully */
        const val SUCCESS_KEY = "SuccessKey"

        /** total number of items to be migrated */
        const val TOTAL_KEY = "TotalKey"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val appDatabase = MediaDatabase.getInstance(applicationContext).favouritedItemEntityDao()
            val items = appDatabase.getAll().first().map { it.uri.toUri() }

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_FAVORITE, 1)
            }

            val contentResolver = applicationContext.contentResolver
            val moved = items.filterIndexed { index, item ->
                contentResolver.update(item, contentValues, null) > 0
            }

            return@withContext Result.success(
                Data.Builder()
                    .putInt(SUCCESS_KEY, moved.size)
                    .putInt(TOTAL_KEY, items.size)
                    .build()
            )
        } catch (e: Throwable) {
            Log.d(TAG, e.message.toString())
            e.printStackTrace()

            return@withContext Result.failure()
        }
    }
}

class FavouritesMigrationSchedulingManager {
    fun scheduleTask(
        context: Context
    ): UUID {
        val uploadWorkRequest =
            OneTimeWorkRequestBuilder<FavouritesMigrationManager>()
                .build()

        WorkManager
            .getInstance(context)
            .enqueueUniqueWork(
                TAG,
                ExistingWorkPolicy.KEEP,
                uploadWorkRequest
            )

        return uploadWorkRequest.id
    }
}