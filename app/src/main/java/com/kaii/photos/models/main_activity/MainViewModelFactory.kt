package com.kaii.photos.models.main_activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Suppress("UNCHECKED_CAST")
class MainViewModelFactory : ViewModelProvider.NewInstanceFactory() {
	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if (modelClass == MainViewModel::class.java) {
			return MainViewModel() as T
		}
		throw IllegalArgumentException("MainDataSharingModel: Cannot cast ${modelClass.simpleName} as ${MainViewModel::class.java.simpleName}!! This should never happen!!")
	}
}
