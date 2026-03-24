package com.kaii.photos.repositories

import android.content.Context
import android.os.CancellationSignal
import androidx.compose.ui.util.fastMap
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.daos.SyncTaskDao
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.file_management.LocalFileManager
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.paging.ListPagingSource
import com.kaii.photos.helpers.paging.mapToMedia
import com.kaii.photos.helpers.paging.mapToSeparatedMedia
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.TrashDataSource
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class TrashRepository(
    mediaDao: MediaDao,
    customDao: CustomEntityDao,
    syncTaskDao: SyncTaskDao,
    client: ApiClient,
    scope: CoroutineScope,
    context: Context,
    sortMode: Flow<MediaItemSortMode>,
    format: Flow<DisplayDateFormat>,
    info: Flow<ImmichBasicInfo>
) {
    private data class Params(
        val items: List<MediaStoreData>,
        override val sortMode: MediaItemSortMode,
        override val format: DisplayDateFormat,
        override val info: ImmichBasicInfo
    ) : RoomQueryParams(sortMode, format, info)

    private val cancellationSignal = CancellationSignal()
    private val dataSource =
        TrashDataSource(
            context = context,
            cancellationSignal = cancellationSignal
        )

    private var fileManager = LocalFileManager(
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

    private fun getMediaDataFlow() = dataSource.loadMediaStoreData().flowOn(Dispatchers.IO)

    private val items = MutableStateFlow(emptyList<MediaStoreData>())

    private val params = combine(info, sortMode, format, items) { info, sortMode, format, items ->
        Params(
            items = items,
            sortMode = sortMode,
            format = format,
            info = info
        )
    }

    init {
        scope.launch(Dispatchers.IO) {
            getMediaDataFlow().collectLatest { media ->
                items.value = media
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val mediaFlow = params.flatMapLatest { params ->
        Pager(
            config = PagingConfig(
                pageSize = 50,
                prefetchDistance = 50,
                enablePlaceholders = true,
                initialLoadSize = 100
            ),
            pagingSourceFactory = { ListPagingSource(media = params.items) }
        ).flow.mapToMedia(accessToken = params.info.accessToken)
    }.cachedIn(scope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val gridMediaFlow = params.flatMapLatest { params ->
        mediaFlow.mapToSeparatedMedia(
            sortMode = if (params.sortMode.isDisabled) MediaItemSortMode.DisabledLastModified else MediaItemSortMode.DateModified,
            format = params.format
        )
    }.cachedIn(scope)

    fun cancel() = cancellationSignal.cancel()

    suspend fun setTrashed(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        trash: Boolean,
        onItemDone: (totaCount: Int) -> Unit
    ) = fileManager.setTrashed(context, list, trash, null, null, onItemDone)

    suspend fun delete(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ) = fileManager.permanentlyDelete(context, list)

    suspend fun deleteAll(
        context: Context
    ) = fileManager.permanentlyDelete(
        context,
        dataSource.query().fastMap {
            SelectionManager.SelectedItem(
                id = it.id,
                uri = it.uri,
                isImage = it.type == MediaType.Image,
                parentPath = it.parentPath
            )
        }
    )
}