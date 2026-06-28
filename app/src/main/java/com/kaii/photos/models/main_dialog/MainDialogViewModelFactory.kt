package com.kaii.photos.models.main_dialog

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kaii.photos.di.appModule

@Suppress("UNCHECKED_CAST")
class MainDialogViewModelFactory(
    private val context: Context
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == MainDialogViewModel::class.java) {
            val settings = context.appModule.settings

            return MainDialogViewModel(settings) as T
        }
        throw IllegalArgumentException("${MainDialogViewModel::class.simpleName}: Cannot cast ${modelClass.simpleName} as ${MainDialogViewModel::class.simpleName}!! This should never happen!!")
    }
}
