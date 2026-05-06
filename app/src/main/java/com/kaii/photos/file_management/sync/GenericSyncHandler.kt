package com.kaii.photos.file_management.sync

import android.annotation.SuppressLint
import android.content.Context
import android.provider.MediaStore
import android.provider.Settings
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import androidx.core.net.toUri
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.preferences.SettingsAlbumsListImpl
import com.kaii.photos.file_management.managers.GenericFileManager
import com.kaii.photos.helpers.calculateSha1Checksum
import com.kaii.photos.mediastore.insertMedia
import com.kaii.photos.mediastore.setDateForMedia
import com.kaii.photos.mediastore.toContentId
import com.kaii.photos.mediastore.toMediaStoreData
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetBulkUploadCheckItem
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetBulkUploadRequest
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetResponse
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetUploadRequest
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.UploadAssetRequest
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
        val cloudMedia = cloudMedia.associateBy { it.id }
        val localMedia = localMedia.associateBy { it.immichId }

        val inner = cloudMedia.mapNotNull { (id, cloud) ->
            localMedia[id]?.let { local ->
                cloud.toMediaStoreData() to local
            }
        }

        val left = cloudMedia.filterKeys { key ->
            localMedia[key] == null
        }.keys.toMutableList()

        val right = localMedia.filterKeys { key ->
            cloudMedia[key] == null
        }.values.toMutableList()

        inner.forEach { (cloud, local) ->
            // very basic, perhaps should be less so
            if (local.dateModified > cloud.dateModified) right.add(local)
            else left.add(cloud.immichId!!)
        }

        uploadMedia(
            context = context,
            media = right,
            albumImmichId = originId
        )

        downloadMedia(
            context = context,
            ids = left,
            origin = originId,
            destination = destinationPath
        )
    }

    suspend fun fetchCloudAlbums() =
        albums.get()
            .first()
            .filter {
                it.immichId != null
            }

    suspend fun uploadMedia(
        context: Context,
        media: List<MediaStoreData>,
        albumImmichId: String
    ): Boolean {
        progressManager.addToTotalItems(count = media.size)

        val assets = media.map { item ->
            Pair(
                UploadAssetRequest(
                    absolutePath = item.absolutePath,
                    filename = item.displayName,
                    id = item.immichId?.let { Uuid.parse(it) } ?: Uuid.NIL,
                    size = item.size,
                    dateTaken = item.dateTaken,
                    dateModified = item.dateModified
                ),
                item
            )
        }

        val hashes = media.associate { item ->
            val hash = item.hash ?: calculateSha1Checksum(file = File(item.absolutePath))

            item.id to hash
        }

        val exists = fileManager.assetClient.check(
            assets = AssetBulkUploadRequest(
                assets.map { item ->
                    AssetBulkUploadCheckItem(
                        checksum = hashes[item.second.id]!!,
                        id = item.first.id.toString()
                    )
                }
            ),
            accessToken = fileManager.info.accessToken
        )?.map {
            it.id
        } ?: emptyList()

        media
            .filter {
                it.immichId in exists && it.immichId != null
            }
            .forEach { item ->
                progressManager.increaseProgress()

                fileManager.mediaDao.linkToImmich(
                    id = item.id,
                    hash = hashes[item.id]!!,
                    immichUrl = item.immichUrl!!
                )
            }

        val missing = assets.fastFilter { it.first.id.toString() !in exists }

        // this is okay because it is not being used to tracking purposes, only for identification to the immich server.
        @SuppressLint("HardwareIds")
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

        val uploaded = missing.fastMap { item ->
            val assetData = File(item.first.absolutePath).inputStream().buffered().readBytes()

            val resp = fileManager.assetClient.upload(
                AssetUploadRequest(
                    assetData = assetData,
                    deviceAssetId = "${item.first.filename}-${item.first.size}",
                    deviceId = deviceId,
                    fileCreatedAt = Instant.fromEpochSeconds(item.first.dateTaken).format(DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET),
                    fileModifiedAt = Instant.fromEpochSeconds(item.first.dateModified).format(DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET),
                    metadata = emptyList(),
                    filename = item.first.filename
                ),
                accessToken = fileManager.info.accessToken
            )

            if (resp != null) {
                progressManager.increaseProgress()
                fileManager.mediaDao.linkToImmich(
                    id = item.second.id,
                    hash = hashes[item.second.id]!!,
                    immichUrl = "/api/assets/${resp.id}/original"
                )
            }

            resp?.id
        }

        val total = media.mapNotNull { item ->
            item.immichId?.takeIf { it in exists }
        } + uploaded.filterNotNull()

        val success = fileManager.albumsClient.addAssets(
            albumId = Uuid.parse(albumImmichId),
            assetIds = total.fastMap { Uuid.parse(it) },
            accessToken = fileManager.info.accessToken
        )

        return total.size == media.size && success
    }

    suspend fun downloadMedia(
        context: Context,
        ids: List<String>,
        origin: String,
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

            // TODO: possibly check date modified too
            if (localItem?.hash == cloudItem.checksum) return@forEach

            val bytes = fileManager.assetClient.download(
                id = Uuid.parse(cloudItem.id),
                accessToken = fileManager.info.accessToken
            )

            if (bytes == null) return@forEach

            if (localItem == null) {
                val item = cloudItem.toMediaStoreData()
                val new = context.contentResolver.insertMedia(
                    context = context,
                    media = cloudItem.toMediaStoreData(),
                    destination = destination,
                    overrideDisplayName = null,
                    currentVolumes = MediaStore.getExternalVolumeNames(context),
                    preserveDate = true,
                    onInsert = { _, _ -> }
                )

                if (new != null) {
                    context.contentResolver.openOutputStream(new)?.use {
                        if (bytes.size <= 8 * 1024) it.write(bytes)
                        else it.buffered().write(bytes)

                        it.flush()
                        it.close()
                    }

                    context.contentResolver.setDateForMedia(
                        uri = new,
                        type = item.type,
                        dateTaken = item.dateTaken
                    )

                    new.toContentId(
                        contentResolver = context.contentResolver,
                        type = item.type
                    )?.let { newId ->
                        fileManager.mediaDao.linkToImmich(
                            id = newId,
                            hash = item.hash!!,
                            immichUrl = item.immichUrl!!
                        )
                    }

                    successes += 1
                }
            } else {
                context.contentResolver.openOutputStream(localItem.uri.toUri())?.use {
                    if (bytes.size <= 8 * 1024) it.write(bytes)
                    else it.buffered().write(bytes)

                    it.flush()
                    it.close()

                    successes += 1
                }
            }
        }

        return successes == ids.size
    }
}