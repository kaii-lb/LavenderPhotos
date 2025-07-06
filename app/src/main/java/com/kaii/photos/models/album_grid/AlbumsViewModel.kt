package com.kaii.photos.models.album_grid

import android.content.Context
import android.os.CancellationSignal
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.mediastore.AlbumStoreDataSource
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.getSQLiteQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

class AlbumsViewModel(context: Context, var albumInfo: List<AlbumInfo>) : ViewModel() {
    private var cancellationSignal = CancellationSignal()
    private val mediaStoreDataSource =
        mutableStateOf(initDataSource(context = context, albums = albumInfo))

    val mediaFlow by derivedStateOf {
        getMediaDataFlow().value.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = Long.MAX_VALUE
            ),
            initialValue = emptyList()
        )
    }

    private fun getMediaDataFlow(): State<Flow<List<Pair<AlbumInfo, MediaStoreData>>>> =
        derivedStateOf {
            mediaStoreDataSource.value.loadMediaStoreData().flowOn(Dispatchers.IO)
        }

    fun refresh(
        context: Context,
        albums: List<AlbumInfo>
    ) {
        if (albums.toSet() == albumInfo.toSet()) return

        cancellationSignal.cancel()
        cancellationSignal = CancellationSignal()

        mediaStoreDataSource.value = initDataSource(context = context, albums = albums)
    }

    private fun initDataSource(
        context: Context,
        albums: List<AlbumInfo>
    ) = run {
        albumInfo = albums
        val queries = albums.map { album ->
            val query = getSQLiteQuery(albums = album.paths)

            Pair(album, query.copy(includedBasePaths = album.paths))
        }

        AlbumStoreDataSource(
            context = context,
            albumQueryPairs = queries,
            cancellationSignal = cancellationSignal
        )
    }
}
