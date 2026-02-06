package com.kaii.photos.repositories

import android.content.Context
import android.os.CancellationSignal
import androidx.compose.ui.util.fastMap
import androidx.core.net.toUri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.SettingsImmichImpl
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.permanentlyDeletePhotoList
import com.kaii.photos.mediastore.TrashDataSource
import com.kaii.photos.models.loading.ListPagingSource
import com.kaii.photos.models.loading.mapToMedia
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class TrashRepository(
    private val context: Context,
    private val scope: CoroutineScope,
    sortMode: MediaItemSortMode,
    format: DisplayDateFormat,
    separators: Boolean
) {
    // TODO: cleanup when moving to room's trash table
    private data class TrashFlowParams(
        val items: List<MediaStoreData>,
        val sortMode: MediaItemSortMode,
        val format: DisplayDateFormat,
        val separators: Boolean,
        val accessToken: String
    )

    private val cancellationSignal = CancellationSignal()
    private val settings = SettingsImmichImpl(context = context, viewModelScope = scope)
    private val dataSource =
        TrashDataSource(
            context = context,
            cancellationSignal = cancellationSignal
        )

    private fun getMediaDataFlow() = dataSource.loadMediaStoreData().flowOn(Dispatchers.IO)

    private val params = MutableStateFlow(
        TrashFlowParams(
            items = emptyList(),
            sortMode = sortMode,
            format = format,
            separators = separators,
            accessToken = ""
        )
    )

    init {
        scope.launch(Dispatchers.IO) {
            getMediaDataFlow().collectLatest { items ->
                params.value = params.value.copy(items = items)
            }
        }

        scope.launch {
            settings.getImmichBasicInfo().collectLatest {
                params.value = params.value.copy(accessToken = it.accessToken)
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
            sortMode = params.sortMode,
            format = params.format,
            accessToken = params.accessToken,
            separators = params.separators
        )
    }.cachedIn(scope)

    fun cancel() = cancellationSignal.cancel()

    fun updateParams(
        sortMode: MediaItemSortMode?,
        format: DisplayDateFormat?,
        separators: Boolean?
    ) {
        val snapshot = params.value
        params.value = snapshot.copy(
            sortMode = sortMode ?: snapshot.sortMode,
            format = format ?: snapshot.format,
            separators = separators ?: snapshot.separators
        )
    }

    fun deleteAll() =
        scope.launch(Dispatchers.IO) {
            permanentlyDeletePhotoList(
                context = context,
                list = dataSource.query().fastMap { it.uri.toUri() }
            )
        }
}