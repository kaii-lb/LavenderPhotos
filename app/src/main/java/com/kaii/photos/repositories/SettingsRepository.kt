package com.kaii.photos.repositories

import com.kaii.photos.data.datasources.SettingsDataSource
import com.kaii.photos.presentation.ui.theme.ThemeConfiguration

class SettingsRepository(
    private val dataSource: SettingsDataSource
) {
    fun getThemeConfiguration() = dataSource.getThemeConfiguration()
    suspend fun setThemeConfiguration(configuration: ThemeConfiguration) = dataSource.setThemeConfiguration(configuration)
}