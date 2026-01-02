package com.kaii.photos.models.favourites_grid

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.MediaItemSortMode

@Suppress("UNCHECKED_CAST")
class FavouritesViewModelFactory(
    private val context: Context,
    private val sortMode: MediaItemSortMode,
    private val displayDateFormat: DisplayDateFormat
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == FavouritesViewModel::class.java) {
            return FavouritesViewModel(context, sortMode, displayDateFormat) as T
        }
        throw IllegalArgumentException("FavouritesViewModel: Cannot cast ${modelClass.simpleName} as ${FavouritesViewModel::class.java.simpleName}!! This should never happen!!")
    }
}
