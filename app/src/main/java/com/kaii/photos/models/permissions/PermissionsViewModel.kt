package com.kaii.photos.models.permissions

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.di.appModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class PermissionsViewModel(
    context: Context
) : ViewModel() {
    private val settings = context.appModule.settings

    fun setIsMediaManager(value: Boolean) {
        viewModelScope.launch {
            settings.permissions.setIsMediaManager(value)
        }
    }

    fun launch(block: suspend (scope: CoroutineScope) -> Unit) {
        viewModelScope.launch(block = block)
    }
}