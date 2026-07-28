package com.kaii.photos.file_management.sync.operations

import androidx.core.net.toUri
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.file_management.managers.gateways.MediaStoreGateway
import com.kaii.photos.file_management.sync.ProgressManager
import com.kaii.photos.helpers.calculateSha1Checksum
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetBulkUploadCheckDto
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetBulkUploadCheckItem
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetMediaCreateDto
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import java.io.File
import javax.inject.Inject
import kotlin.time.Instant
import kotlin.uuid.Uuid

class UploadMediaOperation @Inject constructor(
    private val gateway: MediaStoreGateway,
    private val mediaDao: MediaDao,
    private val assetsClient: AssetsClient,
    private val albumsClient: AlbumsClient,
    private val progressManager: ProgressManager
) {
    suspend fun execute(
        media: List<MediaStoreData>, albumImmichId: String
    ): Result<Unit, FileOperationError> {
        if (media.isEmpty()) return Result.Success(Unit)

        val hashes = media.associate { it.id to (it.hash ?: calculateSha1Checksum(File(it.absolutePath))) }
        val bulkCheck = assetsClient.check(
            AssetBulkUploadCheckDto(
                assets = media.map {
                    AssetBulkUploadCheckItem(
                        checksum = hashes[it.id]!!,
                        id = it.id.toString()
                    )
                }
            )
        )?.associateBy { it.id } ?: return Result.Error(FileOperationError.Failed)

        val semaphore = Semaphore(permits = 5)
        val trashedItems = mutableListOf<Uuid>()

        val uploadedIds = coroutineScope {
            media.map { mediaItem ->
                async {
                    semaphore.withPermit {
                        val bulkResponse = bulkCheck[mediaItem.id.toString()]
                        val assetId = bulkResponse?.assetId ?: assetsClient.upload(
                            AssetMediaCreateDto(
                                assetSource = gateway.getAssetSource(uri = mediaItem.uri.toUri()),
                                fileCreatedAt = Instant.fromEpochSeconds(mediaItem.dateTaken).format(DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET),
                                fileModifiedAt = Instant.fromEpochSeconds(mediaItem.dateModified).format(DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET),
                                metadata = emptyList(),
                                filename = mediaItem.displayName
                            )
                        )?.id?.toString()

                        if (bulkResponse?.isTrashed == true) trashedItems.add(Uuid.parse(bulkResponse.assetId!!))

                        assetId?.let {
                            mediaDao.linkToImmich(id = mediaItem.id, hash = hashes[mediaItem.id]!!, immichUrl = "/api/assets/$it/original")
                            progressManager.increaseProgress()
                            Uuid.parse(it)
                        }
                    }
                }
            }.awaitAll().filterNotNull()
        }

        val restoreSuccess = assetsClient.restore(ids = trashedItems) != null
        val addAssetSuccess = albumsClient.addAssets(albumId = Uuid.parse(albumImmichId), assetIds = uploadedIds)
        val success = uploadedIds.size == media.size && addAssetSuccess && restoreSuccess

        return if (success) Result.Success(Unit)
        else Result.Error(FileOperationError.Failed)
    }
}