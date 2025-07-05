package com.kaii.photos.models.immich

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kaii.photos.database.daos.ImmichDuplicateEntityDao
import com.kaii.photos.datastore.SettingsImmichImpl

@Suppress("UNCHECKED_CAST")
class ImmichViewModelFactory(
    private val application: Application,
    private val immichSettings: SettingsImmichImpl,
    private val immichDuplicateEntityDao: ImmichDuplicateEntityDao
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == ImmichViewModel::class.java) {
            return ImmichViewModel(application, immichSettings, immichDuplicateEntityDao)  as T
        }
        throw IllegalArgumentException("ImmichViewModel: Cannot cast ${modelClass.simpleName} as ${ImmichViewModel::class.java.simpleName}!! This should never happen!!")
    }
}
