package com.kaii.photos.repositories

import android.content.Context
import android.os.CancellationSignal
import androidx.compose.ui.util.fastMap
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.file_management.managers.LocalFileManager
import com.kaii.photos.file_management.secure.LocalSecureManager
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.paging.ListPagingSource
import com.kaii.photos.helpers.paging.mapToMedia
import com.kaii.photos.helpers.paging.mapToSeparatedMedia
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.TrashDataSource
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

class TrashRepository(
    db: MediaDatabase,
    client: ApiClient,
    scope: CoroutineScope,
    context: Context,
    sortMode: Flow<MediaItemSortMode>,
    format: Flow<DisplayDateFormat>,
    info: Flow<ImmichBasicInfo>
) : BaseRepo {
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

    override val fileManager = LocalFileManager(
        mediaDao = db.mediaDao(),
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
        secureManager = LocalSecureManager(
            secureDao = db.securedItemEntityDao()
        )
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
        ).flow.mapToMedia(
            auth = params.info.auth,
            endpoint = params.info.endpoint
        )
    }.cachedIn(scope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val gridMediaFlow = params.flatMapLatest { params ->
        mediaFlow.mapToSeparatedMedia(
            sortMode = if (params.sortMode.isDisabled) MediaItemSortMode.DisabledLastModified else MediaItemSortMode.DateModified,
            format = params.format
        )
    }.cachedIn(scope)

    fun cancel() = cancellationSignal.cancel()

    override suspend fun delete(
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
                immichUrl = it.immichUrl,
                isImage = it.type == MediaType.Image,
                parentPath = it.parentPath
            )
        }
    )

    override suspend fun getMediaCount(): Int {
        throw IllegalAccessException("This cannot and should not be called in a search context.")
    }

    override suspend fun getMediaSize(): Long {
        throw IllegalAccessException("This cannot and should not be called in a search context.")
    }

    override fun allowedAlbumTypesFor(moving: Boolean): List<KClass<out AlbumType>> {
        throw IllegalAccessException("This cannot and should not be called in a search context.")
    }

    override suspend fun renameAlbum(context: Context, newName: String) {
        throw IllegalAccessException("This cannot and should not be called in a search context.")
    }
}