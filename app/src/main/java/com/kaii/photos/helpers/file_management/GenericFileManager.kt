package com.kaii.photos.helpers.file_management

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.IntentSender
import android.provider.MediaStore
import android.provider.Settings
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import androidx.core.net.toUri
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.daos.SyncTaskDao
import com.kaii.photos.database.entities.CustomItem
import com.kaii.photos.database.entities.SyncTask
import com.kaii.photos.database.entities.SyncTaskStatus
import com.kaii.photos.database.entities.SyncTaskType
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.calculateSha1Checksum
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.toActivity
import com.kaii.photos.helpers.toBasePath
import com.kaii.photos.mediastore.MediaType
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
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface GenericFileManager {
    interface Action {
        data class Copy(
            val list: List<SelectionManager.SelectedItem>,
            val destination: AlbumType
        ) : Action

        data class Move(
            val list: List<SelectionManager.SelectedItem>,
            val destination: AlbumType
        ) : Action

        data class Trash(
            val list: List<SelectionManager.SelectedItem>,
            val trashed: Boolean
        ) : Action

        data class Delete(
            val list: List<SelectionManager.SelectedItem>
        ) : Action

        data class Favourite(
            val list: List<SelectionManager.SelectedItem>,
            val favourite: Boolean
        ) : Action

        data class RenameItem(
            val uri: String,
            val newName: String
        ) : Action

        data class RenameAlbum(
            val newName: String
        ) : Action
    }

    data class CopyResult(
        val id: Long,
        val immichId: String?
    )

    val mediaDao: MediaDao
    val customDao: CustomEntityDao
    val syncTaskDao: SyncTaskDao
    val assetClient: AssetsClient
    val albumsClient: AlbumsClient
    val info: ImmichBasicInfo

    fun allowedAlbumTypesFor(
        moving: Boolean,
        current: KClass<out AlbumType>
    ): List<KClass<out AlbumType>> {
        return if (!moving) {
            listOf(
                AlbumType.Folder::class,
                AlbumType.Custom::class,
                AlbumType.Cloud::class
            )
        } else {
            listOf(current)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun setFavourite(
        context: Context,
        favourite: Boolean,
        list: List<SelectionManager.SelectedItem>
    ): PendingIntent? =
        if (list.isEmpty()) null
        else MediaStore.createFavoriteRequest(
            context.contentResolver,
            list.fastMap { it.uri.toUri() },
            favourite
        )

    @OptIn(ExperimentalUuidApi::class)
    suspend fun setTrashed(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        trashed: Boolean,
        albumId: String?,
        onItemDone: (totaCount: Int) -> Unit
    )

    suspend fun permanentlyDelete(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ) {
        if (list.isNotEmpty()) {
            val deleteRequest = MediaStore.createDeleteRequest(
                context.contentResolver,
                list.map { it.uri.toUri() }
            )

            context.toActivity()?.startIntentSenderForResult(
                deleteRequest.intentSender,
                9997,
                null,
                0,
                0,
                0
            )
        }
    }

    fun renameItem(
        context: Context,
        uri: String,
        newName: String
    ): IntentSender?

    suspend fun renameAlbum(
        context: Context,
        album: AlbumType,
        newName: String
    )

    suspend fun moveItems(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: AlbumType,
        preserveDate: Boolean,
        onItemDone: (uri: String) -> Unit
    ): Boolean

    /** @param overrideDisplayName should not contain file extension */
    suspend fun copyItems(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: AlbumType,
        preserveDate: Boolean,
        overrideDisplayName: ((displayName: String) -> String)? = null,
        onItemDone: (uri: String) -> Unit
    ): List<CopyResult>

    suspend fun copyToCustom(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: AlbumType.Custom,
        onItemDone: (uri: String) -> Unit
    ): List<CopyResult> = withContext(Dispatchers.IO) {
        customDao.upsertAll(
            items = list.fastMap {
                CustomItem(
                    id = it.id,
                    album = destination.id
                )
            }
        )

        val ids = list.fastMap { it.id }
        val mediaItems = customDao.getMediaInAlbum(album = destination.id).filter {
            it.id in ids
        }

        mediaItems.forEach { onItemDone(it.uri) }

        return@withContext mediaItems.fastMap {
            CopyResult(
                id = it.id,
                immichId = it.immichId
            )
        }
    }

    /** @param onItemDone gives the uri of the current item copied */
    suspend fun copyToLocal(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: AlbumType.Folder,
        preserveDate: Boolean,
        overrideDisplayName: ((displayName: String) -> String)?,
        onItemDone: (uri: String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver

        val items = getMediaStoreDataForIds(
            ids = list.fastMap { it.id }.toSet(),
            context = context
        )

        val newItems = mutableListOf<CopyResult>()
        items.forEachIndexed { index, media ->
            destination.paths.forEach { path ->
                contentResolver.insertMedia(
                    context = context,
                    media = media,
                    destination = path,
                    basePath = media.absolutePath.toBasePath(),
                    overrideDisplayName = if (overrideDisplayName != null) overrideDisplayName(media.displayName) else null,
                    currentVolumes = MediaStore.getExternalVolumeNames(context),
                    preserveDate = preserveDate,
                    onInsert = { original, new ->
                        contentResolver.copyUriToUri(original, new)
                        newItems.add(
                            CopyResult(
                                id = new.lastPathSegment!!.toLong(),
                                immichId = media.immichId
                            )
                        )
                    }
                )
            }

            onItemDone(media.uri)
        }

        return@withContext newItems.toList()
    }

    /** @param destination is the immich album id */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun copyToCloud(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: AlbumType.Cloud,
        onItemDone: (uri: String) -> Unit
    ): List<CopyResult> = withContext(Dispatchers.IO) {
        val ids = list.fastMap { it.id }.toSet()
        val media = getMediaStoreDataForIds(
            ids = ids,
            context = context
        )

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

                onItemDone(item.uri)
            }

        val missing = assets.fastFilter { it.first.id.toString() !in exists }

        val taskId = syncTaskDao.insert(
            task = SyncTask(
                dateModified = Clock.System.now().epochSeconds,
                status = SyncTaskStatus.Processing,
                type = SyncTaskType.Upload,
                destination = destination.immichId,
                items = missing.fastMap {
                    SelectionManager.SelectedItem(
                        id = it.second.id,
                        uri = it.second.uri,
                        isImage = it.second.type == MediaType.Image,
                        parentPath = it.second.parentPath
                    )
                }
            )
        )

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
                    immichUrl = "${info.endpoint}/api/assets/${resp.id}/original"
                )

                onItemDone(media.first { it.id == item.second.id }.uri)
            }

            CopyResult(
                id = item.second.id,
                immichId = resp?.id
            )
        }

        syncTaskDao.updateTaskStatus(
            id = taskId.toInt(),
            status =
                if (missing.size == uploaded.size) SyncTaskStatus.Synced
                else SyncTaskStatus.Waiting
        )

        val total = media.mapNotNull { item ->
            CopyResult(
                id = item.id,
                immichId = item.immichId
            ).takeIf {
                it.immichId in exists && it.immichId != null
            }
        } + uploaded

        albumsClient.addAssets(
            albumId = Uuid.parse(destination.immichId),
            assetIds = total.fastMap { Uuid.parse(it.immichId!!) },
            accessToken = info.accessToken
        )

        return@withContext total
    }
}