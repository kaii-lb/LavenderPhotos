package com.kaii.photos.repositories

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.SettingsImmichImpl
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.models.loading.mapToMedia
import com.kaii.photos.models.loading.mapToSeparatedMedia
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

class MediaRepository(
    private val context: Context,
    albumInfo: AlbumInfo,
    scope: CoroutineScope,
    sortMode: MediaItemSortMode,
    format: DisplayDateFormat
) {
    private data class RoomQueryParams(
        val paths: Set<String>,
        val sortMode: MediaItemSortMode,
        val format: DisplayDateFormat,
        val accessToken: String,
        val separators: Boolean
    )

    private val mediaDao = MediaDatabase.getInstance(context).mediaDao()
    private val settings = SettingsImmichImpl(context = context, viewModelScope = scope)
    private var params = MutableStateFlow(
        value = RoomQueryParams(
            paths = albumInfo.paths,
            sortMode = sortMode,
            format = format,
            accessToken = "",
            separators = true
        )
    )

    init {
        scope.launch {
            settings.getImmichBasicInfo().collectLatest {
                params.value = params.value.copy(accessToken = it.accessToken)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val mediaFlow = params.flatMapLatest { params ->
        Pager(
            config = PagingConfig(
                pageSize = 50,
                prefetchDistance = 50,
                enablePlaceholders = true,
                initialLoadSize = 100
            ),
            pagingSourceFactory = {
                if (sortMode.isDateModified) mediaDao.getPagedMediaDateModified(paths = params.paths)
                else mediaDao.getPagedMediaDateTaken(paths = params.paths)
            }
        ).flow.mapToMedia(
            sortMode = params.sortMode,
            format = params.format,
            accessToken = params.accessToken,
            separators = false
        )
    }.cachedIn(scope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val gridMediaFlow = params.flatMapLatest { params ->
        mediaFlow.mapToSeparatedMedia(
            sortMode = params.sortMode,
            format = params.format
        )
    }.cachedIn(scope)

    fun update(
        album: AlbumInfo?,
        sortMode: MediaItemSortMode?,
        format: DisplayDateFormat?
    ) {
        val snapshot = params.value
        params.value = snapshot.copy(
            sortMode = sortMode ?: snapshot.sortMode,
            format = format ?: snapshot.format,
            paths = album?.paths ?: snapshot.paths
        )
    }
}