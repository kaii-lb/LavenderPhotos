package com.kaii.photos.repositories

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.file_management.managers.HybridFileManager
import com.kaii.photos.file_management.secure.LocalSecureManager
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.paging.mapToMedia
import com.kaii.photos.helpers.paging.mapToSeparatedMedia
import io.github.kaii_lb.lavender.immichintegration.Auth
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

@OptIn(ExperimentalCoroutinesApi::class)
class HybridRepository(
    db: MediaDatabase,
    client: ApiClient,
    scope: CoroutineScope,
    initialAlbum: AlbumType.Folder,
    info: Flow<ImmichBasicInfo>,
    sortMode: Flow<MediaItemSortMode>,
    format: Flow<DisplayDateFormat>
) : BaseRepo {
    private data class Params(
        val paths: Set<String>,
        override val sortMode: MediaItemSortMode,
        override val format: DisplayDateFormat,
        override val info: ImmichBasicInfo
    ) : RoomQueryParams(sortMode, format, info)

    private val mediaDao = db.mediaDao()

    private val album = MutableStateFlow(initialAlbum)
    private val params = combine(info, sortMode, format, album) { info, sortMode, format, album ->
        Params(
            paths = album.paths,
            sortMode = sortMode,
            format = format,
            info = info
        )
    }

    override val fileManager = HybridFileManager(
        isCustom = false,
        mediaDao = mediaDao,
        customDao = db.customDao(),
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
        ),
        localSecureManager = LocalSecureManager(
            secureDao = db.securedItemEntityDao()
        )
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    override val mediaFlow = params.flatMapLatest { details ->
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
        ).flow.mapToMedia(
            auth = details.info.auth,
            endpoint = details.info.endpoint
        )
    }.cachedIn(scope)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val gridMediaFlow = params.flatMapLatest { details ->
        mediaFlow.mapToSeparatedMedia(
            sortMode = details.sortMode,
            format = details.format
        )
    }.cachedIn(scope)

    fun changeAlbum(album: AlbumType.Folder) {
        this.album.value = album
    }

    init {
        scope.launch {
            params.mapLatest { it.info }
                .distinctUntilChanged()
                .collectLatest { info ->
                    fileManager.setEndpoint(info.endpoint)
                    fileManager.setAuth(info.auth)
                }
        }
    }

    override fun allowedAlbumTypesFor(
        moving: Boolean
    ) = fileManager.allowedAlbumTypesFor(
        moving = moving,
        current = AlbumType.Folder::class
    )

    override suspend fun getMediaCount(): Int = withContext(Dispatchers.IO) {
        return@withContext mediaDao.countMediaInPaths(paths = album.value.paths)
    }

    override suspend fun getMediaSize(): Long = withContext(Dispatchers.IO) {
        return@withContext mediaDao.mediaSize(paths = album.value.paths)
    }

    override suspend fun renameAlbum(
        context: Context,
        newName: String
    ) = fileManager.renameAlbum(context, album.value, newName)

    override suspend fun setTrashed(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        trashed: Boolean,
        albumId: String?,
        immichId: String?,
        onItemDone: (totaCount: Int) -> Unit
    ): Boolean {
        return super.setTrashed(context, list, trashed, albumId ?: album.value.id, immichId ?: album.value.immichId, onItemDone)
    }
}