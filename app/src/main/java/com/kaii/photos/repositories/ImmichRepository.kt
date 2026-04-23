package com.kaii.photos.repositories

import android.content.Context
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.room.withTransaction
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.daos.SyncTaskDao
import com.kaii.photos.database.entities.CustomItem
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.database.entities.toExifData
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.file_management.managers.CloudFileManager
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.immichDurationToSecondsOrNull
import com.kaii.photos.helpers.paging.mapToMedia
import com.kaii.photos.helpers.paging.mapToSeparatedMedia
import com.kaii.photos.mediastore.MediaType
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.ApiClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import io.github.kaii_lb.lavender.immichintegration.serialization.albums.AlbumGetState
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class ImmichRepository(
    private val album: AlbumType,
    private val scope: CoroutineScope,
    private val mediaDao: MediaDao,
    private val customDao: CustomEntityDao,
    syncTaskDao: SyncTaskDao,
    sortMode: Flow<MediaItemSortMode>,
    format: Flow<DisplayDateFormat>,
    info: Flow<ImmichBasicInfo>,
    client: ApiClient,
    context: Context
) : BaseRepo {
    private val db = MediaDatabase.getInstance(context.applicationContext)

    override var fileManager = CloudFileManager(
        mediaDao = mediaDao,
        customDao = customDao,
        syncTaskDao = syncTaskDao,
        assetClient = AssetsClient(
            baseUrl = "",
            client = client
        ),
        albumsClient = AlbumsClient(
            baseUrl = "",
            client = client
        ),
        info = ImmichBasicInfo.Empty
    )

    private val params = combine(info, sortMode, format) { info, sortMode, format ->
        RoomQueryParams(
            sortMode = sortMode,
            format = format,
            info = info
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = RoomQueryParams(
            sortMode = MediaItemSortMode.DateTaken,
            format = DisplayDateFormat.Default,
            info = ImmichBasicInfo.Empty
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
                if (params.sortMode.isDateModified) customDao.getPagedMediaDateModified(album = album.id)
                else customDao.getPagedMediaDateTaken(album = album.id)
            }
        ).flow.mapToMedia(accessToken = params.info.accessToken)
    }.cachedIn(scope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val gridMediaFlow = params.flatMapLatest { params ->
        mediaFlow.mapToSeparatedMedia(
            sortMode = params.sortMode,
            format = params.format
        )
    }.cachedIn(scope)

    fun refresh() = scope.launch(Dispatchers.IO) { refetch() }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun refetch() {
        val snapshot = params.value

        val info = fileManager.albumsClient.get(
            id = Uuid.parse(album.immichId!!),
            accessToken = snapshot.info.accessToken,
            withoutAssets = false
        )

        val state = info?.let { AlbumGetState.Retrieved(it) } ?: AlbumGetState.Failed

        if (state is AlbumGetState.Retrieved) {
            val items =
                state.album.assets.fastMap { asset ->
                    MediaStoreData(
                        id = Uuid.parse(asset.id).toLongs { a, _ -> a },
                        uri = "${snapshot.info.endpoint}/api/assets/${asset.id}/original",
                        dateTaken = Instant.parse(asset.fileCreatedAt).epochSeconds,
                        dateModified = Instant.parse(asset.fileModifiedAt).epochSeconds,
                        type = if (asset.type == AssetType.Image) MediaType.Image else MediaType.Video,
                        absolutePath = "",
                        parentPath = "",
                        displayName = asset.originalFileName,
                        mimeType = asset.originalMimeType,
                        immichUrl = "${snapshot.info.endpoint}/api/assets/${asset.id}/original",
                        hash = asset.checksum,
                        size = asset.exifInfo?.fileSizeInByte ?: 0L,
                        favourited = asset.isFavorite,
                        duration = asset.duration.immichDurationToSecondsOrNull()
                    )
                }

            val mediaIds = customDao.getAllIdsIn(album = album.id).toSet()
            val added = items.fastMap { it.id }.toSet() - mediaIds
            val deleted = mediaIds - items.fastMap { it.id }.toSet()

            db.withTransaction {
                mediaDao.upsertAll(items = items)

                customDao.deleteAll(ids = deleted, album = album.id)
                customDao.upsertAll(items = added.map { CustomItem(id = it, album = album.id) })

                db.exifDataDao().upsertAll(
                    items = state.album.assets.fastMapNotNull {
                        it.exifInfo?.toExifData(
                            mediaId = Uuid.parse(it.id).toLongs { a, _ -> a }
                        )
                    }
                )
            }
        }
    }

    init {
        scope.launch {
            info
                .distinctUntilChanged()
                .collectLatest { info ->
                    fileManager = CloudFileManager(
                        mediaDao = mediaDao,
                        customDao = customDao,
                        syncTaskDao = syncTaskDao,
                        assetClient = AssetsClient(
                            baseUrl = info.endpoint,
                            client = client
                        ),
                        albumsClient = AlbumsClient(
                            baseUrl = info.endpoint,
                            client = client
                        ),
                        info = info
                    )
                }

            refresh()
        }
    }

    override suspend fun getMediaCount(): Int = withContext(Dispatchers.IO) {
        return@withContext db.customDao().countMediaInAlbum(album = album.id)
    }

    override suspend fun getMediaSize(): Long = withContext(Dispatchers.IO) {
        return@withContext db.customDao().mediaSize(album = album.id)
    }

    override fun allowedAlbumTypesFor(
        moving: Boolean
    ) = fileManager.allowedAlbumTypesFor(
        moving = moving,
        current = AlbumType.Cloud::class
    )

    override suspend fun setTrashed(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        trashed: Boolean,
        onItemDone: (totaCount: Int) -> Unit
    ) = fileManager.setTrashed(context, list, trashed, album.id, null, onItemDone)

    override suspend fun renameAlbum(
        context: Context,
        newName: String
    ) = fileManager.renameAlbum(context, album, newName)
}