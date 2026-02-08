package com.kaii.photos.models.custom_album

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.MediaItemSortMode

@Suppress("UNCHECKED_CAST")
class CustomAlbumViewModelFactory(
	private val albumInfo: AlbumInfo,
	private val context: Context,
    private val info: ImmichBasicInfo,
	private val sortBy: MediaItemSortMode,
	private val displayDateFormat: DisplayDateFormat
) : ViewModelProvider.NewInstanceFactory() {
	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if (modelClass == CustomAlbumViewModel::class.java) {
			return CustomAlbumViewModel(albumInfo, context, info, sortBy, displayDateFormat) as T
		}
		throw IllegalArgumentException("${CustomAlbumViewModelFactory::class.simpleName}: Cannot cast ${modelClass.simpleName} as ${CustomAlbumViewModel::class.simpleName}!! This should never happen!!")
	}
}
