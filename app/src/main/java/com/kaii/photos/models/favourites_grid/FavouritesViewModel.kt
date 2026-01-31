package com.kaii.photos.models.favourites_grid

import android.content.Context
import android.os.CancellationSignal
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.models.multi_album.mapToMedia

class FavouritesViewModel(
    context: Context,
    sortMode: MediaItemSortMode,
    displayDateFormat: DisplayDateFormat
) : ViewModel() {
    private val cancellationSignal = CancellationSignal()
    // private val mediaStoreDataSource =
    //     FavouritesDataSource(
    //         context = context,
    //         sortMode = sortMode,
    //         cancellationSignal = cancellationSignal,
    //         displayDateFormat = displayDateFormat
    //     )

    val mediaFlow = Pager(
        config = PagingConfig(
            pageSize = 80,
            prefetchDistance = 40,
            enablePlaceholders = true,
            initialLoadSize = 80
        ),
        pagingSourceFactory = { MediaDatabase.getInstance(context).mediaDao().getPagedMedia() } // TODO
    ).flow.mapToMedia(sortMode = sortMode, format = displayDateFormat).cachedIn(viewModelScope)

    // private fun getMediaDataFlow(): Flow<List<MediaStoreData>> {
    //     return mediaStoreDataSource.loadMediaStoreData().flowOn(Dispatchers.IO)
    // }

    fun cancelMediaFlow() {
        cancellationSignal.cancel()
    }
}
