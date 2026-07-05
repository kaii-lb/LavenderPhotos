package com.kaii.photos.data.datasources

import com.kaii.photos.datastore.Settings
import com.kaii.photos.presentation.ui.theme.ThemeConfiguration
import kotlinx.coroutines.flow.map

class SettingsDataSource(
    private val settings: Settings
) {
    fun getThemeConfiguration() = settings.lookAndFeel.getThemeConfiguration().map { serial ->
        ThemeConfiguration(serial)
    }

    suspend fun setThemeConfiguration(configuration: ThemeConfiguration) = settings.lookAndFeel.setThemeConfiguration(
        config = configuration.serialize()
    )
}