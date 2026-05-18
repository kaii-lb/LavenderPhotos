package com.kaii.photos.file_management.sync

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import com.kaii.photos.database.daos.MediaDao
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetExistsRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.uuid.ExperimentalUuidApi

class CloudCleanupHandler(
    private val mediaDao: MediaDao,
    private val assetsClient: AssetsClient
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend fun cleanUp(context: Context) = withContext(Dispatchers.IO) {
        val cloudItems = mediaDao.getCloudMedia().filter {
            it.isCloud // just to be sure we don't delete user's local media
        }

        if (cloudItems.isEmpty()) return@withContext

        // this is okay because it is not being used to tracking purposes, only for identification to the immich server.
        @SuppressLint("HardwareIds")
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

        cloudItems.forEach { item ->
            val exists = assetsClient.exists(
                request = AssetExistsRequest(
                    deviceAssetIds = listOf(
                        "${deviceId}-${item.displayName}-${item.size}"
                    ),
                    deviceId = deviceId
                )
            )?.existingIds?.isEmpty() == true

            if (!exists) {
                mediaDao.delete(item.id)
            }
        }
    }
}