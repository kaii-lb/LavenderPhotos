package com.kaii.photos.datastore.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.kaii.photos.datastore.datastore
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.TopBarDetailsFormat
import com.kaii.photos.presentation.ui.theme.ThemeConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SettingsLookAndFeelImpl(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val themeConfigKey = intPreferencesKey("look_and_feel_theme_config_key")
    private val displayDateFormat = intPreferencesKey("look_and_feel_display_date_format")
    private val columnSize = intPreferencesKey("look_and_feel_column_size")
    private val albumColumnSize = intPreferencesKey("look_and_feel_album_column_size")
    private val blackBackgroundForViews = booleanPreferencesKey("look_and_feel_black_background")
    private val showExtraSecureNav = booleanPreferencesKey("look_and_feel_extra_secure")
    private val useRoundedCorners = booleanPreferencesKey("look_and_feel_use_rounded_corners") // for photo grid
    private val topBarDetailsFormat = intPreferencesKey("look_and_feel_top_bar_details_format")
    private val blurForViews = booleanPreferencesKey("look_and_feel_blur_views")
    private val vibrateOnMediaClick = booleanPreferencesKey("look_and_feel_vibrate_on_media_click")

    fun getDisplayDateFormat(): Flow<DisplayDateFormat> =
        context.datastore.data.map {
            DisplayDateFormat.entries[it[displayDateFormat] ?: 0]
        }

    fun setDisplayDateFormat(format: DisplayDateFormat) = scope.launch {
        context.datastore.edit {
            it[displayDateFormat] = DisplayDateFormat.entries.indexOf(format)
        }
    }

    fun getColumnSize(): Flow<Int> =
        context.datastore.data.map {
            it[columnSize] ?: 3
        }

    fun setColumnSize(size: Int) = scope.launch {
        context.datastore.edit {
            it[columnSize] = size
        }
    }

    fun getAlbumColumnSize(): Flow<Int> =
        context.datastore.data.map {
            it[albumColumnSize] ?: 2
        }

    fun setAlbumColumnSize(size: Int) = scope.launch {
        context.datastore.edit {
            it[albumColumnSize] = size
        }
    }

    fun getUseBlackBackgroundForViews(): Flow<Boolean> =
        context.datastore.data.map {
            it[blackBackgroundForViews] == true
        }

    fun setUseBlackBackgroundForViews(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[blackBackgroundForViews] = value
        }
    }

    /** shows an extra "navigate to secure page" in the main app dialog */
    fun getShowExtraSecureNav(): Flow<Boolean> =
        context.datastore.data.map {
            it[showExtraSecureNav] == true
        }

    fun setShowExtraSecureNav(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[showExtraSecureNav] = value
        }
    }

    /** round the thumbnail corners in photo grids */
    fun getUseRoundedCorners(): Flow<Boolean> =
        context.datastore.data.map {
            it[useRoundedCorners] == true
        }

    fun setUseRoundedCorners(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[useRoundedCorners] = value
        }
    }

    fun getTopBarDetailsFormat(): Flow<TopBarDetailsFormat> =
        context.datastore.data.map {
            TopBarDetailsFormat.entries[it[topBarDetailsFormat] ?: 0]
        }

    fun setTopBarDetailsFormat(format: TopBarDetailsFormat) = scope.launch {
        context.datastore.edit {
            it[topBarDetailsFormat] = TopBarDetailsFormat.entries.indexOf(format)
        }
    }

    fun getBlurViews(): Flow<Boolean> =
        context.datastore.data.map {
            it[blurForViews] ?: true
        }

    fun setBlurViews(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[blurForViews] = value
        }
    }

    fun getVibrateOnMediaClick() =
        context.datastore.data.map {
            it[vibrateOnMediaClick] ?: true
        }

    fun setVibrateOnMediaClick(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[vibrateOnMediaClick] = value
        }
    }

    fun getThemeConfiguration() =
        context.datastore.data.map {
            it[themeConfigKey] ?: ThemeConfiguration.Default.serialize()
        }

    suspend fun setThemeConfiguration(config: Int) =
        context.datastore.edit {
            it[themeConfigKey] = config
        }
}