package com.kaii.photos.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import java.io.File

private object PreferencesKeys {
    val albumsList = stringPreferencesKey("album_folder_path_list")
    val username = stringPreferencesKey("username")
    val v083firstStart = booleanPreferencesKey("v0.8.3-beta_first_start")
    val shouldRecordLogs = booleanPreferencesKey("should_record_logs")
}

const val separator = "|-SEPARATOR-|"

suspend fun DataStore<Preferences>.addToAlbumsList(path: String) =
    this.edit {
        val stringList = it[PreferencesKeys.albumsList]

        if (stringList == null) it[PreferencesKeys.albumsList] = ""
        
        println("ALBUMS STRING LIST $stringList")
        println("ALBUMS KEYS PATH $path ${stringList?.contains("$separator$path") == false}")

        if (stringList?.contains("$separator$path") == false || stringList?.contains("$path$separator") == false) {
            it[PreferencesKeys.albumsList] += "$separator$path"
        }
    }

suspend fun DataStore<Preferences>.removeFromAlbumsList(path: String) =
    this.edit {
        val stringList = it[PreferencesKeys.albumsList]

        if (stringList?.contains(path) == true) {
            it[PreferencesKeys.albumsList] = stringList.replace("$separator$path", "")
        }
    }

suspend fun DataStore<Preferences>.editInAlbumsList(path: String, newName: String) =
    this.edit {
        val stringList = it[PreferencesKeys.albumsList]
        val last = path.split("/").last()

        if (stringList?.contains(path) == true) {
            it[PreferencesKeys.albumsList] = stringList.replace(path, path.replace(last, newName))
        }
    }

suspend fun DataStore<Preferences>.getAlbumsList(isPreV083: Boolean = false): List<String> {
    val list = this.data.first()[PreferencesKeys.albumsList] ?: return emptyList()

    val splitBy = if (isPreV083) "," else separator
    val split = list.split(splitBy).distinct().toMutableList()

    split.sortByDescending {
        File(it).lastModified()
    }

    split.remove("")

    return split
}

suspend fun DataStore<Preferences>.setAlbumsList(list: List<String>) =
    this.edit {
        var stringList = ""
        list.distinct().forEach { album ->
            if (!stringList.contains("$separator$it") || !stringList.contains("$it$separator")) stringList += "$separator$album"
        }

        it[PreferencesKeys.albumsList] = stringList
    }


suspend fun DataStore<Preferences>.setUsername(name: String) =
    this.edit {
        it[PreferencesKeys.username] = name
    }

suspend fun DataStore<Preferences>.getUsername(): String {
    val name = this.data.first()[PreferencesKeys.username] ?: "No Username Found"

    return name
}

suspend fun DataStore<Preferences>.getIsV083FirstStart(context: Context): Boolean {
    val currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName
    return (this.data.first()[PreferencesKeys.v083firstStart] ?: true) && currentVersion == "v0.8.3-beta"
}

suspend fun DataStore<Preferences>.setIsV083FirstStart(value: Boolean) {
    this.edit {
        it[PreferencesKeys.v083firstStart] = value
    }
}