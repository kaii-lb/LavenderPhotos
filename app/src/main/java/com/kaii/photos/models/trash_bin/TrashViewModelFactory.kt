package com.kaii.photos.models.trash_bin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.models.multi_album.DisplayDateFormat

@Suppress("UNCHECKED_CAST")
class TrashViewModelFactory(
	private val context: Context,
	private val displayDateFormat: DisplayDateFormat,
	private val appDatabase: MediaDatabase
) : ViewModelProvider.NewInstanceFactory() {
	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if (modelClass == TrashViewModel::class.java) {
			return TrashViewModel(context, displayDateFormat, appDatabase) as T
		}
		throw IllegalArgumentException("GalleryViewModel: Cannot cast ${modelClass.simpleName} as ${TrashViewModel::class.java.simpleName}!! This should never happen!!")
	}
}
