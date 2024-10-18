package com.kaii.photos.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import java.io.File

val albumsListKey = stringPreferencesKey("album_folder_path_list")
val usernameKey = stringPreferencesKey("username")

suspend fun DataStore<Preferences>.addToAlbumsList(path: String) {
    this.edit {
        val stringList = it[albumsListKey]

        if (stringList == null) it[albumsListKey] = ""

        if (stringList?.contains(",$path,") == false) {
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

suspend fun DataStore<Preferences>.editInAlbumsList(path: String, newName: String) {
    this.edit {
        val stringList = it[albumsListKey]
        val last = path.split("/").last()

        if (stringList?.contains(path) == true) {
            it[albumsListKey] = stringList.replace(path, path.replace(last, newName))
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

    return split
}


suspend fun DataStore<Preferences>.setUsername(name: String) {
    this.edit {
        it[usernameKey] = name
    }
}

suspend fun DataStore<Preferences>.getUsername(): String {
    val name = this.data.first()[usernameKey] ?: "No Username Found"

    return name
}
