package com.kaii.photos.file_management.managers.operations

import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.file_management.managers.gateways.CloudCacheGateway
import com.kaii.photos.helpers.calculateSha1Checksum
import io.github.kaii_lb.lavender.immichintegration.FileWriteChannel
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.uuid.Uuid

class CloudResolveShareableItemOperation @Inject constructor(
    private val assetClient: AssetsClient,
    private val cacheGateway: CloudCacheGateway
) {
    suspend fun execute(
        item: FileOperationItemMetadata,
        fileName: String
    ): FileOperationItemMetadata? = withContext(Dispatchers.IO) {
        val file = cacheGateway.cacheFile(fileName)
        val checksumOriginal = if (file.exists()) calculateSha1Checksum(file = file) else null
        val checksumCloud = assetClient.get(id = Uuid.parse(item.immichId!!))?.checksum
        val uri = cacheGateway.shareableUri(file)

        if (checksumOriginal != null && checksumCloud == checksumOriginal) {
            item.copy(uri = uri.toString())
        } else {
            val downloaded = assetClient.download(
                id = Uuid.parse(item.immichId!!),
                channel = FileWriteChannel(file = file)
            )

            if (downloaded) item.copy(uri = uri.toString()) else { file.delete(); null }
        }
    }
}