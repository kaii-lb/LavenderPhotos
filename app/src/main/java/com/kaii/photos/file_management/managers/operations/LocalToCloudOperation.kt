package com.kaii.photos.file_management.managers.operations

import androidx.core.net.toUri
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.entities.CustomItem
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.database.entities.SyncOperation
import com.kaii.photos.database.getMediaFromMetadata
import com.kaii.photos.database.sync.SyncTaskRecorder
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FileOperationCopyResult
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.file_management.managers.gateways.MediaStoreGateway
import com.kaii.photos.helpers.calculateSha1Checksum
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetBulkUploadCheckDto
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetBulkUploadCheckItem
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetBulkUploadCheckResult
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetMediaCreateDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import java.io.File
import javax.inject.Inject
import kotlin.time.Instant
import kotlin.uuid.Uuid

class LocalToCloudOperation @Inject constructor(
    private val mediaDao: MediaDao,
    private val customDao: CustomEntityDao,
    private val gateway: MediaStoreGateway,
    private val assetsClient: AssetsClient,
    private val albumsClient: AlbumsClient,
    private val recorder: SyncTaskRecorder
) {
    fun execute(
        files: List<FileOperationItemMetadata>,
        destination: AlbumType.Cloud
    ): Flow<FileOperationProgress<List<FileOperationCopyResult>>> = channelFlow {
        send(FileOperationProgress.Started(action = FileOperationAction.LongOperationType.Copy, fileCount = files.size))

        if (files.isEmpty()) {
            send(FileOperationProgress.Finished(Result.Success(emptyList())))
            return@channelFlow
        }

        val media = mediaDao.getMediaFromMetadata(files)

        val results = recorder.record(
            operation = SyncOperation.Upload(
                destinationAlbumId = destination.immichId
            ),
            mediaIds = media.map { it.id },
            applyLocally = {
                customDao.upsertAll(
                    items = media.map {
                        CustomItem(
                            id = it.id,
                            album = destination.id
                        )
                    })

                media.map {
                    FileOperationCopyResult(
                        id = it.id,
                        immichId = it.immichId
                    )
                }
            },
            attemptRemote = {
                when (val result = uploadAndLink(files = files, destinationAlbumId = destination.immichId)) {
                    is Result.Success -> Result.Success(Unit)
                    is Result.Error -> Result.Error(result.error)
                }
            }
        )

        media.forEach {
            send(FileOperationProgress.ItemDone(uri = it.uri))
        }

        send(FileOperationProgress.Finished(Result.Success(results)))
    }.flowOn(Dispatchers.IO)

    suspend fun uploadAndLink(
        files: List<FileOperationItemMetadata>,
        destinationAlbumId: String
    ): Result<List<FileOperationCopyResult>, FileOperationError> = withContext(Dispatchers.IO) {
        val media = mediaDao.getMediaFromMetadata(files)

        if (media.isEmpty()) return@withContext Result.Success(emptyList())

        val hashes = media.associate {
            it.id to (it.hash ?: calculateSha1Checksum(File(it.absolutePath)))
        }

        val bulkCheck = assetsClient.check(
            assets = AssetBulkUploadCheckDto(
                assets = media.map {
                    AssetBulkUploadCheckItem(
                        checksum = hashes[it.id]!!,
                        id = it.id.toString()
                    )
                })
        )?.associateBy { it.id } ?: return@withContext Result.Error(FileOperationError.Failed)

        val semaphore = Semaphore(permits = 5)
        val trashedItems = mutableListOf<Uuid>()

        val total = media.map { mediaItem ->
            async {
                semaphore.withPermit {
                    uploadOrLinkItem(mediaItem, hashes, bulkCheck, trashedItems)
                }
            }
        }.awaitAll()

        assetsClient.restore(ids = trashedItems)

        val success = albumsClient.addAssets(
            albumId = Uuid.parse(destinationAlbumId),
            assetIds = total.mapNotNull { it.immichId }.map { Uuid.parse(it) }
        )

        if (success) Result.Success(total) else Result.Error(FileOperationError.Failed)
    }

    private suspend fun uploadOrLinkItem(
        mediaItem: MediaStoreData,
        hashes: Map<Long, String>,
        bulkCheck: Map<String, AssetBulkUploadCheckResult>,
        trashedItems: MutableList<Uuid>
    ): FileOperationCopyResult = withContext(Dispatchers.IO) {
        val bulkResponse = bulkCheck[mediaItem.id.toString()]

        if (bulkResponse?.assetId != null) {
            mediaDao.linkToImmich(
                id = mediaItem.id,
                hash = hashes[mediaItem.id]!!,
                immichUrl = mediaItem.immichUrl ?: "/api/assets/${bulkResponse.assetId}"
            )

            if (bulkResponse.isTrashed) {
                trashedItems.add(Uuid.parse(bulkResponse.assetId!!))
            }

            FileOperationCopyResult(
                id = mediaItem.id,
                immichId = mediaItem.immichId ?: bulkResponse.assetId
            )
        } else {
            val resp = assetsClient.upload(
                AssetMediaCreateDto(
                    assetSource = gateway.getAssetSource(mediaItem.uri.toUri()),
                    fileCreatedAt = Instant.fromEpochSeconds(mediaItem.dateTaken).format(DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET),
                    fileModifiedAt = Instant.fromEpochSeconds(mediaItem.dateModified).format(DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET),
                    metadata = emptyList(),
                    filename = mediaItem.displayName
                )
            )

            if (resp != null) {
                mediaDao.linkToImmich(
                    id = mediaItem.id,
                    hash = hashes[mediaItem.id]!!,
                    immichUrl = "/api/assets/${resp.id}/original"
                )
            }

            FileOperationCopyResult(
                id = mediaItem.id,
                immichId = resp?.id?.toString()
            )
        }
    }
}