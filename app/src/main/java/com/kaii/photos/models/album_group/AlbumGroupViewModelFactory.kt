package com.kaii.photos.models.album_group

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Suppress("UNCHECKED_CAST")
class AlbumGroupViewModelFactory(
    private val context: Context
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == AlbumGroupViewModel::class.java) {
            return AlbumGroupViewModel(context) as T
        }
        throw IllegalArgumentException("${AlbumGroupViewModelFactory::class.simpleName}: Cannot cast ${modelClass.simpleName} as ${AlbumGroupViewModel::class.simpleName}!! This should never happen!!")
    }
}