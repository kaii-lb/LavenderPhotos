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
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.file_management.CustomFileManager
import com.kaii.photos.helpers.file_management.GenericFileManager
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
) {
    private val params = combine(info, sortMode, format) { info, sortMode, format ->
        RoomQueryParams(
            sortMode = sortMode,
            format = format,
            info = info
        )
    }

    private var fileManager = CustomFileManager(
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
        accessToken = "",
        endpoint = ""
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

    suspend fun getMediaCount(): Int = withContext(Dispatchers.IO) {
        return@withContext customDao.countMediaInAlbum(album = album.id)
    }

    suspend fun getMediaSize(): Long = withContext(Dispatchers.IO) {
        return@withContext customDao.mediaSize(album = album.id)
    }

    init {
        scope.launch {
            params.mapLatest { it.info }
                .distinctUntilChanged()
                .collectLatest { info ->
                    fileManager = CustomFileManager(
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
                        accessToken = info.accessToken,
                        endpoint = info.endpoint
                    )
                }
        }
    }

    fun allowedAlbumTypesFor(
        action: GenericFileManager.Action
    ) = fileManager.allowedAlbumTypesFor(
        action = action,
        current = AlbumType.Custom::class
    )

    suspend fun copy(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: AlbumType,
        preserveDate: Boolean,
        overrideDisplayName: ((displayName: String) -> String)?,
        onItemDone: (totaCount: Int) -> Unit
    ) {
        var count = 0

        fileManager.copyItems(context, list, destination, preserveDate, overrideDisplayName) {
            count += 1
            onItemDone(count)
        }
    }

    suspend fun move(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: AlbumType,
        preserveDate: Boolean,
        onItemDone: (totalCount: Int) -> Unit
    ) {
        var count = 0

        fileManager.moveItems(context, list, destination, preserveDate) {
            count += 1
            onItemDone(count)
        }
    }

    fun renameItem(
        context: Context,
        uri: String,
        newName: String
    ) = fileManager.renameItem(context, uri, newName)

    suspend fun renameAlbum(
        context: Context,
        newName: String
    ) = fileManager.renameAlbum(context, album, newName)

    suspend fun setTrashed(
        context: Context,
        list: List<String>,
        trashed: Boolean,
        onItemDone: (totaCount: Int) -> Unit
    ) = fileManager.setTrashed(context, list, trashed, album.id, onItemDone)

    suspend fun setFavourite(
        context: Context,
        favourite: Boolean,
        list: List<String>
    ) = fileManager.setFavourite(context, favourite, list)
}