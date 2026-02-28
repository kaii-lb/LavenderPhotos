package com.kaii.photos.repositories

import android.content.Context
import android.os.Build
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.util.fastMap
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.room.withTransaction
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.CustomItem
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
import io.github.kaii_lb.lavender.immichintegration.clients.ApiClient
import io.github.kaii_lb.lavender.immichintegration.serialization.albums.AlbumGetState
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetType
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.UploadAssetRequest
import io.github.kaii_lb.lavender.immichintegration.state_managers.AlbumsStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ImmichRepository(
    private val albumInfo: AlbumInfo,
    private val scope: CoroutineScope,
    sortMode: Flow<MediaItemSortMode>,
    format: Flow<DisplayDateFormat>,
    info: Flow<ImmichBasicInfo>,
    apiClient: ApiClient,
    context: Context
) {
    private data class Params(
        val endpoint: String,
        override val sortMode: MediaItemSortMode,
        override val format: DisplayDateFormat,
        override val accessToken: String
    ) : RoomQueryParams(sortMode, format, accessToken)

    private val appContext = context.applicationContext
    private val db = MediaDatabase.getInstance(appContext)

    private val albumState = mutableStateOf(
        AlbumsStateManager(
            baseUrl = "",
            coroutineScope = scope,
            apiClient = apiClient
        )
    )

    private val params = combine(info, sortMode, format) { info, sortMode, format ->
        Params(
            endpoint = "",
            accessToken = info.accessToken,
            sortMode = sortMode,
            format = format
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = Params(
            endpoint = "",
            accessToken = "",
            sortMode = MediaItemSortMode.DateTaken,
            format = DisplayDateFormat.Default
        )
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val mediaFlow = params.flatMapLatest { params ->
        Pager(
            config = PagingConfig(
                pageSize = 80,
                prefetchDistance = 40,
                enablePlaceholders = true,
                initialLoadSize = 80
            ),
            pagingSourceFactory = {
                if (params.sortMode.isDateModified) db.customDao().getPagedMediaDateModified(album = albumInfo.id)
                else db.customDao().getPagedMediaDateTaken(album = albumInfo.id)
            }
        ).flow.mapToMedia(accessToken = params.accessToken)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val gridMediaFlow = params.flatMapLatest { params ->
        mediaFlow.mapToSeparatedMedia(
            sortMode = params.sortMode,
            format = params.format
        )
    }

    init {
        refresh()
    }

    fun refresh() = scope.launch(Dispatchers.IO) { refetch() }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun refetch() {
        val snapshot = params.value

        val state = albumState.value.getInfo(
            id = Uuid.parse(albumInfo.immichId),
            accessToken = snapshot.accessToken
        )

        if (state is AlbumGetState.Retrieved) {
            val items =
                state.album.assets.fastMap { asset ->
                    MediaStoreData(
                        id = Uuid.parse(asset.id).toLongs { a, _ -> a },
                        uri = "${snapshot.endpoint}/api/assets/${asset.id}/original",
                        dateTaken = Instant.parse(asset.fileCreatedAt).epochSeconds,
                        dateModified = Instant.parse(asset.fileModifiedAt).epochSeconds,
                        type = if (asset.type == AssetType.Image) MediaType.Image else MediaType.Video,
                        absolutePath = "",
                        parentPath = "",
                        displayName = asset.originalFileName,
                        mimeType = asset.originalMimeType,
                        immichUrl = "${snapshot.endpoint}/api/assets/${asset.id}/original",
                        immichThumbnail = "${snapshot.endpoint}/api/assets/${asset.id}/thumbnail",
                        hash = asset.checksum,
                        size = asset.exifInfo?.fileSizeInByte ?: 0L,
                        favourited = asset.isFavorite
                    )
                }

            val mediaIds = db.customDao().getAllIdsIn(album = albumInfo.id).toSet()
            val orphans = db.customDao().getOrphanImmichItems().toSet()
            val added = items.fastMap { it.id }.toSet() - mediaIds
            val deleted = mediaIds - items.fastMap { it.id }.toSet()

            db.withTransaction {
                db.mediaDao().upsertAll(items = items.filter { it.id !in mediaIds })

                db.customDao().deleteAll(ids = deleted, album = albumInfo.id)
                db.customDao().upsertAll(items = added.map { CustomItem(id = it, album = albumInfo.id) })

                db.mediaDao().deleteAll(orphans.map { it.id }.toSet())
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun upload(ids: List<Long>) {
        val media = db.mediaDao()
            .getAllMediaDateTaken()
            .first()
            .filter { it.id in ids }

        albumState.value.addAssets(
            id = Uuid.parse(albumInfo.immichId),
            accessToken = params.value.accessToken,
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
                db.taskDao().insert(
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
        val unsynced = db.taskDao().getUnsyncedTasks()

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

    suspend fun getMediaCount(): Int = withContext(Dispatchers.IO) {
        return@withContext db.customDao().countMediaInAlbum(album = albumInfo.id)
    }

    suspend fun getMediaSize(): Long = withContext(Dispatchers.IO) {
        return@withContext db.customDao().mediaSize(album = albumInfo.id)
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun uploadAssets(task: SyncTask) {
        val ids = task.itemIds.fastMap { it.toLong() }
        val media = db.mediaDao()
            .getAllMediaDateTaken()
            .first()
            .filter { it.id in ids }

        albumState.value.addAssets(
            id = Uuid.parse(albumInfo.immichId),
            accessToken = params.value.accessToken,
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
                    db.taskDao().update(task = task.copy(status = SyncTaskStatus.Synced))
                }
            }
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun deleteAssets(ids: List<String>) {
        ids.forEach {
            albumState.value.delete(
                id = Uuid.parse(it),
                accessToken = params.value.accessToken,
                onResult = {}
            )
        }
    }
}