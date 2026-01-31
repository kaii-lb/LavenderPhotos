package com.kaii.photos.models.custom_album

import android.content.Context
import android.os.CancellationSignal
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.mediastore.content_provider.CustomAlbumDataSource
import com.kaii.photos.models.multi_album.mapToMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration.Companion.seconds

// private const val TAG = "com.kaii.photos.models.CustomAlbumViewModel"

class CustomAlbumViewModel(
    context: Context,
    var albumInfo: AlbumInfo,
    var sortMode: MediaItemSortMode,
    var displayDateFormat: DisplayDateFormat
) : ViewModel() {
    private var cancellationSignal = CancellationSignal()
    private val mediaStoreDataSource = mutableStateOf(
        initDataSource(
            context = context,
            album = albumInfo,
            sortMode = sortMode,
            displayDateFormat = displayDateFormat
        )
    )

    val mediaFlow by derivedStateOf {
        getMediaDataFlow().value.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = 300.seconds.inWholeMilliseconds
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

    private fun getMediaDataFlow() = derivedStateOf {
        mediaStoreDataSource.value.loadMediaStoreData().flowOn(Dispatchers.IO)
    }

    fun cancelMediaFlow() {
        cancellationSignal.cancel()
        cancellationSignal = CancellationSignal()
    }

    fun update(
        context: Context,
        album: AlbumInfo
    ) {
        if (album.id != this.albumInfo.id) {
            reinitDataSource(
                context = context,
                album = album,
                sortMode = sortMode,
                displayDateFormat = displayDateFormat
            )
        }
    }

    fun reinitDataSource(
        context: Context,
        album: AlbumInfo,
        sortMode: MediaItemSortMode = this.sortMode,
        displayDateFormat: DisplayDateFormat = this.displayDateFormat
    ) {
        if (album == albumInfo && sortMode == this.sortMode && displayDateFormat == this.displayDateFormat) return

        this.sortMode = sortMode
        this.displayDateFormat = displayDateFormat

        cancelMediaFlow()
        mediaStoreDataSource.value =
            initDataSource(
                context = context,
                album = album,
                sortMode = sortMode,
                displayDateFormat = displayDateFormat
            )
    }

    fun changeSortMode(
        context: Context,
        sortMode: MediaItemSortMode
    ) {
        if (this.sortMode == sortMode) return

        this.sortMode = sortMode

        cancelMediaFlow()
        mediaStoreDataSource.value =
            initDataSource(
                context = context,
                album = this.albumInfo,
                sortMode = sortMode,
                displayDateFormat = this.displayDateFormat
            )
    }

    fun changeDisplayDateFormat(
        context: Context,
        displayDateFormat: DisplayDateFormat
    ) {
        if (this.displayDateFormat == displayDateFormat) return

        this.displayDateFormat = displayDateFormat

        cancelMediaFlow()
        mediaStoreDataSource.value =
            initDataSource(
                context = context,
                album = this.albumInfo,
                sortMode = this.sortMode,
                displayDateFormat = displayDateFormat
            )
    }

    private fun initDataSource(
        context: Context,
        album: AlbumInfo,
        sortMode: MediaItemSortMode,
        displayDateFormat: DisplayDateFormat
    ) = run {
        this.albumInfo = album
        this.sortMode = sortMode
        this.displayDateFormat = displayDateFormat

        CustomAlbumDataSource(
            context = context,
            parentId = album.id,
            sortMode = sortMode,
            cancellationSignal = cancellationSignal,
            displayDateFormat = displayDateFormat
        )
    }

    override fun onCleared() {
        super.onCleared()
        cancelMediaFlow()
    }
}