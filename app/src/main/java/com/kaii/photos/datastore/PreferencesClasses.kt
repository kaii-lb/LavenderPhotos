package com.kaii.photos.datastore

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val separator = "|-SEPARATOR-|"

class Settings(val context: Context, val viewModelScope: CoroutineScope)

class SettingsAlbumsListImpl(private val context: Context, private val viewModelScope: CoroutineScope) {
    private val albumsListKey = stringPreferencesKey("album_folder_path_list")

    fun addToAlbumsList(path: String) = viewModelScope.launch {
        context.datastore.edit {
            val stringList = it[albumsListKey]

            if (stringList == null) it[albumsListKey] = ""

            println("ALBUMS STRING LIST $stringList")
            println("ALBUMS KEYS PATH $path ${stringList?.contains("$separator$path") == false}")

            if (stringList?.contains("$separator$path") == false || stringList?.contains("$path$separator") == false) {
                it[albumsListKey] += "$separator$path"
            }
        }
    }

    fun removeFromAlbumsList(path: String) = viewModelScope.launch {
        context.datastore.edit {
            val stringList = it[albumsListKey]

            if (stringList?.contains(path) == true) {
                it[albumsListKey] = stringList.replace("$separator$path", "")
            }
        }
    }

    fun editInAlbumsList(path: String, newPath: String) = viewModelScope.launch {
        context.datastore.edit {
            val stringList = it[albumsListKey]
            val last = path.split("/").last()

            if (stringList?.contains(path) == true) {
                it[albumsListKey] = stringList.replace(path, path.replace(last, newPath))
            }
        }
    }

    fun getAlbumsList(isPreV083: Boolean = false): Flow<List<String>> =
        context.datastore.data.map { data ->
            val list = data[albumsListKey]

            if (list == null) {
            	val defaultList = getDefaultAlbumsList()
            	setAlbumsList(defaultList)
            	return@map defaultList
            }

            val splitBy = if (isPreV083) "," else separator
            val split = list.split(splitBy).distinct().toMutableList()

            split.remove("")

            return@map split
        }

    fun setAlbumsList(list: List<String>) = viewModelScope.launch {
        context.datastore.edit {
            var stringList = ""
            list.distinct().forEach { album ->
                if (!stringList.contains("$separator$it") || !stringList.contains("$it$separator")) stringList += "$separator$album"
            }

            it[albumsListKey] = stringList
        }
    }

    private fun getDefaultAlbumsList() : List<String> =
        listOf(
            "DCIM/Camera",
            "Pictures",
            "Pictures/Screenshot",
            "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images",
            "Download"
        )
}

class SettingsVersionImpl(private val context: Context, private val viewModelScope: CoroutineScope) {
    private val v083firstStartKey = booleanPreferencesKey("v0.8.3-beta_first_start")

    fun getIsV083FirstStart(context: Context): Flow<Boolean> =
        context.datastore.data.map {
            val currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName

            (it[v083firstStartKey] ?: true) && currentVersion == "v0.8.3-beta"
        }

    fun setIsV083FirstStart(value: Boolean) = viewModelScope.launch {
        context.datastore.edit {
            it[v083firstStartKey] = value
        }
    }
}

class SettingsUserImpl(private val context: Context, private val viewModelScope: CoroutineScope) {
    private val usernameKey = stringPreferencesKey("username")

    fun getUsername(): Flow<String?> =
        context.datastore.data.map {
            it[usernameKey] ?: "No Username Found"
        }

    fun setUsername(name: String) = viewModelScope.launch {
        context.datastore.edit {
            it[usernameKey] = name
        }
    }
}

class SettingsLogsImpl(private val context: Context, private val viewModelScope: CoroutineScope) {
    private val recordLogsKey = booleanPreferencesKey("record_logs")

    fun getRecordLogs(): Flow<Boolean> =
        context.datastore.data.map {
            it[recordLogsKey] ?: true
        }

