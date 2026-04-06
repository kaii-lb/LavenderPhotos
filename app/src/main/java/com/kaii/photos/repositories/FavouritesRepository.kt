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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class FavouritesRepository(
    mediaDao: MediaDao,
    customDao: CustomEntityDao,
    syncTaskDao: SyncTaskDao,
    client: ApiClient,
    scope: CoroutineScope,
    info: Flow<ImmichBasicInfo>,
    sortMode: Flow<MediaItemSortMode>,
    format: Flow<DisplayDateFormat>
) {
    private val params = combine(info, sortMode, format) { info, sortMode, format ->
        RoomQueryParams(
            sortMode = sortMode,
            format = format,
            info = info
        )
    }

    private var fileManager = HybridFileManager(
        isCustom = false,
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
                if (params.sortMode.isDateModified) mediaDao.getPagedFavouritesDateModified()
                else mediaDao.getPagedFavouritesDateTaken()
            }
        ).flow
            .mapToMedia(accessToken = params.info.accessToken)
    }.cachedIn(scope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val gridMediaFlow = params.flatMapLatest { params ->
        mediaFlow.mapToSeparatedMedia(
            sortMode = params.sortMode,
            format = params.format
        )
    }.cachedIn(scope)

    init {
        scope.launch {
            params.mapLatest { it.info }
                .distinctUntilChanged()
                .collectLatest { info ->
                    fileManager = HybridFileManager(
                        isCustom = false,
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

    suspend fun getExifData(
        context: Context,
        media: MediaStoreData
    ) = fileManager.getExifData(context, media)

    fun allowedAlbumTypesFor(
        moving: Boolean
    ) = fileManager.allowedAlbumTypesFor(
        moving = moving,
        current = AlbumType.Folder::class
    )

    suspend fun copy(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: AlbumType,
        preserveDate: Boolean,
        overrideDisplayName: ((displayName: String) -> String)?,
        onItemDone: (totaCount: Int) -> Unit
    ): Boolean {
        var count = 0

        return fileManager.copyItems(context, list, destination, preserveDate, overrideDisplayName) {
            count += 1
            onItemDone(count)
        }.size == list.size
    }

    fun renameItem(
        context: Context,
        uri: String,
        newName: String
    ) = fileManager.renameItem(context, uri, newName)

    suspend fun setTrashed(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        trashed: Boolean,
        onItemDone: (totaCount: Int) -> Unit
    ) = fileManager.setTrashed(context, list, trashed, null, null, onItemDone)

    suspend fun delete(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ) = fileManager.permanentlyDelete(context, list)

    suspend fun setFavourite(
        context: Context,
        favourite: Boolean,
        list: List<SelectionManager.SelectedItem>
    ) = fileManager.setFavourite(context, favourite, list)
}