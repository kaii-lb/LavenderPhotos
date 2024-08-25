package com.kaii.photos.gallery_model

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class GalleryViewModelFactory(private val context: Context) : ViewModelProvider.NewInstanceFactory() {
	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		return GalleryViewModel(context) as T
	}
}
