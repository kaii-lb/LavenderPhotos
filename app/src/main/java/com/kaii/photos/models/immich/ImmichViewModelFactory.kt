package com.kaii.photos.models.immich

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kaii.photos.datastore.SettingsImmichImpl

@Suppress("UNCHECKED_CAST")
class ImmichViewModelFactory(
    private val immichSettings: SettingsImmichImpl
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == ImmichViewModel::class.java) {
            return ImmichViewModel(immichSettings) as T
        }
        throw IllegalArgumentException("ImmichViewModel: Cannot cast ${modelClass.simpleName} as ${ImmichViewModel::class.java.simpleName}!! This should never happen!!")
    }
}
