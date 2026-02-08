package com.kaii.photos.repositories

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.models.loading.mapToMedia
import com.kaii.photos.models.loading.mapToSeparatedMedia
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi

class MediaRepository(
    context: Context,
    info: ImmichBasicInfo,
    albumInfo: AlbumInfo,
    scope: CoroutineScope,
    sortMode: MediaItemSortMode,
    format: DisplayDateFormat
) {
    private val dao = MediaDatabase.getInstance(context.applicationContext).mediaDao()

    @OptIn(ExperimentalCoroutinesApi::class)
    val mediaFlow =
        Pager(
            config = PagingConfig(
                pageSize = 50,
                prefetchDistance = 50,
                enablePlaceholders = true,
                initialLoadSize = 100
            ),
            pagingSourceFactory = {
                if (sortMode.isDateModified) dao.getPagedMediaDateModified(paths = albumInfo.paths)
                else dao.getPagedMediaDateTaken(paths = albumInfo.paths)
            }
        ).flow
            .mapToMedia(accessToken = info.accessToken)
            .cachedIn(scope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val gridMediaFlow = mediaFlow.mapToSeparatedMedia(
        sortMode = sortMode,
        format = format
    ).cachedIn(scope)
}