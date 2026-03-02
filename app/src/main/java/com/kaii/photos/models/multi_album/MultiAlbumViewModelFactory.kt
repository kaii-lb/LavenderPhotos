package com.kaii.photos.models.multi_album

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kaii.photos.datastore.AlbumInfo

@Suppress("UNCHECKED_CAST")
class MultiAlbumViewModelFactory(
    private val context: Context,
    private val albumInfo: AlbumInfo
) : ViewModelProvider.NewInstanceFactory() {
	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if (modelClass == MultiAlbumViewModel::class.java) {
			return MultiAlbumViewModel(context, albumInfo) as T
		}
		throw IllegalArgumentException("${MultiAlbumViewModelFactory::class.simpleName}: Cannot cast ${modelClass.simpleName} as ${MultiAlbumViewModel::class.simpleName}!! This should never happen!!")
	}
}
