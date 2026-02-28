package com.kaii.photos.repositories

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.paging.mapToMedia
import com.kaii.photos.helpers.paging.mapToSeparatedMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.withContext

class CustomRepository(
    private val dao: CustomEntityDao,
    private val albumInfo: AlbumInfo,
    sortMode: Flow<MediaItemSortMode>,
    format: Flow<DisplayDateFormat>,
    info: Flow<ImmichBasicInfo>
) {
    private val params = combine(info, sortMode, format) { info, sortMode, format ->
        RoomQueryParams(
            accessToken = info.accessToken,
            sortMode = sortMode,
            format = format
        )
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
                if (params.sortMode.isDateModified) dao.getPagedMediaDateModified(album = albumInfo.id)
                else dao.getPagedMediaDateTaken(album = albumInfo.id)
            }
        ).flow.mapToMedia(accessToken = params.accessToken)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val gridMediaFlow = params.flatMapLatest { params ->
        mediaFlow.mapToSeparatedMedia(
            sortMode = params.sortMode,
            format = params.format
        )
    }

    suspend fun remove(
        items: Set<MediaStoreData>,
        albumId: Int
    ) {
        dao.deleteAll(ids = items.map { it.id }.toSet(), album = albumId)
    }

    suspend fun getMediaCount(): Int = withContext(Dispatchers.IO) {
        return@withContext dao.countMediaInAlbum(album = albumInfo.id)
    }

    suspend fun getMediaSize(): Long = withContext(Dispatchers.IO) {
        return@withContext dao.mediaSize(album = albumInfo.id)
    }
}