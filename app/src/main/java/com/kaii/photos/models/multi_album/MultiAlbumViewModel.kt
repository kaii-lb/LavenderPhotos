package com.kaii.photos.models.multi_album

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.SettingsImmichImpl
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.models.loading.mapToMedia
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// private const val TAG = "com.kaii.photos.models.MultiAlbumViewModel"

data class RoomQueryParams(
    val paths: Set<String>,
    val sortMode: MediaItemSortMode,
    val format: DisplayDateFormat,
    val accessToken: String,
    val separators: Boolean
)

class MultiAlbumViewModel(
    context: Context,
    albumInfo: AlbumInfo,
    sortMode: MediaItemSortMode,
    format: DisplayDateFormat
) : ViewModel() {
    private val settings = SettingsImmichImpl(context = context, viewModelScope = viewModelScope)
    private var _params = MutableStateFlow(
        value = RoomQueryParams(
            paths = albumInfo.paths,
            sortMode = sortMode,
            format = format,
            accessToken = "",
            separators = true
        )
    )

    init {
        viewModelScope.launch {
            settings.getImmichBasicInfo().collectLatest {
                _params.value = _params.value.copy(accessToken = it.accessToken)
            }
        }
    }

    private val params = _params
        .asStateFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = RoomQueryParams(
                paths = albumInfo.paths,
                sortMode = sortMode,
                format = format,
                accessToken = "",
                separators = true
            )
        )

    private val mediaDao = MediaDatabase.getInstance(context).mediaDao()

    @OptIn(ExperimentalCoroutinesApi::class)
    val mediaFlow = params.flatMapLatest { (paths, sortMode, format, accessToken, separators) ->
        Pager(
            config = PagingConfig(
                pageSize = 100,
                enablePlaceholders = true,
                initialLoadSize = 300
            ),
            pagingSourceFactory = {
                if (sortMode.isDateModified) mediaDao.getPagedMediaDateModified(paths = paths)
                else mediaDao.getPagedMediaDateTaken(paths = paths)
            }
        ).flow.mapToMedia(
            sortMode = sortMode,
            format = format,
            accessToken = accessToken,
            separators = separators
        )
    }.cachedIn(viewModelScope)

    fun update(album: AlbumInfo) {
        if (album.paths != _params.value.paths) {
            _params.value = _params.value.copy(paths = album.paths)
        }
    }

    fun changeSortMode(sortMode: MediaItemSortMode) {
        _params.value = _params.value.copy(sortMode = sortMode)
    }

    fun changeDisplayDateFormat(format: DisplayDateFormat) {
        _params.value = _params.value.copy(format = format)
    }

    fun changeSeparator(use: Boolean) {
        _params.value = _params.value.copy(separators = use)
    }
}

