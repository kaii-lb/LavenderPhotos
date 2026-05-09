package com.kaii.photos.file_management.sync

import android.annotation.SuppressLint
import android.content.Context
import android.provider.MediaStore
import android.provider.Settings
import androidx.compose.ui.util.fastMap
import androidx.core.net.toUri
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.preferences.SettingsAlbumsListImpl
import com.kaii.photos.file_management.managers.CustomFileManager
import com.kaii.photos.file_management.managers.GenericFileManager
import com.kaii.photos.helpers.calculateSha1Checksum
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.insertMedia
import com.kaii.photos.mediastore.setDateForMedia
import com.kaii.photos.mediastore.toContentId
import com.kaii.photos.mediastore.toMediaStoreData
import io.github.kaii_lb.lavender.immichintegration.UriAssetSource
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetBulkUploadCheckItem
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetBulkUploadRequest
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetResponse
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetUploadRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import java.io.File
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
interface GenericSyncHandler {
    val fileManager: GenericFileManager
    val progressManager: ProgressManager
    val albums: SettingsAlbumsListImpl

    suspend fun sync(
        context: Context,
        cloudMedia: List<AssetResponse>,
        localMedia: List<MediaStoreData>,
        originId: String,
        destinationPath: String
    ) = withContext(Dispatchers.IO) {
        val cloudById = cloudMedia.associateBy { it.id }
        val cloudByHash = cloudMedia.associateBy { it.checksum }

        val toUpload = mutableListOf<MediaStoreData>()
        val toDownload = mutableListOf<String>()
        val toTrash = mutableListOf<MediaStoreData>()

        localMedia.forEach { local ->
            when {
                // item was deleted in the cloud
                local.immichId != null && !cloudById.containsKey(local.immichId) -> {
                    toTrash.add(local)
                }

                // item was locally added
                local.immichId == null -> {
                    val currentHash = local.hash ?: calculateSha1Checksum(File(local.absolutePath))
                    val cloudMatch = cloudByHash[currentHash]

                    if (cloudMatch != null) {
                        fileManager.mediaDao.linkToImmich(
                            id = local.id,
                            hash = currentHash,
                            immichUrl = "/api/assets/${cloudMatch.id}/original"
                        )
                    } else {
                        toUpload.add(local.copy(hash = currentHash))
                    }
                }
            }
        }

        val localHashes = localMedia.map { local ->
            val hash = local.hash ?: calculateSha1Checksum(File(local.absolutePath))

            fileManager.mediaDao.linkToHash(
                id = local.id,
                hash = hash
            )

            hash to local.immichId
        }.toSet()

        cloudMedia.forEach { cloud ->
            // check for id as well because setting media date might change checksum
            val check = !localHashes.any { (hash, id) ->
                hash == cloud.checksum || id == cloud.id
            }

            if (check) {
                toDownload.add(cloud.id)
            }
        }

        if (toUpload.isNotEmpty()) {
            uploadMedia(
                context = context,
                media = toUpload,
                albumImmichId = originId
            )
        }

        if (toDownload.isNotEmpty()) {
            downloadMedia(
                context = context,
                ids = toDownload,
                destination = destinationPath
            )
        }

        if (toTrash.isNotEmpty()) {
            albums.get().first().find { album ->
                album.immichId == originId
            }?.id?.let { albumId ->
                trashMedia(
                    context = context,
                    media = toTrash,
                    albumId = albumId
                )
            }
        }
    }

    suspend fun fetchCloudAlbums() =
        albums.get()
            .first()
            .filter {
                it.immichId?.isNotBlank() == true && (it is AlbumType.Folder || it is AlbumType.Custom)
            }

