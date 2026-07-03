package com.kaii.photos.repositories

import android.content.Context
import androidx.compose.ui.util.fastMap
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.room.withTransaction
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.CustomItem
import com.kaii.photos.database.entities.toExifData
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.file_management.managers.CloudFileManager
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.paging.mapToMedia
import com.kaii.photos.helpers.paging.mapToSeparatedMedia
import com.kaii.photos.mediastore.toMediaStoreData
import io.github.kaii_lb.lavender.immichintegration.Auth
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.ApiClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
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
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class ImmichRepository(
    private val album: AlbumType,
    private val scope: CoroutineScope,
    private val db: MediaDatabase,
    sortMode: Flow<MediaItemSortMode>,
    format: Flow<DisplayDateFormat>,
    info: Flow<ImmichBasicInfo>,
    client: ApiClient
) : BaseRepo {
    private val mediaDao = db.mediaDao()
    private val customDao = db.customDao()

    override val fileManager = CloudFileManager(
        mediaDao = mediaDao,
        customDao = customDao,
        syncTaskDao = db.taskDao(),
        assetClient = AssetsClient(
            endpoint = "",
            auth = Auth.None,
            client = client
        ),
        albumsClient = AlbumsClient(
            endpoint = "",
            auth = Auth.None,
            client = client
        )
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
    override val mediaFlow = params.flatMapLatest { params ->
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
        ).flow.mapToMedia(
            auth = params.info.auth,
            endpoint = params.info.endpoint
        )
    }.cachedIn(scope)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val gridMediaFlow = params.flatMapLatest { params ->
        mediaFlow.mapToSeparatedMedia(
            sortMode = params.sortMode,
            format = params.format
        )
    }.cachedIn(scope)

    fun refresh() = scope.launch(Dispatchers.IO) { refetch() }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun refetch() {
        val cloudAlbum = fileManager.albumsClient.get(
            id = Uuid.parse(album.immichId!!),
            withoutAssets = false
        ) ?: return

        val cloudAssets = fileManager.assetClient.getForAlbum(cloudAlbum.id) ?: return

        val items =
            cloudAssets.map { asset ->
                asset.toMediaStoreData()
            }

        val mediaIds = customDao.getAllIdsIn(album = album.id).toSet()
        val added = items.fastMap { it.id }.toSet() - mediaIds
        val deleted = mediaIds - items.fastMap { it.id }.toSet()

        db.withTransaction {
            mediaDao.upsertAll(items = items)

            customDao.deleteAll(ids = deleted, album = album.id)
            customDao.upsertAll(items = added.map { CustomItem(id = it, album = album.id) })

            db.exifDataDao().upsertAll(
                items = cloudAssets.mapNotNull {
                    it.exifInfo?.toExifData(
                        mediaId = Uuid.parse(it.id).toLongs { a, _ -> a }
                    )
                }
            )
        }
    }

    init {
        scope.launch {
            info
                .distinctUntilChanged()
                .collectLatest { info ->
                    fileManager.setEndpoint(info.endpoint)
                    fileManager.setAuth(info.auth)
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
        albumId: String?,
        immichId: String?,
        onItemDone: (totaCount: Int) -> Unit
    ) = fileManager.setTrashed(context, list, trashed, albumId ?: album.id, immichId ?: album.immichId, null, onItemDone)

    override suspend fun renameAlbum(
        context: Context,
        newName: String
    ) = fileManager.renameAlbum(context, album, newName)
}