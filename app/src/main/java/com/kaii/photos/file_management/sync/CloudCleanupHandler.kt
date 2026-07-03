package com.kaii.photos.file_management.sync

import androidx.compose.ui.util.fastMap
import com.kaii.photos.database.daos.MediaDao
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetBulkUploadCheckDto
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetBulkUploadCheckItem
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetUploadAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.uuid.ExperimentalUuidApi

class CloudCleanupHandler(
    private val mediaDao: MediaDao,
    private val assetsClient: AssetsClient
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend fun cleanUp() = withContext(Dispatchers.IO) {
        val cloudItems = mediaDao.getCloudMedia()
        if (cloudItems.isEmpty()) return@withContext

        // check what exists in cloud from what we have
        val bulkCheck = assetsClient.check(
            assets = AssetBulkUploadCheckDto(
                assets = cloudItems.fastMap { item ->
                    AssetBulkUploadCheckItem(
                        checksum = item.hash!!,
                        id = item.id.toString()
                    )
                }
            )
        )

        bulkCheck?.forEach { item ->
            // if it doesn't exist in cloud, remove it from local
            if (item.isTrashed || (item.assetId == null && item.action == AssetUploadAction.Accept)) {
                mediaDao.delete(item.id.toLong())
            }
        }
    }
}