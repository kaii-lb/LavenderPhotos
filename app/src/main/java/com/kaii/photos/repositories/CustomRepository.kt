package com.kaii.photos.repositories

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.paging.mapToMedia
import com.kaii.photos.helpers.paging.mapToSeparatedMedia
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

class CustomRepository(
    private val scope: CoroutineScope,
    context: Context,
    albumInfo: AlbumInfo,
    info: ImmichBasicInfo,
    sortMode: MediaItemSortMode,
    format: DisplayDateFormat
) {
    private val appContext = context.applicationContext
    private val dao = MediaDatabase.getInstance(appContext).customDao()

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
                if (sortMode.isDateModified) dao.getPagedMediaDateModified(album = albumInfo.id)
                else dao.getPagedMediaDateTaken(album = albumInfo.id)
            }
        ).flow.mapToMedia(accessToken = info.accessToken).cachedIn(scope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val gridMediaFlow = mediaFlow.mapToSeparatedMedia(
        sortMode = sortMode,
        format = format
    ).cachedIn(scope)

    fun remove(
        items: Set<MediaStoreData>,
        albumId: Int
    ) = scope.launch(Dispatchers.IO) {
        items.map {
            it.id
        }.let { ids ->
            dao.deleteAll(ids = ids.toSet(), album = albumId)
        }
    }
}