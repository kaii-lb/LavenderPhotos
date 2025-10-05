package com.kaii.photos.models.search_page

import android.content.Context
import android.os.CancellationSignal
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.datastore.SQLiteQuery
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.StreamingDataSource
import com.kaii.photos.models.multi_album.DisplayDateFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

class SearchViewModel(
    context: Context,
    sortBy: MediaItemSortMode,
    displayDateFormat: DisplayDateFormat
) : ViewModel() {
    private val cancellationSignal = CancellationSignal()
    private val mediaStoreDataSource =
        StreamingDataSource(
            context = context,
            queryString = SQLiteQuery(query = "", paths = null, includedBasePaths = null),
            sortBy = sortBy,
            cancellationSignal = cancellationSignal,
            displayDateFormat = displayDateFormat
        )

    val mediaFlow by lazy {
        getMediaDataFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getMediaDataFlow(): Flow<List<MediaStoreData>> =
        mediaStoreDataSource.loadMediaStoreData().flowOn(Dispatchers.IO).flatMapMerge { it.flowOn(Dispatchers.IO) }.flowOn(Dispatchers.IO)
}
