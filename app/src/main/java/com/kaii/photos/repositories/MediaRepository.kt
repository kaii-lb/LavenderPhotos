package com.kaii.photos.repositories

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.paging.mapToMedia
import com.kaii.photos.helpers.paging.mapToSeparatedMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.withContext

open class RoomQueryParams(
    open val sortMode: MediaItemSortMode,
    open val format: DisplayDateFormat,
    open val accessToken: String
)

class MediaRepository(
    private val dao: MediaDao,
    initialAlbumInfo: AlbumInfo,
    info: Flow<ImmichBasicInfo>,
    sortMode: Flow<MediaItemSortMode>,
    format: Flow<DisplayDateFormat>
) {
    private data class Params(
        val paths: Set<String>,
        override val sortMode: MediaItemSortMode,
        override val format: DisplayDateFormat,
        override val accessToken: String
    ) : RoomQueryParams(sortMode, format, accessToken)

    private val paths = MutableStateFlow(initialAlbumInfo.paths)
    private val params = combine(info, sortMode, format, paths) { info, sortMode, format, paths ->
        Params(
            paths = paths,
            accessToken = info.accessToken,
            sortMode = sortMode,
            format = format
        )
    }

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
        ).flow.mapToMedia(accessToken = details.accessToken)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val gridMediaFlow = params.flatMapLatest { details ->
        mediaFlow.mapToSeparatedMedia(
            sortMode = details.sortMode,
            format = details.format
        )
    }

    fun changePaths(new: Set<String>) {
        paths.value = new
    }

    suspend fun getMediaCount(): Int = withContext(Dispatchers.IO) {
        return@withContext dao.countMediaInPaths(paths = paths.value)
    }

    suspend fun getMediaSize(): Long = withContext(Dispatchers.IO) {
        return@withContext dao.mediaSize(paths = paths.value)
    }
}