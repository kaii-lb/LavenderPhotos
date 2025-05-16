package com.aks_labs.tulsi.models.custom_album

import android.content.Context
import android.os.CancellationSignal
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aks_labs.tulsi.MainActivity.Companion.mainViewModel
import com.aks_labs.tulsi.datastore.AlbumInfo
import com.aks_labs.tulsi.datastore.MainPhotosView
import com.aks_labs.tulsi.helpers.MediaItemSortMode
import com.aks_labs.tulsi.mediastore.MediaStoreData
import com.aks_labs.tulsi.mediastore.content_provider.CustomAlbumDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

private const val TAG = "MULTI_ALBUM_VIEW_MODEL"

class CustomAlbumViewModel(
    context: Context,
    var albumInfo: AlbumInfo,
    var sortBy: MediaItemSortMode
) : ViewModel() {
    private var cancellationSignal = CancellationSignal()
    private val mediaStoreDataSource = mutableStateOf(initDataSource(context, albumInfo, sortBy))

    val mediaFlow by derivedStateOf {
        getMediaDataFlow().value.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            emptyList()
        )
    }

    private fun getMediaDataFlow(): State<Flow<List<MediaStoreData>>> = derivedStateOf {
        mediaStoreDataSource.value.loadMediaStoreData().flowOn(Dispatchers.IO)
    }

    fun cancelMediaFlow() = cancellationSignal.cancel()

    fun reinitDataSource(
        context: Context,
        album: AlbumInfo,
        sortMode: MediaItemSortMode = sortBy
    ) {
        sortBy = sortMode
        if (album == albumInfo) return

        cancelMediaFlow()
        cancellationSignal = CancellationSignal()
        mediaStoreDataSource.value = initDataSource(context, album, sortBy)
    }

    fun changeSortMode(
        context: Context,
        sortMode: MediaItemSortMode
    ) {
        sortBy = sortMode

        cancelMediaFlow()
        cancellationSignal = CancellationSignal()
        mediaStoreDataSource.value = initDataSource(context, albumInfo, sortBy)
    }

    private fun initDataSource(
        context: Context,
        album: AlbumInfo,
        sortBy: MediaItemSortMode
    ) = run {
        val query = mainViewModel.settings.MainPhotosView.getSQLiteQuery(album.paths)
        Log.d(TAG, "query is $query")

        this.albumInfo = album
        this.sortBy = sortBy

        CustomAlbumDataSource(
            context = context,
            parentId = album.id,
            sortBy = sortBy,
            cancellationSignal = cancellationSignal
        )
    }
}

