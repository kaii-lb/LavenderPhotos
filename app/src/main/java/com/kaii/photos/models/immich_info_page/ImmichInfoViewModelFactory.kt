package com.kaii.photos.models.immich_info_page

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Suppress("UNCHECKED_CAST")
class ImmichInfoViewModelFactory : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == ImmichInfoViewModel::class.java) {
            return ImmichInfoViewModel() as T
        }
        throw IllegalArgumentException("${ImmichInfoViewModelFactory::class.simpleName}: Cannot cast ${modelClass.simpleName} as ${ImmichInfoViewModel::class.simpleName}!! This should never happen!!")
    }
}
