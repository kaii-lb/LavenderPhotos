package com.kaii.photos.models.immich_album

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kaii.photos.datastore.AlbumType

@Suppress("UNCHECKED_CAST")
class ImmichAlbumViewModelFactory(
    private val context: Context,
    private val album: AlbumType
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == ImmichAlbumViewModel::class.java) {
            return ImmichAlbumViewModel(context, album) as T
        }
        throw IllegalArgumentException("${ImmichAlbumViewModelFactory::class.simpleName}: Cannot cast ${modelClass.simpleName} as ${ImmichAlbumViewModel::class.simpleName}!! This should never happen!!")
    }
}