    private suspend fun uploadMedia(
        context: Context,
        media: List<MediaStoreData>,
        albumImmichId: String
    ): Boolean {
        progressManager.addToTotalItems(count = media.size)

        val hashes = media.associate { item ->
            val hash = item.hash ?: calculateSha1Checksum(file = File(item.absolutePath))

            item.id to hash
        }

        val bulkCheck = fileManager.assetClient.check(
            assets = AssetBulkUploadRequest(
                media.map { item ->
                    AssetBulkUploadCheckItem(
                        checksum = hashes[item.id]!!,
                        id = item.id.toString()
                    )
                }
            ),
            accessToken = fileManager.info.accessToken
        )?.associateBy { it.id } ?: return false

        val trashedItems = mutableListOf<Uuid>()

        // this is okay because it is not being used to tracking purposes, only for identification to the immich server.
        @SuppressLint("HardwareIds")
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

        val total = media.mapNotNull { mediaItem ->
            val bulkResponse = bulkCheck[mediaItem.id.toString()]

            if (bulkResponse?.assetId != null) {
                progressManager.increaseProgress()

                fileManager.mediaDao.linkToImmich(
                    id = mediaItem.id,
                    hash = hashes[mediaItem.id]!!,
                    immichUrl = "/api/assets/${bulkResponse.assetId!!}/original"
                )

                if (bulkResponse.isTrashed) {
                    trashedItems.add(Uuid.parse(bulkResponse.assetId!!))
                }

                bulkResponse.assetId
            } else {
                val resp = fileManager.assetClient.upload(
                    AssetUploadRequest(
                        assetSource = UriAssetSource(
                            context = context,
                            uri = mediaItem.uri.toUri()
                        ),
                        deviceAssetId = "${mediaItem.displayName}-${mediaItem.size}",
                        deviceId = deviceId,
                        fileCreatedAt = Instant.fromEpochSeconds(mediaItem.dateTaken).format(DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET),
                        fileModifiedAt = Instant.fromEpochSeconds(mediaItem.dateModified).format(DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET),
                        metadata = emptyList(),
                        filename = mediaItem.displayName
                    ),
                    accessToken = fileManager.info.accessToken
                )

                if (resp != null) {
                    progressManager.increaseProgress()
                    fileManager.mediaDao.linkToImmich(
                        id = mediaItem.id,
                        hash = hashes[mediaItem.id]!!,
                        immichUrl = "/api/assets/${resp.id}/original"
                    )
                }

                resp?.id
            }
        }

        fileManager.assetClient.restore(
            ids = trashedItems,
            accessToken = fileManager.info.accessToken
        )

        val success = fileManager.albumsClient.addAssets(
            albumId = Uuid.parse(albumImmichId),
            assetIds = total.fastMap { Uuid.parse(it) },
            accessToken = fileManager.info.accessToken
        )

        return total.size == media.size && success
    }

    private suspend fun downloadMedia(
        context: Context,
        ids: List<String>,
        destination: String
    ): Boolean {
        var successes = 0

        ids.forEach { id ->
            val cloudItem = fileManager.assetClient.get(
                id = Uuid.parse(id),
                accessToken = fileManager.info.accessToken
            ) ?: return@forEach

            val localItem = fileManager.mediaDao.getMediaFromHashes(
                hashes = listOf(cloudItem.checksum)
            ).firstOrNull()

            if (localItem != null) {
                successes += 1
                fileManager.mediaDao.linkToImmich(
                    id = localItem.id,
                    hash = cloudItem.checksum,
                    immichUrl = "/api/assets/${cloudItem.id}/original"
                )

                return@forEach
            }

            // Proceed with download only if content is truly missing
            val bytes = fileManager.assetClient.download(Uuid.parse(cloudItem.id), fileManager.info.accessToken) ?: return@forEach
            val item = cloudItem.toMediaStoreData()

            val newUri = context.contentResolver.insertMedia(
                context = context,
                media = item,
                destination = destination,
                currentVolumes = MediaStore.getExternalVolumeNames(context),
                preserveDate = true,
                onInsert = { _, _ -> }
            )

            newUri?.let { uri ->
                context.contentResolver.openOutputStream(uri)?.use {
                    if (bytes.size <= 8 * 1024) it.write(bytes)
                    else it.buffered().write(bytes)

                    it.flush()
                    it.close()
                }

                context.contentResolver.setDateForMedia(uri, item.type, item.dateTaken)

                uri.toContentId(context.contentResolver, item.type)?.let { newId ->
                    fileManager.mediaDao.upsertAll(listOf(item.copy(
                        id = newId,
                        uri = uri.toString(),
                        hash = cloudItem.checksum,
                        immichUrl = "/api/assets/${cloudItem.id}/original"
                    )))
                }

                successes += 1
            }
        }

        return successes == ids.size
    }

    private suspend fun trashMedia(
        context: Context,
        media: List<MediaStoreData>,
        albumId: String
    ) {
        fileManager.setTrashed(
            context = context,
            list = media.fastMap {
                SelectionManager.SelectedItem(
                    id = it.id,
                    uri = it.uri,
                    immichUrl = it.immichUrl,
                    isImage = it.type == MediaType.Image,
                    parentPath = it.parentPath
                )
            },
            trashed = true,
            albumId = albumId.takeIf {
                fileManager is CustomFileManager
            },
            onItemDone = {}
        )
    }
}