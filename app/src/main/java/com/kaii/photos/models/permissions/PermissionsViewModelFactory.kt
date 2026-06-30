package com.kaii.photos.models.permissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Suppress("UNCHECKED_CAST")
class PermissionsViewModelFactory : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == PermissionsViewModel::class.java) {
            return PermissionsViewModel() as T
        }
        throw IllegalArgumentException("${PermissionsViewModel::class.simpleName}: Cannot cast ${modelClass.simpleName} as ${PermissionsViewModel::class.simpleName}!! This should never happen!!")
    }
}
