package com.kaii.photos.file_management.sync

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.datastore.preferences.SettingsAlbumsListImpl
import com.kaii.photos.helpers.calculateSha1Checksum
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import io.github.kaii_lb.lavender.immichintegration.serialization.albums.AlbumCreationInfo
import io.github.kaii_lb.lavender.immichintegration.serialization.albums.AlbumCreationState
import io.github.kaii_lb.lavender.immichintegration.serialization.albums.AlbumUserCreationInfo
import io.github.kaii_lb.lavender.immichintegration.serialization.albums.AlbumUserRole
import io.github.kaii_lb.lavender.immichintegration.serialization.albums.AlbumsGetAllState
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetBulkUploadCheckItem
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetBulkUploadRequest
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetUploadRequest
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.UploadAssetRequest
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import java.io.File
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
interface GenericSyncHandler {
    val mediaDao: MediaDao
    val customDao: CustomEntityDao
    val assetClient: AssetsClient
    val albumsClient: AlbumsClient
    val info: ImmichBasicInfo
    val albums: SettingsAlbumsListImpl

    suspend fun download(
        context: Context,
        id: String
    )

    suspend fun upload(
        context: Context,
        id: String
    ): Boolean

    suspend fun getAlbumId(
        album: AlbumType
    ): String? {
        val existingImmichAlbums = albumsClient.getAll(accessToken = info.accessToken)

        if (existingImmichAlbums is AlbumsGetAllState.Failed) return null
        existingImmichAlbums as AlbumsGetAllState.Retrieved

        var immichId = album.immichId
        val exists = existingImmichAlbums.albums.find {
            it.id == album.immichId
        } != null

        if (!exists) {
            val albumCreationState = albumsClient.createAlbum(
                info = AlbumCreationInfo(
                    albumName = album.name,
                    albumUsers = AlbumUserCreationInfo(
                        role = AlbumUserRole.Editor,
                        userId = Uuid.parse(info.userId)
                    ),
                    assetIds = emptyList(),
                    description = ""
                ),
                accessToken = info.accessToken
            )

            if (albumCreationState is AlbumCreationState.Failed) return null
            immichId = (albumCreationState as AlbumCreationState.Created).album.id
        }

        return immichId
    }

    suspend fun uploadMedia(
        context: Context,
        media: List<MediaStoreData>,
        albumImmichId: String
    ): Boolean {
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

        val exists = assetClient.check(
            assets = AssetBulkUploadRequest(
                assets.map { item ->
                    AssetBulkUploadCheckItem(
                        checksum = hashes[item.second.id]!!,
                        id = item.first.id.toString()
                    )
                }
            ),
            accessToken = info.accessToken
        )?.map {
            it.id
        } ?: emptyList()

        media
            .filter {
                it.immichId in exists && it.immichId != null
            }
            .forEach { item ->
                mediaDao.linkToImmich(
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

            val resp = assetClient.upload(
                AssetUploadRequest(
                    assetData = assetData,
                    deviceAssetId = "${item.first.filename}-${item.first.size}",
                    deviceId = deviceId,
                    fileCreatedAt = Instant.fromEpochSeconds(item.first.dateTaken).format(DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET),
                    fileModifiedAt = Instant.fromEpochSeconds(item.first.dateModified).format(DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET),
                    metadata = emptyList(),
                    filename = item.first.filename
                ),
                accessToken = info.accessToken
            )

            if (resp != null) {
                mediaDao.linkToImmich(
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

        val success = albumsClient.addAssets(
            albumId = Uuid.parse(albumImmichId),
            assetIds = total.fastMap { Uuid.parse(it) },
            accessToken = info.accessToken
        )

        return total.size == media.size && success
    }
}