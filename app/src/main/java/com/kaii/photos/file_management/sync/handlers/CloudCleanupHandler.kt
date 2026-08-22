package com.kaii.photos.file_management.sync.handlers

import com.kaii.photos.database.daos.MediaDao
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetBulkUploadCheckDto
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetBulkUploadCheckItem
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetUploadAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.uuid.ExperimentalUuidApi

class CloudCleanupHandler @Inject constructor(
    private val mediaDao: MediaDao,
    private val assetsClient: AssetsClient
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend fun cleanUp() = withContext(Dispatchers.IO) {
        val cloudItems = mediaDao.getCloudMedia()
        if (cloudItems.isEmpty()) return@withContext

        val hashed = cloudItems.filter { it.hash != null }
        val bulkCheck = assetsClient.check(
            AssetBulkUploadCheckDto(hashed.map { AssetBulkUploadCheckItem(checksum = it.hash!!, id = it.id.toString()) })
        ) ?: return@withContext

        val staleIds = bulkCheck
            .filter { it.isTrashed || (it.assetId == null && it.action == AssetUploadAction.Accept) }
            .map { it.id.toLong() }

        if (staleIds.isNotEmpty()) {
            staleIds.chunked(500).forEach { chunk ->
                mediaDao.deleteAll(ids = chunk)
            }
        }
    }
}