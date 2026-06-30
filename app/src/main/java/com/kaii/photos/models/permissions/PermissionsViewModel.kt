package com.kaii.photos.models.permissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.PhotosApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class PermissionsViewModel : ViewModel() {
    private val settings = PhotosApplication.appModule.settings

    fun setIsMediaManager(value: Boolean) {
        viewModelScope.launch {
            settings.permissions.setIsMediaManager(value)
        }
    }

    fun launch(block: suspend (scope: CoroutineScope) -> Unit) {
        viewModelScope.launch(block = block)
    }
}