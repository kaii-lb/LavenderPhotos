package com.aks_labs.tulsi.models.main_activity

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Suppress("UNCHECKED_CAST")
class MainViewModelFactory(private val context: Context) : ViewModelProvider.NewInstanceFactory() {
	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if (modelClass == MainViewModel::class.java) {
			return MainViewModel(context) as T
		}
		throw IllegalArgumentException("MainDataSharingModel: Cannot cast ${modelClass.simpleName} as ${MainViewModel::class.java.simpleName}!! This should never happen!!")
	}
}


