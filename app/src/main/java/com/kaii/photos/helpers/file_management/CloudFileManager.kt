package com.kaii.photos.helpers.file_management

import android.content.Context
import android.content.IntentSender
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.util.fastMap
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.SyncTaskDao
import com.kaii.photos.database.entities.CustomItem
import com.kaii.photos.database.entities.SyncTask
import com.kaii.photos.database.entities.SyncTaskStatus
import com.kaii.photos.database.entities.SyncTaskType
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.di.appModule
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.toBasePath
import com.kaii.photos.mediastore.insertMedia
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetFavouriteRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CloudFileManager(
    override val customDao: CustomEntityDao,
    override val syncTaskDao: SyncTaskDao,
    override val assetClient: AssetsClient,
    override val albumsClient: AlbumsClient,
    override val accessToken: String,
    override val endpoint: String
) : GenericFileManager {
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun setFavourite(
        context: Context,
        favourite: Boolean,
        list: List<String>
    ) {
        assetClient.favourite(
            request = AssetFavouriteRequest(
                ids = list.fastMap { Uuid.parse(it) },
                isFavorite = favourite
            ),
            accessToken = accessToken
        )
    }

    /** @param albumId should be immich id of this album */
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun setTrashed(
        context: Context,
        list: List<String>,
        trashed: Boolean,
        albumId: String?,
        onItemDone: (totaCount: Int) -> Unit
    ) {
        if (trashed) {
            val taskId = syncTaskDao.insert(
                task = SyncTask(
                    dateModified = Clock.System.now().epochSeconds,
                    status = SyncTaskStatus.Processing,
                    type = SyncTaskType.Delete,
                    destination = albumId,
                    itemIds = list.fastMap { it }
                )
            )

            albumsClient.removeAssets(
                albumId = Uuid.parse(albumId!!),
                assetIds = list.fastMap { Uuid.parse(it) },
                accessToken = accessToken
            ).let {
                onItemDone(if (it) list.size else -1)

                syncTaskDao.updateTaskStatus(
                    id = taskId.toInt(),
                    status =
                        if (it) SyncTaskStatus.Synced
                        else SyncTaskStatus.Waiting
                )
            }
        } else {
            val taskId = syncTaskDao.insert(
                task = SyncTask(
                    dateModified = Clock.System.now().epochSeconds,
                    status = SyncTaskStatus.Processing,
                    type = SyncTaskType.Restore,
                    destination = albumId,
                    itemIds = list.fastMap { it }
                )
            )

            albumsClient.addAssets(
                albumId = Uuid.parse(albumId!!),
                assetIds = list.fastMap { Uuid.parse(it) },
                accessToken = accessToken
            ).let {
                onItemDone(if (it) list.size else -1)

                syncTaskDao.updateTaskStatus(
                    id = taskId.toInt(),
                    status =
                        if (it) SyncTaskStatus.Synced
                        else SyncTaskStatus.Waiting
                )
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun permanentlyDelete(
        context: Context,
        list: List<String>
    ) {
        assetClient.delete(
            ids = list.fastMap { Uuid.parse(it) },
            accessToken = accessToken,
            force = true
        )
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
        newName: String
    ) {
        albumsClient.rename(
            id = Uuid.parse(album.immichId!!),
            newName = newName,
            accessToken = accessToken
        ).let {
            if (it) {
                context.appModule.settings.albums.edit(
                    id = album.id,
                    newInfo = (album as AlbumType.Folder).copy(name = newName)
                )
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun moveItems(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        origin: AlbumType,
        destination: AlbumType,
        preserveDate: Boolean,
        onItemDone: (uri: String) -> Unit
    ): Boolean {
        if (destination !is AlbumType.Cloud || origin !is AlbumType.Cloud) {
            throw IllegalArgumentException("Cannot move items between ${origin::class.simpleName} and ${destination::class.simpleName}")
        }

        val assetIds = copyItems(
            context = context,
            list = list,
            origin = origin,
            destination = destination,
            preserveDate = preserveDate,
            overrideDisplayName = null,
            onItemDone = onItemDone
        ).fastMap { Uuid.parse(it.immichId!!) }

        return albumsClient.removeAssets(
            albumId = Uuid.parse(origin.immichId),
            assetIds = assetIds,
            accessToken = accessToken
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun copyItems(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        origin: AlbumType,
        destination: AlbumType,
        preserveDate: Boolean,
        overrideDisplayName: ((displayName: String) -> String)?,
        onItemDone: (uri: String) -> Unit
    ): List<GenericFileManager.CopyResult> = withContext(Dispatchers.IO) {
        return@withContext when (destination) {
            is AlbumType.Folder -> {
                copyToLocal(context, list, origin, destination, preserveDate, overrideDisplayName, onItemDone)
            }

            is AlbumType.Custom -> {
                copyToCustom(context, list, origin, destination, onItemDone)
            }

            is AlbumType.Cloud -> {
                copyToCloud(context, list, destination, onItemDone)
            }

            else -> {
                emptyList()
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun copyToCustom(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        origin: AlbumType,
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
            origin = origin,
            preserveDate = true,
            destination = album,
            overrideDisplayName = null,
            onItemDone = onItemDone
        )

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
        origin: AlbumType,
        destination: AlbumType.Folder,
        preserveDate: Boolean,
        overrideDisplayName: ((displayName: String) -> String)?,
        onItemDone: (uri: String) -> Unit
    ): List<GenericFileManager.CopyResult> = withContext(Dispatchers.IO) {
        val ids = list.fastMap { it.id }
        val mediaItems = customDao.getMediaInAlbum(album = origin.id).filter {
            it.id in ids
        }

        val contentResolver = context.contentResolver
        return@withContext list.mapNotNull { item ->
            val media = mediaItems.first { it.id == item.id }
            val uuid = media.immichId!!
            val bytes = assetClient.download(
                id = Uuid.parse(uuid),
                accessToken = accessToken
            )

            if (bytes == null) return@mapNotNull null

            onItemDone(media.uri)

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
                    }
                )
            }

            GenericFileManager.CopyResult(
                id = item.id,
                immichId = media.immichId
            )
        }
    }
}