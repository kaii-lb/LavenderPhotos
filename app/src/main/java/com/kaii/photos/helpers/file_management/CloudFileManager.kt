package com.kaii.photos.helpers.file_management

import android.content.Context
import android.content.IntentSender
import android.os.Environment
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
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetFavouriteRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.reflect.KClass
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CloudFileManager(
    override val customDao: CustomEntityDao,
    override val syncTaskDao: SyncTaskDao,
    override val assetClient: AssetsClient,
    override val albumsClient: AlbumsClient,
    override val accessToken: String
) : GenericFileManager {
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun setFavourite(
        context: Context,
        favourite: Boolean,
        list: List<String>,
        onItemDone: (totaCount: Int) -> Unit
    ) {
        assetClient.favourite(
            request = AssetFavouriteRequest(
                ids = list.fastMap { Uuid.parse(it) },
                isFavorite = favourite
            ),
            accessToken = accessToken
        ).let {
            onItemDone(if (it) list.size else -1)
        }
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
        origin: String,
        originType: KClass<out AlbumType>,
        destination: String,
        preserveDate: Boolean,
        onItemDone: (totalCount: Int) -> Unit
    ): Boolean {
        val assetIds = copyItems(
            context = context,
            list = list,
            origin = origin,
            originType = originType,
            destination = destination,
            preserveDate = preserveDate,
            overrideDisplayName = null,
            onItemDone = onItemDone
        ).fastMap { Uuid.parse(it.immichId!!) }

        return albumsClient.removeAssets(
            albumId = Uuid.parse(origin),
            assetIds = assetIds,
            accessToken = accessToken
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun copyItems(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        origin: String,
        originType: KClass<out AlbumType>,
        destination: String,
        preserveDate: Boolean,
        overrideDisplayName: ((displayName: String) -> String)?,
        onItemDone: (totalCount: Int) -> Unit
    ): List<GenericFileManager.CopyResult> = withContext(Dispatchers.IO) {
        return@withContext when (originType) {
            AlbumType.Folder::class -> {
                copyToLocal(context, list, origin, destination, preserveDate, overrideDisplayName, onItemDone)
            }

            AlbumType.Custom::class -> {
                copyToCustom(context, list, origin, destination, onItemDone)
            }

            AlbumType.Cloud::class -> {
                copyToCloud(context, list, destination, onItemDone)
            }

            else -> {
                emptyList()
            }
        }
    }

    override suspend fun copyToCustom(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        origin: String,
        destination: String,
        onItemDone: (totalCount: Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        if (!pictures.exists()) pictures.mkdirs()

        val download = File(pictures, "Lavender Photos")
        if (!download.exists()) download.mkdirs()

        val ids = copyToLocal(
            context = context,
            list = list,
            origin = origin,
            preserveDate = true,
            destination = download.absolutePath,
            overrideDisplayName = null,
            onItemDone = onItemDone
        )

        customDao.upsertAll(
            ids.fastMap { item ->
                CustomItem(
                    id = item.id,
                    album = destination
                )
            }
        )

        return@withContext ids
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun copyToLocal(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        origin: String,
        destination: String,
        preserveDate: Boolean,
        overrideDisplayName: ((displayName: String) -> String)?,
        onItemDone: (totalCount: Int) -> Unit
    ): List<GenericFileManager.CopyResult> = withContext(Dispatchers.IO) {
        val ids = list.fastMap { it.id }
        val mediaItems = customDao.getMediaInAlbum(album = origin).filter {
            it.id in ids
        }

        return@withContext list.mapIndexedNotNull { index, item ->
            val media = mediaItems.first { it.id == item.id }
            val uuid = media.immichId!!
            val bytes = assetClient.download(
                id = Uuid.parse(uuid),
                accessToken = accessToken
            )

            if (bytes == null) return@mapIndexedNotNull null

            onItemDone(index + 1)

            File(destination, media.displayName)
                .outputStream()
                .buffered()
                .write(bytes)

            GenericFileManager.CopyResult(
                id = item.id,
                immichId = media.immichId
            )
        }
    }
}