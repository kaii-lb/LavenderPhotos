package com.kaii.photos.models.permissions

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Suppress("UNCHECKED_CAST")
class PermissionsViewModelFactory(
    private val context: Context
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == PermissionsViewModel::class.java) {
            return PermissionsViewModel(context) as T
        }
        throw IllegalArgumentException("${PermissionsViewModel::class.simpleName}: Cannot cast ${modelClass.simpleName} as ${PermissionsViewModel::class.simpleName}!! This should never happen!!")
    }
}
