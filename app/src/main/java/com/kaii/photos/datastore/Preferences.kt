package com.kaii.photos.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import java.io.File

val albumsListKey = stringPreferencesKey("album_folder_path_list")

suspend fun DataStore<Preferences>.addToAlbumsList(path: String) {
    this.edit {
    	val stringList = it[albumsListKey]

    	if (stringList?.contains(path) == false) {
	        it[albumsListKey] += ",$path"
    	}
    }
}

suspend fun DataStore<Preferences>.removeFromAlbumsList(path: String) {
    this.edit {
    	val stringList = it[albumsListKey]

    	if (stringList?.contains(path) == true) {
	        it[albumsListKey] = stringList.replace(",$path", "")
    	}
    }
}

suspend fun DataStore<Preferences>.getAlbumsList(): List<String> {
    val list = this.data.first()[albumsListKey] ?: return emptyList()

    val split = list.split(",").toMutableList()

    split.sortByDescending {
        File(it).lastModified()
    }

    split.remove("")

    split.forEach {
        println("SPLIT VALUE $it")
    }
    return split
}
