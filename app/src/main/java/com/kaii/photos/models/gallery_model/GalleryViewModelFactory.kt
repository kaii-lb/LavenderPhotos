package com.kaii.photos.models.gallery_model

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kaii.photos.helpers.MediaItemSortMode

@Suppress("UNCHECKED_CAST")
class GalleryViewModelFactory(
	private val context: Context,
	private val path: String,
	private val sortBy: MediaItemSortMode
) : ViewModelProvider.NewInstanceFactory() {
	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if (modelClass == GalleryViewModel::class.java) {
			return GalleryViewModel(context, path, sortBy) as T
		}
		throw IllegalArgumentException("GalleryViewModel: Cannot cast ${modelClass.simpleName} as ${GalleryViewModel::class.java.simpleName}!! This should never happen!!")
	}
}
