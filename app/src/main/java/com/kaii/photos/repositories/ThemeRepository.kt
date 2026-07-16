package com.kaii.photos.repositories

import com.kaii.photos.data.datasources.ThemeDataSource
import com.kaii.photos.presentation.ui.theme.ThemeConfiguration

class ThemeRepository(
    private val dataSource: ThemeDataSource
) {
    fun getThemeConfiguration() = dataSource.getThemeConfiguration()
    suspend fun setThemeConfiguration(configuration: ThemeConfiguration) = dataSource.setThemeConfiguration(configuration)
}