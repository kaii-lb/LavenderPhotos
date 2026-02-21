package com.kaii.photos.repositories

import android.content.Context
import android.os.Build
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.util.fastMap
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.kaii.lavender.immichintegration.clients.ApiClient
import com.kaii.lavender.immichintegration.serialization.albums.AlbumGetState
import com.kaii.lavender.immichintegration.serialization.assets.AssetType
import com.kaii.lavender.immichintegration.serialization.assets.UploadAssetRequest
import com.kaii.lavender.immichintegration.state_managers.AlbumsStateManager
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.CustomItemEntity
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.database.entities.SyncTask
import com.kaii.photos.database.entities.SyncTaskStatus
import com.kaii.photos.database.entities.SyncTaskType
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.paging.mapToMedia
import com.kaii.photos.helpers.paging.mapToSeparatedMedia
import com.kaii.photos.mediastore.MediaType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ImmichRepository(
    private val albumInfo: AlbumInfo,
    private val info: ImmichBasicInfo,
    private val scope: CoroutineScope,
    private val sortMode: MediaItemSortMode,
    format: DisplayDateFormat,
    apiClient: ApiClient,
    context: Context
) {
    private val appContext = context.applicationContext
    private val customDao = MediaDatabase.getInstance(appContext).customDao()
    private val taskDao = MediaDatabase.getInstance(appContext).taskDao()
    private val mediaDao = MediaDatabase.getInstance(appContext).mediaDao()

    private val albumState = mutableStateOf(
        AlbumsStateManager(
            baseUrl = info.endpoint,
            coroutineScope = scope,
            apiClient = apiClient
        )
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val mediaFlow =
        Pager(
            config = PagingConfig(
                pageSize = 80,
                prefetchDistance = 40,
                enablePlaceholders = true,
                initialLoadSize = 80
            ),
            pagingSourceFactory = {
                if (sortMode.isDateModified) customDao.getPagedMediaDateModified(album = albumInfo.id)
                else customDao.getPagedMediaDateTaken(album = albumInfo.id)
            }
        ).flow.mapToMedia(accessToken = info.accessToken).cachedIn(scope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val gridMediaFlow = mediaFlow.mapToSeparatedMedia(
        sortMode = sortMode,
        format = format
    ).cachedIn(scope)

    init {
        refresh()
    }

    fun refresh() = scope.launch(Dispatchers.IO) { refetch() }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun refetch() {
        albumState.value.getInfo(
            id = Uuid.parse(albumInfo.immichId),
            accessToken = info.accessToken
        ) { state ->
            if (state is AlbumGetState.Retrieved) {
                val items =
                    state.album.assets.fastMap { asset ->
                        MediaStoreData(
                            id = Uuid.parse(asset.id).toLongs { a, _ -> a },
                            uri = "${info.endpoint}/api/assets/${asset.id}/original",
                            dateTaken = Instant.parse(asset.fileCreatedAt).epochSeconds,
                            dateModified = Instant.parse(asset.fileModifiedAt).epochSeconds,
                            type = if (asset.type == AssetType.Image) MediaType.Image else MediaType.Video,
                            absolutePath = "",
                            parentPath = "",
                            displayName = asset.originalFileName,
                            mimeType = asset.originalMimeType,
                            immichUrl = "${info.endpoint}/api/assets/${asset.id}/original",
                            immichThumbnail = "${info.endpoint}/api/assets/${asset.id}/thumbnail",
                            hash = asset.checksum,
                            size = asset.exifInfo?.fileSizeInByte ?: 0L,
                            favourited = asset.isFavorite
                        )
                    }

                val mediaIds = customDao.getAllIdsIn(album = albumInfo.id).toSet()
                val diff = mediaIds - items.fastMap { it.id }.toSet()

                mediaDao.deleteAll(ids = diff)
                mediaDao.insertAll(items = items.toSet())

                customDao.upsertAll(
                    items.fastMap {
                        CustomItemEntity(
                            mediaId = it.id,
                            album = albumInfo.id
                        )
                    }
                )
            }
        }.join()
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun upload(ids: List<Long>) {
        val media = mediaDao
            .getAllMediaDateTaken()
            .first()
            .filter { it.id in ids }

        albumState.value.addAssets(
            id = Uuid.parse(albumInfo.immichId),
            accessToken = info.accessToken,
            assetIds = media.fastMap { item ->
                UploadAssetRequest(
                    absolutePath = item.absolutePath,
                    filename = item.displayName,
                    size = item.size,
                    dateTaken = item.dateTaken,
                    dateModified = item.dateModified,
                    id = Uuid.random()
                )
            },
            deviceId = Build.MODEL,
            onItemDone = {},
            onResult = { success ->
                taskDao.insert(
                    task = SyncTask(
                        dateModified = Clock.System.now().epochSeconds,
                        status = if (success) SyncTaskStatus.Synced else SyncTaskStatus.Waiting,
                        type = SyncTaskType.Upload,
                        itemIds = ids.fastMap { it.toString() }
                    )
                )
            }
        )
    }

    suspend fun syncUploads() {
        val unsynced = taskDao.getUnsyncedTasks()

        unsynced.forEach { task ->
            when (task.type) {
                SyncTaskType.Upload -> {
                    uploadAssets(task = task)
                }

                SyncTaskType.Delete -> {
                    deleteAssets(ids = task.itemIds)
                }

                SyncTaskType.Update -> {
                    TODO()
                }
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun uploadAssets(task: SyncTask) {
        val ids = task.itemIds.fastMap { it.toLong() }
        val media = mediaDao
            .getAllMediaDateTaken()
            .first()
            .filter { it.id in ids }

        albumState.value.addAssets(
            id = Uuid.parse(albumInfo.immichId),
            accessToken = info.accessToken,
            assetIds = media.fastMap { item ->
                UploadAssetRequest(
                    absolutePath = item.absolutePath,
                    filename = item.displayName,
                    size = item.size,
                    dateTaken = item.dateTaken,
                    dateModified = item.dateModified,
                    id = Uuid.random()
                )
            },
            deviceId = Build.MODEL,
            onItemDone = {},
            onResult = { success ->
                if (success) {
                    taskDao.update(task = task.copy(status = SyncTaskStatus.Synced))
                }
            }
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun deleteAssets(ids: List<String>) {
        ids.forEach {
            albumState.value.delete(
                id = Uuid.parse(it),
                accessToken = info.accessToken,
                onResult = {}
            )
        }
    }
}