package com.kaii.photos.repositories

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.paging.mapToMedia
import com.kaii.photos.helpers.paging.mapToSeparatedMedia
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.withContext

class MediaRepository(
    context: Context,
    initialAlbumInfo: AlbumInfo,
    info: ImmichBasicInfo,
    scope: CoroutineScope,
    sortMode: MediaItemSortMode,
    format: DisplayDateFormat
) {
    private data class RoomQueryParams(
        val paths: Set<String>,
        val sortMode: MediaItemSortMode,
        val format: DisplayDateFormat,
        val accessToken: String
    )

    private val dao = MediaDatabase.getInstance(context.applicationContext).mediaDao()

    private val params = MutableStateFlow(
        value = RoomQueryParams(
            paths = initialAlbumInfo.paths,
            sortMode = sortMode,
            format = format,
            accessToken = ""
        )
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val mediaFlow = params.flatMapLatest { details ->
        Pager(
            config = PagingConfig(
                pageSize = 50,
                prefetchDistance = 50,
                enablePlaceholders = true,
                initialLoadSize = 100
            ),
            pagingSourceFactory = {
                if (details.sortMode.isDateModified) dao.getPagedMediaDateModified(paths = details.paths)
                else dao.getPagedMediaDateTaken(paths = details.paths)
            }
        ).flow.mapToMedia(accessToken = info.accessToken)
    }.cachedIn(scope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val gridMediaFlow = params.flatMapLatest { details ->
        mediaFlow.mapToSeparatedMedia(
            sortMode = details.sortMode,
            format = details.format
        )
    }.cachedIn(scope)

    fun update(
        album: AlbumInfo?,
        sortMode: MediaItemSortMode?,
        format: DisplayDateFormat?,
        accessToken: String?
    ) {
        val snapshot = params.value
        params.value = snapshot.copy(
            sortMode = sortMode ?: snapshot.sortMode,
            format = format ?: snapshot.format,
            paths = album?.paths ?: snapshot.paths,
            accessToken = accessToken ?: snapshot.accessToken
        )
    }

    suspend fun getMediaCount(): Int = withContext(Dispatchers.IO) {
        return@withContext dao.countMediaInPaths(paths = params.value.paths)
    }
}