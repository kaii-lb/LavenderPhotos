package com.kaii.photos.models.main_activity

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kaii.photos.datastore.AlbumInfo

@Suppress("UNCHECKED_CAST")
class MainViewModelFactory(private val context: Context, private val albumInfo: List<AlbumInfo>) : ViewModelProvider.NewInstanceFactory() {
	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if (modelClass == MainViewModel::class.java) {
			return MainViewModel(context, albumInfo) as T
		}
		throw IllegalArgumentException("MainDataSharingModel: Cannot cast ${modelClass.simpleName} as ${MainViewModel::class.java.simpleName}!! This should never happen!!")
	}
}
