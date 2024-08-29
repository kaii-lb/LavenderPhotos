package com.kaii.photos.gallery_model

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Suppress("UNCHECKED_CAST")
class GalleryViewModelFactory(private val context: Context, private val path: String) : ViewModelProvider.NewInstanceFactory() {
	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if (modelClass == GalleryViewModel::class.java) {
			return GalleryViewModel(context, path) as T
		}
		throw IllegalArgumentException("GalleryViewModel: Cannot cast ${modelClass.simpleName} as ${GalleryViewModel::class.java.simpleName}!! This should never happen!!")
	}
}
