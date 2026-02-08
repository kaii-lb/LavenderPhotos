package com.kaii.photos.repositories

import android.content.Context
import android.os.CancellationSignal
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.mediastore.content_provider.CustomAlbumDataSource
import com.kaii.photos.mediastore.content_provider.LavenderContentProvider
import com.kaii.photos.mediastore.content_provider.LavenderMediaColumns
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

class CustomRepository(
    private val scope: CoroutineScope,
    context: Context,
    albumInfo: AlbumInfo,
    info: ImmichBasicInfo,
    sortMode: MediaItemSortMode,
    format: DisplayDateFormat
) {
    private val appContext = context.applicationContext
    private val cancellationSignal = CancellationSignal()
    private val dataSource =
        CustomAlbumDataSource(
            context = appContext,
            parentId = albumInfo.id,
            sortMode = sortMode,
            cancellationSignal = cancellationSignal
        )

    private fun getMediaDataFlow() = dataSource.loadMediaStoreData().flowOn(Dispatchers.IO)

    private val media = MutableStateFlow(emptyList<MediaStoreData>())

    init {
        scope.launch(Dispatchers.IO) {
            getMediaDataFlow().collectLatest { items ->
                media.value = items
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val mediaFlow = media.flatMapLatest { items ->
        Pager(
            config = PagingConfig(
                pageSize = 50,
                prefetchDistance = 50,
                enablePlaceholders = true,
                initialLoadSize = 100
            ),
            pagingSourceFactory = { ListPagingSource(media = items) }
        ).flow.mapToMedia(accessToken = info.accessToken)
    }.cachedIn(scope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val gridMediaFlow = mediaFlow.mapToSeparatedMedia(
        sortMode = sortMode,
        format = format
    ).cachedIn(scope)

    fun cancel() = cancellationSignal.cancel()

    fun remove(
        items: Set<MediaStoreData>,
        albumId: Int
    ) = scope.launch(Dispatchers.IO) {
        items.forEach { item ->
            appContext.contentResolver.delete(
                LavenderContentProvider.CONTENT_URI,
                "${LavenderMediaColumns.ID} = ? AND ${LavenderMediaColumns.PARENT_ID} = ?",
                arrayOf(item.customId.toString(), albumId.toString())
            )
        }
    }
}