package com.kaii.photos.repositories

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.paging.mapToMedia
import com.kaii.photos.helpers.paging.mapToSeparatedMedia
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

class FavouritesRepository(
    dao: MediaDao,
    info: Flow<ImmichBasicInfo>,
    sortMode: Flow<MediaItemSortMode>,
    format: Flow<DisplayDateFormat>
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
                if (params.sortMode.isDateModified) dao.getPagedFavouritesDateModified()
                else dao.getPagedFavouritesDateTaken()
            }
        ).flow
            .mapToMedia(accessToken = params.accessToken)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val gridMediaFlow = params.flatMapLatest { params ->
        mediaFlow.mapToSeparatedMedia(
            sortMode = params.sortMode,
            format = params.format
        )
    }
}