    fun setRecordLogs(value: Boolean) = viewModelScope.launch {
        context.datastore.edit {
            it[recordLogsKey] = value
        }
    }
}

class SettingsPermissionsImpl(private val context: Context, private val viewModelScope: CoroutineScope) {
    private val isMediaManagerKey = booleanPreferencesKey("is_media_manager")

    fun getIsMediaManager(): Flow<Boolean> =
        context.datastore.data.map {
            it[isMediaManagerKey] ?: false
        }

    fun setIsMediaManager(value: Boolean) = viewModelScope.launch {
        context.datastore.edit {
            it[isMediaManagerKey] = value
        }
    }
}

class SettingsTrashBinImpl(private val context: Context, private val viewModelScope: CoroutineScope) {
    private val autoDeleteIntervalKey = intPreferencesKey("auto_delete_trash_interval")

    fun getAutoDeleteInterval(): Flow<Int> =
        context.datastore.data.map {
            it[autoDeleteIntervalKey] ?: 30
        }

    fun setAutoDeleteInterval(value: Int) = viewModelScope.launch {
        context.datastore.edit {
            it[autoDeleteIntervalKey] = value
        }
    }
}

class SettingsStorageImpl(private val context: Context, private val viewModelScope: CoroutineScope) {
    private val thumbnailSizeKey = intPreferencesKey("thumbnail_size_key")
    private val cacheThumbnailsKey = booleanPreferencesKey("cache_thumbnails_key")

    fun getThumbnailSize(): Flow<Int> =
        context.datastore.data.map {
            it[thumbnailSizeKey] ?: 256
        }

    fun setThumbnailSize(value: Int) = viewModelScope.launch {
        context.datastore.edit {
            it[thumbnailSizeKey] = value
        }
    }

    fun getCacheThumbnails(): Flow<Boolean> =
        context.datastore.data.map {
            it[cacheThumbnailsKey] ?: true
        }

    fun setCacheThumbnails(value: Boolean) = viewModelScope.launch {
        context.datastore.edit {
            it[cacheThumbnailsKey] = value
        }
    }

    fun clearThumbnailCache() = viewModelScope.launch {
    	withContext(Dispatchers.IO) {
   			Glide.get(context.applicationContext).clearDiskCache()
		}
    }
}

class SettingsVideoImpl(private val context: Context, private val viewModelScope: CoroutineScope) {
    private val shouldAutoPlayKey = booleanPreferencesKey("video_should_autoplay")
    private val muteOnStartKey = booleanPreferencesKey("video_mute_on_start")

    fun getShouldAutoPlay(): Flow<Boolean> =
        context.datastore.data.map {
            it[shouldAutoPlayKey] ?: true
        }

    fun setShouldAutoPlay(value: Boolean) = viewModelScope.launch {
        context.datastore.edit {
            it[shouldAutoPlayKey] = value
        }
    }

    fun getMuteOnStart(): Flow<Boolean> =
        context.datastore.data.map {
            it[muteOnStartKey] ?: false
        }

    fun setMuteOnStart(value: Boolean) = viewModelScope.launch {
        context.datastore.edit {
            it[muteOnStartKey] = value
        }
    }
}

class SettingsLookAndFeelImpl(private val context: Context, private val viewModelScope: CoroutineScope) {
    private val followDarkModeKey = intPreferencesKey("look_and_feel_follow_dark_mode")

    /** 0 is follow system
     * 1 is dark
     * 2 is light */
    fun getFollowDarkMode(): Flow<Int> =
        context.datastore.data.map {
            it[followDarkModeKey] ?: 0
        }

    /** 0 is follow system
     * 1 is dark
     * 2 is light */
    fun setFollowDarkMode(value: Int) = viewModelScope.launch {
        context.datastore.edit {
            it[followDarkModeKey] = value
        }

        AppCompatDelegate.setDefaultNightMode(
        	when(value) {
				1 -> AppCompatDelegate.MODE_NIGHT_YES
				2 -> AppCompatDelegate.MODE_NIGHT_NO

				else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        	}
        )
    }
}
