package com.kaii.photos.models.album_grid

import android.content.Context
import android.os.CancellationSignal
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.AlbumStoreDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

class AlbumsViewModel(context: Context, val pathList: List<String>) : ViewModel() {
	private var cancellationSignal = CancellationSignal()
    private val mediaStoreDataSource = mutableStateOf(initDataSource(context, pathList))

    val mediaFlow by derivedStateOf	{
		getMediaDataFlow().value.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyMap())
	}

    private fun getMediaDataFlow(): State<Flow<LinkedHashMap<String, MediaStoreData>>> = derivedStateOf {
		mediaStoreDataSource.value.loadMediaStoreData().flowOn(Dispatchers.IO)
	}

    fun refresh(
    	context: Context,
    	albumsList: List<String>
   	) {
    	if (albumsList == pathList) return

		cancellationSignal.cancel()
    	cancellationSignal = CancellationSignal()
    	mediaStoreDataSource.value = initDataSource(context, albumsList)
    }

    private fun initDataSource(
    	context: Context,
    	paths: List<String>
    ) = AlbumStoreDataSource(
    	context = context,
    	multiplePaths = paths,
    	cancellationSignal = cancellationSignal
    )
}
