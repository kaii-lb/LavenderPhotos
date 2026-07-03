package com.kaii.photos.file_management.managers

import android.content.Context
import android.content.IntentSender
import android.provider.MediaStore
import androidx.compose.ui.util.fastMap
import androidx.core.content.FileProvider
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.daos.SyncTaskDao
import com.kaii.photos.database.entities.CustomItem
import com.kaii.photos.database.entities.SyncTask
import com.kaii.photos.database.entities.SyncTaskItem
import com.kaii.photos.database.entities.SyncTaskStatus
import com.kaii.photos.database.entities.SyncTaskType
import com.kaii.photos.database.sync.CloudSyncWorker
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.helpers.appCloudFolderDir
import com.kaii.photos.helpers.calculateSha1Checksum
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.mediastore.LAVENDER_FILE_PROVIDER_AUTHORITY
import com.kaii.photos.mediastore.insertMedia
import com.kaii.photos.mediastore.setDateForMedia
import com.kaii.photos.mediastore.toContentId
import io.github.kaii_lb.lavender.immichintegration.FileWriteChannel
import io.github.kaii_lb.lavender.immichintegration.UriWriteChannel
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import io.github.kaii_lb.lavender.immichintegration.serialization.albums.AlbumUpdateDto
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetFavouriteRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CloudFileManager(
    override val mediaDao: MediaDao,
    override val customDao: CustomEntityDao,
    override val syncTaskDao: SyncTaskDao,
    override val assetClient: AssetsClient,
    override val albumsClient: AlbumsClient
) : GenericFileManager {
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun getShareItems(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ): List<SelectionManager.SelectedItem> {
        val names = list.chunked(500).flatMap { chunk ->
            mediaDao.getMedia(ids = chunk.fastMap { it.id })
        }.associate { it.id to it.displayName }

        return list.mapNotNull { item ->
            val file = File(context.cacheDir, names[item.id]!!)

            val checksumOriginal = calculateSha1Checksum(file = file)
            val checksumCloud = assetClient.get(
                id = Uuid.parse(item.immichId!!)
            )?.checksum

            val checksumMatch = checksumCloud == checksumOriginal

            val uri = FileProvider.getUriForFile(
                context,
                LAVENDER_FILE_PROVIDER_AUTHORITY,
                file
            ).toString()

            if (checksumMatch) {
                item.copy(uri = uri)
            } else {
                assetClient.download(
                    id = Uuid.parse(item.immichId!!),
                    channel = FileWriteChannel(file = file)
                ).let { success ->
                    if (success) {
                        item.copy(uri = uri)
                    } else {
                        file.delete()
                        null
                    }
                }
            }
        }
    }

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
                destination = favourite.toString()
            )
        ).toInt()

        syncTaskDao.insert(
            items = list.fastMap {
                SyncTaskItem(
                    mediaId = it.id,
                    taskId = taskId
                )
            }
        )

        mediaDao.setFavouriteOnMedia(
            ids = list.fastMap { it.id }.toSet(),
            favourite = favourite
        )

        assetClient.favourite(
            request = AssetFavouriteRequest(
                ids = list.fastMap { Uuid.parse(it.immichId!!) },
                isFavorite = favourite
            )
        ).let { success ->
            syncTaskDao.updateTaskStatus(
                id = taskId,
                status =
                    if (success) SyncTaskStatus.Synced
                    else SyncTaskStatus.Waiting
            )
        }

        null
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun setTrashed(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        trashed: Boolean,
        albumId: String?,
        immichId: String?,
        taskId: Int?,
        onItemDone: (totaCount: Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (list.isEmpty()) return@withContext true

        if (!trashed) {
            throw IllegalArgumentException("Cannot restore files to albums!")
        }

        if (immichId == null) {
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
                destination = immichId
            )
        ).toInt()

        syncTaskDao.insert(
            items = list.fastMap {
                SyncTaskItem(
                    mediaId = it.id,
                    taskId = taskId
                )
            }
        )

        customDao.deleteAll(
            ids = list.fastMap { it.id }.toSet(),
            album = immichId
        )

        albumsClient.removeAssets(
            albumId = Uuid.parse(immichId),
            assetIds = list.fastMap { Uuid.parse(it.immichId!!) }
        ).let { success ->
            onItemDone(if (success) list.size else -1)

            syncTaskDao.updateTaskStatus(
                id = taskId,
                status =
                    if (success) SyncTaskStatus.Synced
                    else SyncTaskStatus.Waiting
            )

            return@withContext success
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun permanentlyDelete(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        taskId: Int?
    ) = withContext(Dispatchers.IO) {
        if (list.isEmpty()) return@withContext false

        val taskId = taskId ?: syncTaskDao.insert(
            task = SyncTask(
                dateModified = Clock.System.now().epochSeconds,
                status = SyncTaskStatus.Processing,
                type = SyncTaskType.Delete,
                destination = null
            )
        ).toInt()

        syncTaskDao.insert(
            items = list.fastMap {
                SyncTaskItem(
                    mediaId = it.id,
                    taskId = taskId
                )
            }
        )

        mediaDao.deleteAll(
            ids = list.fastMap { it.id }.toSet()
        )

        assetClient.delete(
            ids = list.fastMap { Uuid.parse(it.immichId!!) },
            force = false
        ).let { success ->
            syncTaskDao.updateTaskStatus(
                id = taskId,
                status =
                    if (success) SyncTaskStatus.Synced
                    else SyncTaskStatus.Waiting
            )

            return@withContext success
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
                extraData = newName
            )
        ).toInt()

        super.renameAlbum(context, album, newName, taskId)

        albumsClient.update(
            id = Uuid.parse(
                uuidString = album.immichId!!
            ),
            info = AlbumUpdateDto(
                albumName = newName,
                albumThumbnailAssetId = null,
                description = null,
                isActivityEnabled = null,
                order = null
            )
        ).let { success ->
            syncTaskDao.updateTaskStatus(
                id = taskId,
                status =
                    if (success) SyncTaskStatus.Synced
                    else SyncTaskStatus.Waiting
            )
        }
    }

    // TODO: implement cloud backup for secure items
    override suspend fun secure(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ): Boolean {
        throw NotImplementedError("Cannot access secure folder functionality in an immich context!")
    }

    override suspend fun restore(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ): Boolean {
        throw NotImplementedError("Cannot restore items outside secure folder")
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun moveItems(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: AlbumType,
        preserveDate: Boolean,
        taskId: Int?,
        origin: AlbumType?,
        onItemDone: (uri: String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        throw NotImplementedError("Immich does not have this functionality.")
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
        val taskId = taskId ?: syncTaskDao.insert(
            task = SyncTask(
                dateModified = Clock.System.now().epochSeconds,
                status = SyncTaskStatus.Processing,
                type = SyncTaskType.Copy,
                destination = destination.id
            )
        ).toInt()

        syncTaskDao.insert(
            items = list.fastMap {
                SyncTaskItem(
                    mediaId = it.id,
                    taskId = taskId
                )
            }
        )

        albumsClient.addAssets(
            albumId = Uuid.parse(destination.immichId),
            assetIds = list.fastMap { Uuid.parse(it.immichId!!) }
        ).let { success ->
            syncTaskDao.updateTaskStatus(
                id = taskId,
                status =
                    if (success) SyncTaskStatus.Synced
                    else SyncTaskStatus.Waiting
            )
        }

        list.forEach { onItemDone(it.uri) }

        return@withContext list.fastMap {
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
        val folder = appCloudFolderDir

        val album = AlbumType.Folder(
            id = Uuid.random().toString(),
            name = folder.name,
            pinned = false,
            immichId = null,
            paths = setOf(folder.absolutePath)
        )

        val ids = copyToLocal(
            context = context,
            list = list,
            preserveDate = true,
            destination = album,
            overrideDisplayName = null,
            onItemDone = onItemDone
        )

        if (ids.isEmpty()) return@withContext emptyList()

        var tries = 0
        while (!mediaDao.exists(ids.last().id) && tries < 100) {
            delay(500.milliseconds)
            tries += 1
        }

        customDao.upsertAll(
            ids.fastMap { item ->
                CustomItem(
                    id = item.id,
                    album = destination.id
                )
            }
        )

        if (destination.immichId != null) {
            CloudSyncWorker.immediateEnqueue(context = context, albumId = destination.id)
        }

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
        }.associateBy { it.id }

        val result = list.mapNotNull { item ->
            val media = mediaItems[item.id]!!

            var newId = 0L
            destination.paths.forEach { path ->
                contentResolver.insertMedia(
                    context = context,
                    media = media,
                    destination = path,
                    overrideDisplayName = if (overrideDisplayName != null) overrideDisplayName(media.displayName) else null,
                    currentVolumes = MediaStore.getExternalVolumeNames(context),
                    preserveDate = preserveDate,
                    onInsert = { _, _ -> }
                )?.let { new ->
                    new.toContentId(contentResolver = contentResolver, type = media.type)?.let {
                        newId = it
                    }

                    val downloaded = assetClient.download(
                        id = Uuid.parse(item.immichId!!),
                        channel = UriWriteChannel(
                            uri = new,
                            context = context
                        )
                    )

                    if (!downloaded) {
                        context.contentResolver.delete(new, null)
                        return@mapNotNull null
                    }

                    contentResolver.setDateForMedia(
                        uri = new,
                        type = media.type,
                        dateTaken = media.dateTaken
                    )

                    onItemDone(item.uri)
                }
            }

            GenericFileManager.CopyResult(
                id = newId,
                immichId = item.immichId
            )
        }

        launch {
            delay(5000.milliseconds)
            if (destination.immichId != null) {
                CloudSyncWorker.immediateEnqueue(context = context, albumId = destination.id)
            }
        }

        return@withContext result
    }
}