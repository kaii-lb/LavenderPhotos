package com.kaii.photos.models.album_grid

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kaii.photos.datastore.AlbumInfo

@Suppress("UNCHECKED_CAST")
class AlbumsViewModelFactory(private val context: Context, private val albums: List<AlbumInfo>) : ViewModelProvider.NewInstanceFactory() {
	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if (modelClass == AlbumsViewModel::class.java) {
			return AlbumsViewModel(context = context, albumInfo = albums) as T
		}
		throw IllegalArgumentException("AlbumsViewModel: Cannot cast ${modelClass.simpleName} as ${AlbumsViewModel::class.java.simpleName}!! This should never happen!!")
	}
}
