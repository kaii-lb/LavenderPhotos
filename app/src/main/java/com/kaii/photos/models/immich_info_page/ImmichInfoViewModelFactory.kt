package com.kaii.photos.models.immich_info_page

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Suppress("UNCHECKED_CAST")
class ImmichInfoViewModelFactory(
    private val context: Context
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == ImmichInfoViewModel::class.java) {
            return ImmichInfoViewModel(context) as T
        }
        throw IllegalArgumentException("${ImmichInfoViewModelFactory::class.simpleName}: Cannot cast ${modelClass.simpleName} as ${ImmichInfoViewModel::class.simpleName}!! This should never happen!!")
    }
}
