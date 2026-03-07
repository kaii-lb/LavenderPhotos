package com.kaii.photos.models.custom_album

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kaii.photos.datastore.AlbumType

@Suppress("UNCHECKED_CAST")
class CustomAlbumViewModelFactory(
    private val context: Context,
    private val album: AlbumType
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == CustomAlbumViewModel::class.java) {
            return CustomAlbumViewModel(context, album) as T
        }
        throw IllegalArgumentException("${CustomAlbumViewModelFactory::class.simpleName}: Cannot cast ${modelClass.simpleName} as ${CustomAlbumViewModel::class.simpleName}!! This should never happen!!")
    }
}
