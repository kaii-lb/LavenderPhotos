package com.kaii.photos.models.immich_share_album_page

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Suppress("UNCHECKED_CAST")
class ImmichShareAlbumViewModelFactory(
    private val context: Context,
    private val albumImmichId: String
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == ImmichShareAlbumViewModel::class.java) {
            return ImmichShareAlbumViewModel(context, albumImmichId) as T
        }
        throw IllegalArgumentException("${ImmichShareAlbumViewModelFactory::class.simpleName}: Cannot cast ${modelClass.simpleName} as ${ImmichShareAlbumViewModel::class.simpleName}!! This should never happen!!")
    }
}
