package com.kaii.photos.models.custom_album

import android.content.Context
import android.os.CancellationSignal
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.content_provider.CustomAlbumDataSource
import com.kaii.photos.mediastore.getSQLiteQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

private const val TAG = "com.kaii.photos.models.CustomAlbumViewModel"

class CustomAlbumViewModel(
    context: Context,
    var albumInfo: AlbumInfo,
    var sortMode: MediaItemSortMode,
    var displayDateFormat: DisplayDateFormat
) : ViewModel() {
    private var cancellationSignal = CancellationSignal()
    private val mediaStoreDataSource = mutableStateOf(initDataSource(context, albumInfo, sortMode, displayDateFormat))

    val mediaFlow by derivedStateOf {
        getMediaDataFlow().value.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = 300000
            ),
            initialValue = emptyList()
        )
    }

    private fun getMediaDataFlow(): State<Flow<List<MediaStoreData>>> = derivedStateOf {
        mediaStoreDataSource.value.loadMediaStoreData().flowOn(Dispatchers.IO).flowOn(Dispatchers.IO)
    }

    fun cancelMediaFlow() = cancellationSignal.cancel()

    fun reinitDataSource(
        context: Context,
        album: AlbumInfo,
        sortMode: MediaItemSortMode = this.sortMode,
        displayDateFormat: DisplayDateFormat = this.displayDateFormat
    ) {
        this.sortMode = sortMode
        this.displayDateFormat = displayDateFormat

        if (album == albumInfo) return

        cancelMediaFlow()
        cancellationSignal = CancellationSignal()
        mediaStoreDataSource.value = initDataSource(context, album, this.sortMode, this.displayDateFormat)
    }

    fun changeSortMode(
        context: Context,
        sortMode: MediaItemSortMode
    ) {
        if (this.sortMode == sortMode) return

        this.sortMode = sortMode

        cancelMediaFlow()
        cancellationSignal = CancellationSignal()
        mediaStoreDataSource.value = initDataSource(context, this.albumInfo, this.sortMode, this.displayDateFormat)
    }

    fun changeDisplayDateFormat(
        context: Context,
        displayDateFormat: DisplayDateFormat
    ) {
        if (this.displayDateFormat == displayDateFormat) return

        this.displayDateFormat = displayDateFormat

        cancelMediaFlow()
        cancellationSignal = CancellationSignal()
        mediaStoreDataSource.value = initDataSource(context, this.albumInfo, this.sortMode, displayDateFormat)
    }

    private fun initDataSource(
        context: Context,
        album: AlbumInfo,
        sortBy: MediaItemSortMode,
        displayDateFormat: DisplayDateFormat
    ) = run {
        val query = getSQLiteQuery(album.paths)
        Log.d(TAG, "query is $query")

        this.albumInfo = album
        this.sortMode = sortBy
        this.displayDateFormat = displayDateFormat

        CustomAlbumDataSource(
            context = context,
            parentId = album.id,
            sortMode = sortBy,
            cancellationSignal = this.cancellationSignal,
            displayDateFormat = displayDateFormat
        )
    }
}