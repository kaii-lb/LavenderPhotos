package com.kaii.photos.file_management.sync.operations

import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.file_management.managers.gateways.MediaStoreGateway
import com.kaii.photos.file_management.sync.ProgressManager
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.toMediaStoreData
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.uuid.Uuid

class DownloadMediaOperation(
    private val gateway: MediaStoreGateway,
    private val mediaDao: MediaDao,
    private val assetsClient: AssetsClient,
    private val progressManager: ProgressManager
) {
    suspend fun execute(
        ids: List<Uuid>,
        destination: String
    ): Result<Unit, FileOperationError> {
        if (ids.isEmpty()) return Result.Error(FileOperationError.Failed)
        val semaphore = Semaphore(permits = 5)

        return coroutineScope {
            ids.map { id ->
                async {
                    semaphore.withPermit {
                        downloadOne(id, destination)
                    }
                }
            }.awaitAll().all { it }
        }.let {
            if (it) Result.Success(Unit)
            else Result.Error(FileOperationError.Failed)
        }
    }

    private suspend fun downloadOne(id: Uuid, destination: String): Boolean {
        val cloudItem = assetsClient.get(id = id) ?: return false
        val localMatches = mediaDao.getMediaFromHashes(
            hashes = listOf(cloudItem.checksum)
        ).filter { it.parentPath == destination }

        if (localMatches.isNotEmpty()) {
            localMatches.forEach {
                mediaDao.linkToImmich(
                    id = it.id,
                    hash = cloudItem.checksum,
                    immichUrl = "/api/assets/${cloudItem.id}/original"
                )
            }

            progressManager.increaseProgress()
            return true
        }

        val item = cloudItem.toMediaStoreData()
        val inserted = when (val result = gateway.insertMedia(item, destination)) {
            is Result.Success -> result.data
            is Result.Error -> return false
        }

        val downloaded = assetsClient.download(
            id = Uuid.parse(cloudItem.id),
            channel = gateway.getWriteChannel(uri = inserted)
        )

        if (!downloaded) {
            gateway.delete(
                files = listOf(
                    FileOperationItemMetadata(
                        id = item.id,
                        uri = inserted.toString(),
                        absolutePath = item.absolutePath,
                        isImage = item.type == MediaType.Image,
                        immichUrl = item.immichUrl
                    )
                )
            )
            return false
        }

        when (val result = gateway.getContentId(uri = inserted, type = item.type)) {
            is Result.Error -> return false

            is Result.Success -> {
                gateway.setDateForMedia(inserted, item.dateTaken, item.type)
                mediaDao.linkToImmich(
                    id = result.data,
                    hash = cloudItem.checksum,
                    immichUrl = "/api/assets/${cloudItem.id}/original"
                )
                progressManager.increaseProgress()
                return true
            }
        }
    }
}