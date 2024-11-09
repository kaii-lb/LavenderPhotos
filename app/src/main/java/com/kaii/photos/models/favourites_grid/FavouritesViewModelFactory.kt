package com.kaii.photos.models.favourites_grid

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.models.gallery_model.GalleryViewModel

@Suppress("UNCHECKED_CAST")
class FavouritesViewModelFactory() : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == FavouritesViewModel::class.java) {
            return FavouritesViewModel() as T
        }
        throw IllegalArgumentException("FavouritesViewModel: Cannot cast ${modelClass.simpleName} as ${FavouritesViewModel::class.java.simpleName}!! This should never happen!!")
    }
}