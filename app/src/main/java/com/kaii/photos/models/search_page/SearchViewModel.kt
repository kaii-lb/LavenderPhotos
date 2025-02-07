package com.kaii.photos.models.search_page

import android.content.Context
import android.os.CancellationSignal
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MultiAlbumDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

class SearchViewModel(context: Context, sortBy: MediaItemSortMode) : ViewModel() {
	private val cancellationSignal = CancellationSignal()
    private val mediaStoreDataSource =
				    MultiAlbumDataSource(
				    	context = context,
				    	queryString = Pair("", null),
				    	sortBy = sortBy,
				    	cancellationSignal = cancellationSignal
				    )

    val mediaFlow by lazy {
        getMediaDataFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    }

    private fun getMediaDataFlow(): Flow<List<MediaStoreData>> = mediaStoreDataSource.loadMediaStoreData().flowOn(Dispatchers.IO)

    fun cancelMediaFlow() = cancellationSignal.cancel()
}
