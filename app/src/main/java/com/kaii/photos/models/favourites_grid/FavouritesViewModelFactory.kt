package com.kaii.photos.models.favourites_grid

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Suppress("UNCHECKED_CAST")
class FavouritesViewModelFactory(
    private val context: Context
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == FavouritesViewModel::class.java) {
            return FavouritesViewModel(context) as T
        }
        throw IllegalArgumentException("${FavouritesViewModelFactory::class.simpleName}: Cannot cast ${modelClass.simpleName} as ${FavouritesViewModel::class.simpleName}!! This should never happen!!")
    }
}
