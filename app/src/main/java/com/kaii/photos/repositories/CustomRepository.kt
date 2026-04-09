package com.kaii.photos.repositories

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.daos.SyncTaskDao
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.file_management.HybridFileManager
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.paging.mapToMedia
import com.kaii.photos.helpers.paging.mapToSeparatedMedia
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.ApiClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class CustomRepository(
    private val customDao: CustomEntityDao,
    private val album: AlbumType,
    mediaDao: MediaDao,
    syncTaskDao: SyncTaskDao,
    client: ApiClient,
    scope: CoroutineScope,
    sortMode: Flow<MediaItemSortMode>,
    format: Flow<DisplayDateFormat>,
    info: Flow<ImmichBasicInfo>
) : BaseRepo {
    private val params = combine(info, sortMode, format) { info, sortMode, format ->
        RoomQueryParams(
            sortMode = sortMode,
            format = format,
            info = info
        )
    }

    override var fileManager = HybridFileManager(
        isCustom = true,
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val mediaFlow = params.flatMapLatest { params ->
        Pager(
            config = PagingConfig(
                pageSize = 50,
                prefetchDistance = 50,
                enablePlaceholders = true,
                initialLoadSize = 100
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

    suspend fun remove(
        items: Set<MediaStoreData>,
        albumId: String
    ) {
        customDao.deleteAll(ids = items.map { it.id }.toSet(), album = albumId)
    }

    override suspend fun getMediaCount(): Int = withContext(Dispatchers.IO) {
        return@withContext customDao.countMediaInAlbum(album = album.id)
    }

    override suspend fun getMediaSize(): Long = withContext(Dispatchers.IO) {
        return@withContext customDao.mediaSize(album = album.id)
    }

    init {
        scope.launch {
            params.mapLatest { it.info }
                .distinctUntilChanged()
                .collectLatest { info ->
                    fileManager = HybridFileManager(
                        isCustom = true,
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
        }
    }

    override fun allowedAlbumTypesFor(
        moving: Boolean
    ) = fileManager.allowedAlbumTypesFor(
        moving = moving,
        current = AlbumType.Custom::class
    )

    override suspend fun move(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        origin: AlbumType?,
        destination: AlbumType,
        preserveDate: Boolean,
        onItemDone: (totalCount: Int) -> Unit
    ): Boolean {
        var count = 0

        return fileManager.moveItems(context, list, destination, preserveDate, null, origin) {
            count += 1
            onItemDone(count)
        }
    }

    override suspend fun renameAlbum(
        context: Context,
        newName: String
    ) = fileManager.renameAlbum(context, album, newName)

    override suspend fun setTrashed(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        trashed: Boolean,
        onItemDone: (totaCount: Int) -> Unit
    ) = fileManager.setTrashed(context, list, trashed, album.id, null, onItemDone)
}