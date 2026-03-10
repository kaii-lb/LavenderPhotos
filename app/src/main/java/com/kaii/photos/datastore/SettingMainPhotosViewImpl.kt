package com.kaii.photos.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.kaii.photos.helpers.baseInternalStorageDirectory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SettingMainPhotosViewImpl(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val albumsList = stringSetPreferencesKey("main_photos_path_list")
    private val shouldShowEverything = booleanPreferencesKey("main_photos_show_everything")

    fun getAlbums(): Flow<Set<String>> =
        context.datastore.data.map { data ->
            return@map data[albumsList] ?: defaultAlbumsList
        }

    fun addAlbum(path: String) = scope.launch {
        context.datastore.edit {
            val list = (it[albumsList] ?: defaultAlbumsList).toMutableList()

            list.add(path)

            it[albumsList] = list.toSet()
        }
    }

    fun clearAlbums() = scope.launch {
        context.datastore.edit {
            it[albumsList] = emptySet()
        }
    }

    fun getShowEverything() =
        context.datastore.data.map {
            it[shouldShowEverything] == true
        }

    fun setShowEverything(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[shouldShowEverything] = value
        }
    }

    private val defaultAlbumsList = setOf(
        "${baseInternalStorageDirectory}DCIM/Camera",
        "${baseInternalStorageDirectory}Pictures",
        "${baseInternalStorageDirectory}Pictures/Screenshot"
    )
}