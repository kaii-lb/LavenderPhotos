package com.kaii.photos.helpers.file_management

import android.content.Context
import android.content.IntentSender
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.util.fastMap
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.daos.SyncTaskDao
import com.kaii.photos.database.entities.CustomItem
import com.kaii.photos.database.entities.SyncTask
import com.kaii.photos.database.entities.SyncTaskStatus
import com.kaii.photos.database.entities.SyncTaskType
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.di.appModule
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.toBasePath
import com.kaii.photos.mediastore.getUriFromAbsolutePath
import com.kaii.photos.mediastore.insertMedia
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetFavouriteRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CloudFileManager(
    override val mediaDao: MediaDao,
    override val customDao: CustomEntityDao,
    override val syncTaskDao: SyncTaskDao,
    override val assetClient: AssetsClient,
    override val albumsClient: AlbumsClient,
    override val info: ImmichBasicInfo
) : GenericFileManager {
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun setFavourite(
        context: Context,
        favourite: Boolean,
        list: List<SelectionManager.SelectedItem>,
        taskId: Int?
    ) = withContext(Dispatchers.IO) {
        if (list.isEmpty()) return@withContext null

        val taskId = taskId ?: syncTaskDao.insert(
            task = SyncTask(
                dateModified = Clock.System.now().epochSeconds,
                status = SyncTaskStatus.Processing,
                type = SyncTaskType.Favourite,
                destination = favourite.toString(),
                items = list
            )
        )

        mediaDao.setFavouriteOnMedia(
            ids = list.fastMap { it.id }.toSet(),
            favourite = favourite
        )

        assetClient.favourite(
            request = AssetFavouriteRequest(
                ids = list.fastMap { Uuid.parse(it.immichId!!) },
                isFavorite = favourite
            ),
            accessToken = info.accessToken
        ).let { success ->
            syncTaskDao.updateTaskStatus(
                id = taskId.toInt(),
                status =
                    if (success) SyncTaskStatus.Synced
                    else SyncTaskStatus.Waiting
            )
        }

        null
    }

    /** @param albumId should be immich id of this album */
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun setTrashed(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        trashed: Boolean,
        albumId: String?,
        taskId: Int?,
        onItemDone: (totaCount: Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (!trashed) {
            throw IllegalArgumentException("Cannot restore files to albums!")
        }

        if (albumId == null) {
            permanentlyDelete(
                context = context,
                list = list,
                taskId = taskId
            )

            onItemDone(list.size)

            return@withContext true
        }

        val taskId = taskId ?: syncTaskDao.insert(
            task = SyncTask(
                dateModified = Clock.System.now().epochSeconds,
                status = SyncTaskStatus.Processing,
                type = SyncTaskType.Trash,
                destination = albumId,
                items = list
            )
        )

        customDao.deleteAll(
            ids = list.fastMap { it.id }.toSet(),
            album = albumId
        )

        albumsClient.removeAssets(
            albumId = Uuid.parse(albumId),
            assetIds = list.fastMap { Uuid.parse(it.immichId!!) },
            accessToken = info.accessToken
        ).let { success ->
            onItemDone(if (success) list.size else -1)

            syncTaskDao.updateTaskStatus(
                id = taskId.toInt(),
                status =
                    if (success) SyncTaskStatus.Synced
                    else SyncTaskStatus.Waiting
            )

            onItemDone(list.size)

            return@withContext success
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun permanentlyDelete(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        taskId: Int?
    ) = withContext(Dispatchers.IO) {
        val taskId = taskId ?: syncTaskDao.insert(
            task = SyncTask(
                dateModified = Clock.System.now().epochSeconds,
                status = SyncTaskStatus.Processing,
                type = SyncTaskType.Delete,
                destination = null,
                items = list
            )
        )

        mediaDao.deleteAll(
            ids = list.fastMap { it.id }.toSet()
        )

        assetClient.delete(
            ids = list.fastMap { Uuid.parse(it.immichId!!) },
            accessToken = info.accessToken,
            force = false
        ).let { success ->
            syncTaskDao.updateTaskStatus(
                id = taskId.toInt(),
                status =
                    if (success) SyncTaskStatus.Synced
                    else SyncTaskStatus.Waiting
            )
        }
    }

    override fun renameItem(
        context: Context,
        uri: String,
        newName: String
    ): IntentSender? {
        throw NotImplementedError("Immich does not have this functionality.")
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun renameAlbum(
        context: Context,
        album: AlbumType,
        newName: String,
        taskId: Int?
    ) = withContext(Dispatchers.IO) {
        val taskId = taskId ?: syncTaskDao.insert(
            task = SyncTask(
                dateModified = Clock.System.now().epochSeconds,
                status = SyncTaskStatus.Processing,
                type = SyncTaskType.RenameAlbum,
                destination = album.id,
                items = listOf(
                    SelectionManager.SelectedItem(
                        id = 0L,
                        uri = newName,
                        isImage = false,
                        parentPath = ""
                    )
                )
            )
        )

        context.appModule.settings.albums.edit(
            id = album.id,
            newInfo = (album as AlbumType.Cloud).copy(name = newName)
        )

        albumsClient.rename(
            id = Uuid.parse(album.immichId),
            newName = newName,
            accessToken = info.accessToken
        ).let { success ->
            syncTaskDao.updateTaskStatus(
                id = taskId.toInt(),
                status =
                    if (success) SyncTaskStatus.Synced
                    else SyncTaskStatus.Waiting
            )
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun moveItems(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: AlbumType,
        preserveDate: Boolean,
        taskId: Int?,
        onItemDone: (uri: String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        if (list.isEmpty()) return@withContext true

        if (destination !is AlbumType.Cloud) {
            throw IllegalArgumentException("Cannot move items between ${AlbumType.Cloud::class.simpleName} and ${destination::class.simpleName}")
        }

        val taskId = taskId ?: syncTaskDao.insert(
            task = SyncTask(
                dateModified = Clock.System.now().epochSeconds,
                status = SyncTaskStatus.Processing,
                type = SyncTaskType.Move,
                destination = destination.id,
                items = list
            )
        )

        customDao.upsertAll(
            items = list.map {
                CustomItem(
                    id = it.id,
                    album = destination.id
                )
            }
        )

        val origins = list.map { it.parentPath }.distinct().filter { it != destination.immichId }
        origins.forEach { origin ->
            customDao.deleteAll(
                ids = list.fastMap { it.id }.toSet(),
                album = origin
            )
        }

        val assetIds = copyItems(
            context = context,
            list = list,
            destination = destination,
            preserveDate = preserveDate,
            overrideDisplayName = null,
            onItemDone = onItemDone
        ).fastMap { Uuid.parse(it.immichId!!) }

        var success = true

        origins.forEach { origin ->
            success = success && albumsClient.removeAssets(
                albumId = Uuid.parse(origin),
                assetIds = assetIds,
                accessToken = info.accessToken
            )
        }

        syncTaskDao.updateTaskStatus(
            id = taskId.toInt(),
            status =
                if (success) SyncTaskStatus.Synced
                else SyncTaskStatus.Waiting
        )

        return@withContext success
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun copyItems(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: AlbumType,
        preserveDate: Boolean,
        overrideDisplayName: ((displayName: String) -> String)?,
        taskId: Int?,
        onItemDone: (uri: String) -> Unit
    ): List<GenericFileManager.CopyResult> = withContext(Dispatchers.IO) {
        if (list.isEmpty()) return@withContext emptyList()

        return@withContext when (destination) {
            is AlbumType.Folder -> {
                copyToLocal(context, list, destination, preserveDate, overrideDisplayName, onItemDone)
            }

            is AlbumType.Custom -> {
                copyToCustom(context, list, destination, onItemDone)
            }

            is AlbumType.Cloud -> {
                copyToCloud(context, list, destination, taskId, onItemDone)
            }

            else -> {
                emptyList()
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun copyToCloud(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: AlbumType.Cloud,
        taskId: Int?,
        onItemDone: (uri: String) -> Unit
    ): List<GenericFileManager.CopyResult> = withContext(Dispatchers.IO) {
        val items = list.filter { it.parentPath != destination.immichId }

        val taskId = taskId ?: syncTaskDao.insert(
            task = SyncTask(
                dateModified = Clock.System.now().epochSeconds,
                status = SyncTaskStatus.Processing,
                type = SyncTaskType.Copy,
                destination = destination.id,
                items = list
            )
        )

        albumsClient.addAssets(
            albumId = Uuid.parse(destination.immichId),
            assetIds = items.fastMap { Uuid.parse(it.immichId!!) },
            accessToken = info.accessToken
        ).let { success ->
            syncTaskDao.updateTaskStatus(
                id = taskId.toInt(),
                status =
                    if (success) SyncTaskStatus.Synced
                    else SyncTaskStatus.Waiting
            )
        }

        items.forEach { onItemDone(it.uri) }

        return@withContext items.fastMap {
            GenericFileManager.CopyResult(
                id = it.id,
                immichId = it.immichId
            )
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun copyToCustom(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: AlbumType.Custom,
        onItemDone: (uri: String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        if (!pictures.exists()) pictures.mkdirs()

        val download = File(pictures, "Lavender Photos")
        if (!download.exists()) download.mkdirs()

        val album = AlbumType.Folder(
            id = Uuid.random().toString(),
            name = "Lavender Photos",
            pinned = false,
            immichId = null,
            paths = setOf(download.absolutePath)
        )

        val ids = copyToLocal(
            context = context,
            list = list,
            preserveDate = true,
            destination = album,
            overrideDisplayName = null,
            onItemDone = onItemDone
        )

        while (!mediaDao.exists(ids.last().id)) {
            delay(500)
        }

        customDao.upsertAll(
            ids.fastMap { item ->
                CustomItem(
                    id = item.id,
                    album = destination.id
                )
            }
        )

        return@withContext ids
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun copyToLocal(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: AlbumType.Folder,
        preserveDate: Boolean,
        overrideDisplayName: ((displayName: String) -> String)?,
        onItemDone: (uri: String) -> Unit
    ): List<GenericFileManager.CopyResult> = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver

        val mediaItems = list.chunked(500).flatMap { chunk ->
            mediaDao.getMedia(ids = chunk.fastMap { it.id })
        }

        return@withContext list.mapNotNull { item ->
            val media = mediaItems.first { it.id == item.id }
            val bytes = assetClient.download(
                id = Uuid.parse(media.immichId!!),
                accessToken = info.accessToken
            )

            if (bytes == null) return@mapNotNull null

            onItemDone(media.uri)

            var newId = 0L
            destination.paths.forEach { path ->
                contentResolver.insertMedia(
                    context = context,
                    media = media,
                    destination = path,
                    basePath = path.toBasePath(),
                    overrideDisplayName = if (overrideDisplayName != null) overrideDisplayName(media.displayName) else null,
                    currentVolumes = MediaStore.getExternalVolumeNames(context),
                    preserveDate = preserveDate,
                    onInsert = { _, new ->
                        contentResolver.openOutputStream(new)?.use {
                            it.buffered().write(bytes)
                        }

                        if (new.toString().startsWith("content")) {
                            newId = new.lastPathSegment!!.toLong()
                        } else {
                            val path = new.toString().substringAfter(":")
                            val new = contentResolver.getUriFromAbsolutePath(
                                absolutePath = path,
                                type = media.type
                            )
                            newId = new?.lastPathSegment?.toLong() ?: 0
                        }
                    }
                )
            }

            GenericFileManager.CopyResult(
                id = newId,
                immichId = media.immichId
            )
        }
    }
}