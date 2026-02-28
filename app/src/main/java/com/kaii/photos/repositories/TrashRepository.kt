package com.kaii.photos.repositories

import android.content.Context
import android.os.CancellationSignal
import androidx.compose.ui.util.fastMap
import androidx.core.net.toUri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.paging.ListPagingSource
import com.kaii.photos.helpers.paging.mapToMedia
import com.kaii.photos.helpers.paging.mapToSeparatedMedia
import com.kaii.photos.helpers.permanentlyDeletePhotoList
import com.kaii.photos.mediastore.TrashDataSource
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
import kotlinx.coroutines.withContext

class TrashRepository(
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
        override val accessToken: String
    ) : RoomQueryParams(sortMode, format, accessToken)

    private val appContext = context.applicationContext

    private val cancellationSignal = CancellationSignal()
    private val dataSource =
        TrashDataSource(
            context = context,
            cancellationSignal = cancellationSignal
        )

    private fun getMediaDataFlow() = dataSource.loadMediaStoreData().flowOn(Dispatchers.IO)

    private val items = MutableStateFlow(emptyList<MediaStoreData>())

    private val params = combine(info, sortMode, format, items) { info, sortMode, format, items ->
        Params(
            items = items,
            accessToken = info.accessToken,
            sortMode = sortMode,
            format = format
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
        ).flow.mapToMedia(accessToken = params.accessToken)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val gridMediaFlow = params.flatMapLatest { params ->
        mediaFlow.mapToSeparatedMedia(
            sortMode = if (params.sortMode.isDisabled) MediaItemSortMode.DisabledLastModified else MediaItemSortMode.DateModified,
            format = params.format
        )
    }

    fun cancel() = cancellationSignal.cancel()

    suspend fun deleteAll() =
        withContext(Dispatchers.IO) {
            permanentlyDeletePhotoList(
                context = appContext,
                list = dataSource.query().fastMap { it.uri.toUri() }
            )
        }
}