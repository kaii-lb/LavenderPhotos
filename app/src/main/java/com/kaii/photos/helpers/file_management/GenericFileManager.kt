package com.kaii.photos.helpers.file_management

import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapIndexed
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.entities.CustomItem
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.toBasePath
import com.kaii.photos.mediastore.copyUriToUri
import com.kaii.photos.mediastore.getMediaStoreDataForIds
import com.kaii.photos.mediastore.insertMedia
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetBulkUploadCheckItem
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetBulkUploadRequest
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetUploadRequest
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.UploadAssetRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import java.io.File
import kotlin.reflect.KClass
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface GenericFileManager {
    enum class Action {
        Copy,
        Move
    }

    val customDao: CustomEntityDao
    val assetClient: AssetsClient
    val albumsClient: AlbumsClient
    val accessToken: String

    fun allowedAlbumTypesFor(
        action: Action,
        current: KClass<out AlbumType>
    ): List<KClass<out AlbumType>> {
        return if (action == Action.Copy) {
            listOf(
                AlbumType.Folder::class,
                AlbumType.Custom::class,
                AlbumType.Cloud::class
            )
        } else {
            listOf(current)
        }
    }

    suspend fun setFavourite(
        context: Context,
        favourite: Boolean,
        list: List<String>
    )

    suspend fun setTrashed(
        context: Context,
        list: List<String>,
        trashed: Boolean
    )

    fun renameItem(
        context: Context,
        uri: String,
        newName: String
    ): IntentSender?

    suspend fun renameDirectory(
        context: Context,
        path: String,
        newName: String,
    )

    suspend fun moveItems(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        origin: String,
        originType: KClass<out AlbumType>,
        destination: String,
        preserveDate: Boolean,
        onItemDone: (totalCount: Int) -> Unit
    ): Boolean

    /** @param overrideDisplayName should not contain file extension */
    suspend fun copyItems(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        origin: String,
        originType: KClass<out AlbumType>,
        destination: String,
        preserveDate: Boolean,
        overrideDisplayName: ((displayName: String) -> String)? = null,
        onItemDone: (totaCount: Int) -> Unit
    ): List<String>

    suspend fun copyToCustom(
        list: List<SelectionManager.SelectedItem>,
        destination: String,
        onItemDone: (totalCount: Int) -> Unit
    ): List<String> = withContext(Dispatchers.IO) {
        customDao.upsertAll(
            list.fastMapIndexed { index, item ->
                onItemDone(index + 1)
                CustomItem(
                    id = item.id,
                    album = destination
                )
            }
        )

        return@withContext list.fastMap { it.id.toString() }
    }

    suspend fun copyToLocal(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        origin: String,
        destination: String,
        preserveDate: Boolean,
        overrideDisplayName: ((displayName: String) -> String)?,
        onItemDone: (totalCount: Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver

        val items = getMediaStoreDataForIds(
            ids = list.fastMap { it.id }.toSet(),
            context = context
        )

        val newUris = mutableListOf<Uri>()
        items.forEachIndexed { index, media ->
            contentResolver.insertMedia(
                context = context,
                media = media,
                destination = destination,
                basePath = media.absolutePath.toBasePath(),
                overrideDisplayName = if (overrideDisplayName != null) overrideDisplayName(media.displayName) else null,
                currentVolumes = MediaStore.getExternalVolumeNames(context),
                preserveDate = preserveDate,
                onInsert = { original, new ->
                    contentResolver.copyUriToUri(original, new)
                    newUris.add(new)
                }
            )?.let {
                onItemDone(index + 1)
            }
        }

        return@withContext newUris.fastMap { it.lastPathSegment!! }
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun copyToCloud(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: String,
        onItemDone: (totalCount: Int) -> Unit
    ): List<String> = withContext(Dispatchers.IO) {
        val media = getMediaStoreDataForIds(
            ids = list.fastMap { it.id }.toSet(),
            context = context
        )

        val assets = media.map {
            UploadAssetRequest(
                absolutePath = it.absolutePath,
                filename = it.displayName,
                id = Uuid.parse(it.immichId ?: ""),
                size = it.size,
                dateTaken = it.dateTaken,
                dateModified = it.dateModified
            )
        }

        val exists = assetClient.check(
            assets = AssetBulkUploadRequest(
                assets.map { item ->
                    val checksum = item.filename // TODO

                    AssetBulkUploadCheckItem(
                        checksum = checksum,
                        id = item.id.toString()
                    )
                }
            ),
            accessToken = accessToken
        )?.map {
            it.id
        } ?: emptyList()

        onItemDone(exists.size)

        val missing = assets.fastFilter { it.id.toString() !in exists }

        val uploaded = missing.mapIndexedNotNull { index, item ->
            val assetData = File(item.absolutePath).inputStream().buffered().readBytes()

            val resp = assetClient.upload(
                AssetUploadRequest(
                    assetData = assetData,
                    deviceAssetId = "${item.filename}-${item.size}",
                    deviceId = Build.MODEL, // TODO: check if this is the right format
                    fileCreatedAt = Instant.fromEpochSeconds(item.dateTaken).format(DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET),
                    fileModifiedAt = Instant.fromEpochSeconds(item.dateModified).format(DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET),
                    metadata = emptyList(),
                    filename = item.filename
                ),
                accessToken = accessToken
            )

            if (resp != null) {
                onItemDone(exists.size + index + 1)
            }

            resp?.id
        }

        val total = exists + uploaded
        albumsClient.addAssets(
            albumId = Uuid.parse(destination),
            assetIds = total.fastMap { Uuid.parse(it) },
            accessToken = accessToken
        )

        return@withContext total
    }
}