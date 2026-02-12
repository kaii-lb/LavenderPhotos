package com.kaii.photos.repositories

import android.content.Context
import android.os.CancellationSignal
import androidx.compose.ui.util.fastMap
import androidx.core.net.toUri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.permanentlyDeletePhotoList
import com.kaii.photos.mediastore.TrashDataSource
import com.kaii.photos.models.loading.ListPagingSource
import com.kaii.photos.models.loading.mapToMedia
import com.kaii.photos.models.loading.mapToSeparatedMedia
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class TrashRepository(
    private val scope: CoroutineScope,
    private val info: ImmichBasicInfo,
    context: Context,
    sortMode: MediaItemSortMode,
    format: DisplayDateFormat
) {
    private val appContext = context.applicationContext

    private val cancellationSignal = CancellationSignal()
    private val dataSource =
        TrashDataSource(
            context = context,
            cancellationSignal = cancellationSignal
        )

    private fun getMediaDataFlow() = dataSource.loadMediaStoreData().flowOn(Dispatchers.IO)

    private val items = MutableStateFlow(emptyList<MediaStoreData>())

    init {
        scope.launch(Dispatchers.IO) {
            getMediaDataFlow().collectLatest { media ->
                items.value = media
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val mediaFlow = items.flatMapLatest { media ->
        Pager(
            config = PagingConfig(
                pageSize = 50,
                prefetchDistance = 50,
                enablePlaceholders = true,
                initialLoadSize = 100
            ),
            pagingSourceFactory = { ListPagingSource(media = media) }
        ).flow.mapToMedia(accessToken = info.accessToken)
    }.cachedIn(scope)

    val gridMediaFlow = mediaFlow.mapToSeparatedMedia(
        sortMode = sortMode,
        format = format
    ).cachedIn(scope)

    fun cancel() = cancellationSignal.cancel()

    fun deleteAll() =
        scope.launch(Dispatchers.IO) {
            permanentlyDeletePhotoList(
                context = appContext,
                list = dataSource.query().fastMap { it.uri.toUri() }
            )
        }
}