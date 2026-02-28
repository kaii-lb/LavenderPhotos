package com.kaii.photos.models.main_grid

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Suppress("UNCHECKED_CAST")
class MainGridViewModelFactory(
    private val context: Context
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == MainGridViewModel::class.java) {
            return MainGridViewModel(context) as T
        }
        throw IllegalArgumentException("${MainGridViewModelFactory::class.simpleName}: Cannot cast ${modelClass.simpleName} as ${MainGridViewModel::class.simpleName}!! This should never happen!!")
    }
}