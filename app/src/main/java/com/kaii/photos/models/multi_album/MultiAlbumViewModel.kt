package com.kaii.photos.models.multi_album

import android.util.Log
import android.content.Context
import android.os.CancellationSignal
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.MainActivity.Companion.mainViewModel
import com.kaii.photos.datastore.MainPhotosList
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MultiAlbumDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

private const val TAG = "MULTI_ALBUM_VIEW_MODEL"

class MultiAlbumViewModel(
	context: Context,
	albums: List<String>,
	sortBy: MediaItemSortMode
) : ViewModel() {
	private var cancellationSignal = CancellationSignal()
    private var mediaStoreDataSource = mutableStateOf(initDataSource(context, albums, sortBy))

    val mediaFlow by derivedStateOf	{ getMediaDataFlow().value.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList()) }

    private fun getMediaDataFlow(): State<Flow<List<MediaStoreData>>> = derivedStateOf { mediaStoreDataSource.value.loadMediaStoreData().flowOn(Dispatchers.IO) }

    fun cancelMediaFlow() = cancellationSignal.cancel()

    fun reinitDataSource(
    	context: Context,
    	albums: List<String>,
    	sortBy: MediaItemSortMode
   	) {
   		cancelMediaFlow()
   		cancellationSignal = CancellationSignal()
    	mediaStoreDataSource.value = initDataSource(context, albums, sortBy)
    }

    private fun initDataSource(
    	context: Context,
    	albums: List<String>,
    	sortBy: MediaItemSortMode
   	) = run {
    	val query = mainViewModel.settings.MainPhotosList.getSQLiteQuery(albums)
    	Log.d(TAG, "query is $query")

		MultiAlbumDataSource(
			context = context,
			queryString = query,
			sortBy = sortBy,
			cancellationSignal = cancellationSignal
		)
   	}
}
