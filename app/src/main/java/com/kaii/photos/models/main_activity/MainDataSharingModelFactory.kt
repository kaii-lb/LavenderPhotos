package com.kaii.photos.models.main_activity

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Suppress("UNCHECKED_CAST")
class MainDataSharingModelFactory() : ViewModelProvider.NewInstanceFactory() {
	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if (modelClass == MainDataSharingModel::class.java) {
			return MainDataSharingModel() as T
		}
		throw IllegalArgumentException("MainDataSharingModel: Cannot cast ${modelClass.simpleName} as ${MainDataSharingModel::class.java.simpleName}!! This should never happen!!")
	}
}
