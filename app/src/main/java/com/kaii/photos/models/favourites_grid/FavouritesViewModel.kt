package com.kaii.photos.models.favourites_grid

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.datastore.SettingsImmichImpl
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.models.loading.mapToMedia
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest

class FavouritesViewModel(
    context: Context,
    sortMode: MediaItemSortMode,
    displayDateFormat: DisplayDateFormat
) : ViewModel() {
    private val settings = SettingsImmichImpl(context = context, viewModelScope = viewModelScope)
    private val basicInfo = settings.getImmichBasicInfo()

    @OptIn(ExperimentalCoroutinesApi::class)
    val mediaFlow = basicInfo.flatMapLatest { info ->
        Pager(
            config = PagingConfig(
                pageSize = 80,
                prefetchDistance = 40,
                enablePlaceholders = true,
                initialLoadSize = 80
            ),
            pagingSourceFactory = {
                MediaDatabase.getInstance(context)
                    .mediaDao()
                    .getPagedFavourites(sortByProp = sortMode.toSortProp())
            }
        ).flow.mapToMedia(
            sortMode = sortMode,
            format = displayDateFormat,
            accessToken = info.accessToken
        ).cachedIn(viewModelScope)
    }
}
