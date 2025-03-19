package com.kaii.photos.models.album_grid

import android.content.Context
import android.os.CancellationSignal
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.mediastore.AlbumStoreDataSource
import com.kaii.photos.mediastore.MediaStoreData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

class AlbumsViewModel(context: Context, private val pathList: List<String>) : ViewModel() {
    private var cancellationSignal = CancellationSignal()
    private val mediaStoreDataSource = mutableStateOf(initDataSource(context, pathList))

    val mediaFlow by derivedStateOf {
        getMediaDataFlow().value.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    }

    private fun getMediaDataFlow(): State<Flow<List<MediaStoreData>>> = derivedStateOf {
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
    ) = run {
    	cancellationSignal.cancel()
    	cancellationSignal = CancellationSignal()

        AlbumStoreDataSource(
            context = context,
            albumPaths = paths,
            cancellationSignal = cancellationSignal
        )
    }
}
