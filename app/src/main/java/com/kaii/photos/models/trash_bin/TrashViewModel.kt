package com.kaii.photos.models.trash_bin

import android.content.Context
import android.os.CancellationSignal
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.mediastore.TrashDataSource
import com.kaii.photos.models.multi_album.mapToMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration.Companion.seconds

class TrashViewModel(
    context: Context,
    sortMode: MediaItemSortMode,
    displayDateFormat: DisplayDateFormat
) : ViewModel() {
    private val cancellationSignal = CancellationSignal()
    private val mediaStoreDataSource =
        TrashDataSource(
            context = context,
            sortMode = sortMode,
            cancellationSignal = cancellationSignal,
            displayDateFormat = displayDateFormat
        )

    val mediaFlow by lazy {
        getMediaDataFlow().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = 10.seconds.inWholeMilliseconds
            ),
            initialValue = emptyList()
        )
    }

    // TODO
    val mediaPagingFlow = Pager(
        config = PagingConfig(
            pageSize = 80,
            prefetchDistance = 40,
            enablePlaceholders = true,
            initialLoadSize = 80
        ),
        pagingSourceFactory = { MediaDatabase.getInstance(context).mediaDao().getPagedMedia() }
    ).flow.mapToMedia(sortMode = sortMode, format = displayDateFormat).cachedIn(viewModelScope)

    private fun getMediaDataFlow(): Flow<List<MediaStoreData>> {
        return mediaStoreDataSource.loadMediaStoreData().flowOn(Dispatchers.IO)
    }

    fun cancelMediaFlow() {
        cancellationSignal.cancel()
    }
}
