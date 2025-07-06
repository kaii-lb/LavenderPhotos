package com.kaii.photos.models.favourites_grid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kaii.photos.database.MediaDatabase

@Suppress("UNCHECKED_CAST")
class FavouritesViewModelFactory(
    private val appDatabase: MediaDatabase
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == FavouritesViewModel::class.java) {
            return FavouritesViewModel(appDatabase = appDatabase) as T
        }
        throw IllegalArgumentException("FavouritesViewModel: Cannot cast ${modelClass.simpleName} as ${FavouritesViewModel::class.java.simpleName}!! This should never happen!!")
    }
}
