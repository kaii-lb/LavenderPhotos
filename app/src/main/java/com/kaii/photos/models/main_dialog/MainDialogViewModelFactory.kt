package com.kaii.photos.models.main_dialog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kaii.photos.PhotosApplication

@Suppress("UNCHECKED_CAST")
class MainDialogViewModelFactory : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == MainDialogViewModel::class.java) {
            val settings = PhotosApplication.appModule.settings

            return MainDialogViewModel(settings) as T
        }
        throw IllegalArgumentException("${MainDialogViewModel::class.simpleName}: Cannot cast ${modelClass.simpleName} as ${MainDialogViewModel::class.simpleName}!! This should never happen!!")
    }
}
