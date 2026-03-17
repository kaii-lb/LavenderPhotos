package com.kaii.photos.repositories

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.daos.SyncTaskDao
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.file_management.GenericFileManager
import com.kaii.photos.helpers.file_management.LocalFileManager
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class RoomQueryParams(
    open val sortMode: MediaItemSortMode,
    open val format: DisplayDateFormat,
    open val info: ImmichBasicInfo
)

@OptIn(ExperimentalCoroutinesApi::class)
class MediaRepository(
    private val mediaDao: MediaDao,
    private val album: AlbumType.Folder,
    customDao: CustomEntityDao,
    syncTaskDao: SyncTaskDao,
    client: ApiClient,
    scope: CoroutineScope,
    initialAlbum: AlbumType.Folder,
    info: Flow<ImmichBasicInfo>,
    sortMode: Flow<MediaItemSortMode>,
    format: Flow<DisplayDateFormat>
) {
    private data class Params(
        val paths: Set<String>,
        override val sortMode: MediaItemSortMode,
        override val format: DisplayDateFormat,
        override val info: ImmichBasicInfo
    ) : RoomQueryParams(sortMode, format, info)

    private val paths = MutableStateFlow(initialAlbum.paths)
    private val params = combine(info, sortMode, format, paths) { info, sortMode, format, paths ->
        Params(
            paths = paths,
            sortMode = sortMode,
            format = format,
            info = info
        )
    }

    private var fileManager = LocalFileManager(
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
        accessToken = ""
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val mediaFlow = params.flatMapLatest { details ->
        Pager(
            config = PagingConfig(
                pageSize = 50,
                prefetchDistance = 50,
                enablePlaceholders = true,
                initialLoadSize = 100
            ),
            pagingSourceFactory = {
                if (details.sortMode.isDateModified) mediaDao.getPagedMediaDateModified(paths = details.paths)
                else mediaDao.getPagedMediaDateTaken(paths = details.paths)
            }
        ).flow.mapToMedia(accessToken = details.info.accessToken)
    }.cachedIn(scope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val gridMediaFlow = params.flatMapLatest { details ->
        mediaFlow.mapToSeparatedMedia(
            sortMode = details.sortMode,
            format = details.format
        )
    }.cachedIn(scope)

    fun changePaths(new: Set<String>) {
        paths.value = new
    }

    suspend fun getMediaCount(): Int = withContext(Dispatchers.IO) {
        return@withContext mediaDao.countMediaInPaths(paths = paths.value)
    }

    suspend fun getMediaSize(): Long = withContext(Dispatchers.IO) {
        return@withContext mediaDao.mediaSize(paths = paths.value)
    }

    init {
        scope.launch {
            params.mapLatest { it.info }
                .distinctUntilChanged()
                .collectLatest { info ->
                    fileManager = LocalFileManager(
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
                        accessToken = info.accessToken
                    )
                }
        }
    }

    fun allowedAlbumTypesFor(
        action: GenericFileManager.Action
    ) = fileManager.allowedAlbumTypesFor(
        action = action,
        current = AlbumType.Folder::class
    )

    suspend fun copy(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: String,
        preserveDate: Boolean,
        overrideDisplayName: ((displayName: String) -> String)?,
        onItemDone: (totaCount: Int) -> Unit
    ) = fileManager.copyItems(context, list, album.id, AlbumType.Folder::class, destination, preserveDate, overrideDisplayName, onItemDone)

    suspend fun move(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: String,
        preserveDate: Boolean,
        onItemDone: (totalCount: Int) -> Unit
    ) = fileManager.moveItems(context, list, album.id, AlbumType.Folder::class, destination, preserveDate, onItemDone)

    fun renameItem(
        context: Context,
        uri: String,
        newName: String
    ) = fileManager.renameItem(context, uri, newName)

    suspend fun renameDirectory(
        context: Context,
        newName: String
    ) = fileManager.renameAlbum(context, album, newName)

    suspend fun setTrashed(
        context: Context,
        list: List<String>,
        trashed: Boolean,
        onItemDone: (totaCount: Int) -> Unit
    ) = fileManager.setTrashed(context, list, trashed, null, onItemDone)

    suspend fun setFavourite(
        context: Context,
        favourite: Boolean,
        list: List<String>,
        onItemDone: (totaCount: Int) -> Unit
    ) = fileManager.setFavourite(context, favourite, list, onItemDone)
}