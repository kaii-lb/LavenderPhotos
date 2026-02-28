package com.kaii.photos.models.trash_bin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Suppress("UNCHECKED_CAST")
class TrashViewModelFactory(
    private val context: Context
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == TrashViewModel::class.java) {
            return TrashViewModel(context) as T
        }
        throw IllegalArgumentException("${TrashViewModelFactory::class.simpleName}: Cannot cast ${modelClass.simpleName} as ${TrashViewModel::class.simpleName}!! This should never happen!!")
    }
}
