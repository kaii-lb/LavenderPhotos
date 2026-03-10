package com.kaii.photos.datastore.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kaii.photos.datastore.datastore
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SettingsPhotoGridImpl(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val mediaSortModeKey = stringPreferencesKey("media_sort_mode")

    fun getSortMode() = context.datastore.data.map {
        val name = it[mediaSortModeKey] ?: MediaItemSortMode.DateTaken.name

        MediaItemSortMode.entries.find { entry ->
            entry.name == name
        }
            ?: throw IllegalArgumentException("Sort mode $name does not exist! This should never happen!")
    }

    fun setSortMode(mode: MediaItemSortMode) = scope.launch {
        context.datastore.edit {
            it[mediaSortModeKey] = mode.name
        }
    }
}