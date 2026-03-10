package com.kaii.photos.datastore.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.kaii.photos.datastore.datastore
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.TopBarDetailsFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SettingsLookAndFeelImpl(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val followDarkModeKey = intPreferencesKey("look_and_feel_follow_dark_mode")
    private val displayDateFormat = intPreferencesKey("look_and_feel_display_date_format")
    private val columnSize = intPreferencesKey("look_and_feel_column_size")
    private val albumColumnSize = intPreferencesKey("look_and_feel_album_column_size")
    private val blackBackgroundForViews = booleanPreferencesKey("look_and_feel_black_background")
    private val showExtraSecureNav = booleanPreferencesKey("look_and_feel_extra_secure")
    private val useRoundedCorners = booleanPreferencesKey("look_and_feel_use_rounded_corners") // for photo grid
    private val topBarDetailsFormat = intPreferencesKey("look_and_feel_top_bar_details_format")
    private val blurForViews = booleanPreferencesKey("look_and_feel_blur_views")

    /** 0 is follow system
     * 1 is dark
     * 2 is light
     * 3 is amoled black */
    fun getFollowDarkMode(): Flow<Int> =
        context.datastore.data.map {
            it[followDarkModeKey] ?: 0
        }

    /** 0 is follow system
     * 1 is dark
     * 2 is light
     * 3 is amoled black */
    fun setFollowDarkMode(value: Int) = scope.launch {
        context.datastore.edit {
            it[followDarkModeKey] = value
        }
    }

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
            it[blurForViews] ?: false
        }

    fun setBlurViews(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[blurForViews] = value
        }
    }
}