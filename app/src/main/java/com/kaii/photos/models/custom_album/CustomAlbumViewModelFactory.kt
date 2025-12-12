package com.kaii.photos.models.custom_album

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.MediaItemSortMode

@Suppress("UNCHECKED_CAST")
class CustomAlbumViewModelFactory(
	private val context: Context,
	private val albumInfo: AlbumInfo,
	private val sortBy: MediaItemSortMode,
	private val displayDateFormat: DisplayDateFormat
) : ViewModelProvider.NewInstanceFactory() {
	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if (modelClass == CustomAlbumViewModel::class.java) {
			return CustomAlbumViewModel(context, albumInfo, sortBy, displayDateFormat) as T
		}
		throw IllegalArgumentException("MultiAlbumViewModel: Cannot cast ${modelClass.simpleName} as ${CustomAlbumViewModel::class.java.simpleName}!! This should never happen!!")
	}
}
