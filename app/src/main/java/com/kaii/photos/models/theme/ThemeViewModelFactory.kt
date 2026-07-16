package com.kaii.photos.models.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kaii.photos.PhotosApplication
import com.kaii.photos.data.datasources.ThemeDataSource
import com.kaii.photos.repositories.ThemeRepository

@Suppress("UNCHECKED_CAST")
class ThemeViewModelFactory : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == ThemeViewModel::class.java) {
            val dataSource = ThemeDataSource(
                settings = PhotosApplication.appModule.settings
            )

            return ThemeViewModel(ThemeRepository(dataSource)) as T
        }
        throw IllegalArgumentException("${ThemeViewModel::class.simpleName}: Cannot cast ${modelClass.simpleName} as ${ThemeViewModel::class.simpleName}!! This should never happen!!")
    }
}
