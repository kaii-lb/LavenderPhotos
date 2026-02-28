package com.kaii.photos.models.secure_folder

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Suppress("UNCHECKED_CAST")
class SecureFolderViewModelFactory(
    private val context: Context
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == SecureFolderViewModel::class.java) {
            return SecureFolderViewModel(context) as T
        }
        throw IllegalArgumentException("${SecureFolderViewModelFactory::class.simpleName}: Cannot cast ${modelClass.simpleName} as ${SecureFolderViewModel::class.simpleName}!! This should never happen!!")
    }
}
