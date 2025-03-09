package com.kaii.photos.datastore

import android.content.Context
import android.provider.MediaStore.Files.FileColumns
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bumptech.glide.Glide
import com.kaii.photos.helpers.baseInternalStorageDirectory
import com.kaii.photos.helpers.getAllAlbumsOnDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.io.path.Path

const val separator = "|-SEPARATOR-|"
private const val TAG = "PREFERENCES_CLASSES"

class Settings(val context: Context, val viewModelScope: CoroutineScope)

class SettingsAlbumsListImpl(private val context: Context, private val viewModelScope: CoroutineScope) {
    private val albumsListKey = stringPreferencesKey("album_folder_path_list")
    private val sortModeKey = intPreferencesKey("album_sort_mode")
    private val sortModeOrderKey = booleanPreferencesKey("album_sort_mode_order")
    private val autoDetectAlbums = booleanPreferencesKey("album_auto_detect")

    fun addToAlbumsList(path: String) = viewModelScope.launch {
    	if (path == "") return@launch

        context.datastore.edit {
            val stringList = it[albumsListKey]

            if (stringList == null) it[albumsListKey] = ""

            Log.d(TAG, "ALBUMS STRING LIST $stringList")
            Log.d(TAG, "ALBUMS KEYS PATH $path ${stringList?.contains("$separator$path") == false}")

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

    fun getAlbumsList(): Flow<List<String>> = channelFlow {
        val prevList = context.datastore.data.map { data ->
            val list = data[albumsListKey]
            val isPreV083 = list?.startsWith(",") == true// if list starts with a , then its using an old version of list storing system, move to new version

            if (list == null) {
                val defaultList = getDefaultAlbumsList()
                setAlbumsList(defaultList)
                return@map defaultList
            } else if (isPreV083) {
                val split = list.split(",").distinct().toMutableList()
                split.remove("")
                split.remove("/storage/emulated/0")

                return@map split
            }

            val split = list.split(separator).distinct().toMutableList()

            split.remove("")

            return@map split
        }

        prevList.collectLatest { send(it) }

        val autoDetectAlbums = getAutoDetect()

        autoDetectAlbums.collectLatest {
            if (it) {
                send(getAllAlbumsOnDevice())
            }
        }
    }

    fun setAlbumSortMode(sortMode: AlbumSortMode) = viewModelScope.launch {
        context.datastore.edit {
            it[sortModeKey] = sortMode.ordinal
        }
    }

    fun getAlbumSortMode() : Flow<AlbumSortMode> = context.datastore.data.map {
        AlbumSortMode.entries[it[sortModeKey] ?: AlbumSortMode.LastModified.ordinal]
    }

    fun setSortByDescending(descending: Boolean) = viewModelScope.launch {
        context.datastore.edit {
            it[sortModeOrderKey] = descending
        }
    }

    fun getSortByDescending() = context.datastore.data.map {
        it[sortModeOrderKey] ?: true
    }

    fun setAlbumsList(list: List<String>) = viewModelScope.launch {
        context.datastore.edit {
            var stringList = ""
            list.distinct().forEach { album ->
                if (!stringList.contains("$separator$it") && !stringList.contains("$it$separator")) stringList += "$separator$album"
            }

            it[albumsListKey] = stringList
        }
    }

    fun getAutoDetect() = context.datastore.data.map {
        it[autoDetectAlbums] ?: true
    }

    fun setAutoDetect(value: Boolean) = viewModelScope.launch {
        context.datastore.edit {
            it[autoDetectAlbums] = value
        }
    }

    private fun getDefaultAlbumsList() =
        listOf(
            "DCIM/Camera",
            "Pictures",
            "Pictures/Screenshot",
            "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images",
            "Download"
        )

	suspend fun getAllAlbumsOnDevice() : List<String> = withContext(Dispatchers.IO) {
		Path(baseInternalStorageDirectory).getAllAlbumsOnDevice()
	}
}

class SettingsVersionImpl(private val context: Context, private val viewModelScope: CoroutineScope) {
    private val migrateToEncryptedSecurePhotos = booleanPreferencesKey("migrate_to_encrypted_secure_media")

    fun getShouldMigrateToEncryptedSecurePhotos(): Flow<Boolean> =
    	context.datastore.data.map {
    		it[migrateToEncryptedSecurePhotos] ?: true
    	}

   	fun setShouldMigrateToEncryptedSecurePhotos(value: Boolean) = viewModelScope.launch {
   		context.datastore.edit {
   			it[migrateToEncryptedSecurePhotos] = value
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

class SettingsDebuggingImpl(private val context: Context, private val viewModelScope: CoroutineScope) {
    private val recordLogsKey = booleanPreferencesKey("debugging_record_logs")

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

class SettingsEditingImpl(private val context: Context, private val viewModelScope: CoroutineScope) {
    private val overwriteByDefaultKey = booleanPreferencesKey("editing_overwrite_by_default")

    fun getOverwriteByDefault(): Flow<Boolean> =
        context.datastore.data.map {
            it[overwriteByDefaultKey] ?: false
        }

    fun setOverwriteByDefault(value: Boolean) = viewModelScope.launch {
        context.datastore.edit {
            it[overwriteByDefaultKey] = value
        }
    }
}

class SettingMainPhotosViewImpl(private val context: Context, private val viewModelScope: CoroutineScope) {
	private val mainPhotosAlbumsList = stringPreferencesKey("main_photos_albums_list")

	fun getAlbums() : Flow<List<String>> =
		context.datastore.data.map {
			val string = it[mainPhotosAlbumsList] ?: defaultAlbumsList

            val list = mutableListOf<String>()
            string.split(separator).forEach { album ->
                if (!list.contains(album) && album != "") list.add(album.removeSuffix("/"))
            }

            list
		}

	fun addAlbum(relativePath: String) = viewModelScope.launch {
		context.datastore.edit {
			var list = it[mainPhotosAlbumsList] ?: defaultAlbumsList

			val addedPath = relativePath.removeSuffix("/") + separator

			if (!list.contains(addedPath)) list += addedPath

			it[mainPhotosAlbumsList] = list
		}
	}

	fun clear() = viewModelScope.launch {
		context.datastore.edit {
			it[mainPhotosAlbumsList] = ""
		}
	}

	/** first item is the match paths
	    second item is the non match paths
	    non match means subAlbums */
	fun getSQLiteQuery(albums: List<String>) : Pair<String, List<String>?> {
		if (albums.isEmpty()) {
            return Pair("AND false", null)
        }

		albums.forEach {
			Log.d(TAG, "Trying to get query for album: $it")
		}

		val colName = FileColumns.RELATIVE_PATH
		val base = "($colName = ?)"

		val list = mutableListOf<String>()
		var string = base
		val firstAlbum = albums.first().apply {
			removeSuffix("/")
		}
		list.add("$firstAlbum/")

		for (i in 1..<albums.size) {
			val album = albums[i].apply {
				removeSuffix("/")
			}

			string += " OR $base"
			list.add("$album/")
		}

		val query = "AND ($string)"
		return Pair(query, list)
	}

	private val defaultAlbumsList =
		"DCIM/Camera" + separator +
		"Pictures" + separator +
		"Pictures/Screenshot" + separator
}

class SettingsDefaultTabsImpl(private val context: Context, private val viewModelScope: CoroutineScope) {
    private val defaultTab = stringPreferencesKey("default_open_tab")
    private val tabList = stringPreferencesKey("tab_list")

    fun getTabList() = context.datastore.data.map {
        val list = it[tabList] ?: getDefaultTabList()

        val separatedList = list.split(separator)

        val typedList = separatedList
        	.toMutableList()
        	.apply {
	        	removeAll { item ->
	        		item == ""
	        	}
			}
        	.chunked(2)
        	.map { nameAndIndex ->
	            val name = nameAndIndex[0]
	            val index = nameAndIndex[1].toInt()

	            BottomBarTab(
	                name = name,
	                index = index
	            )
	        }

        typedList.forEach { item ->
            Log.d(TAG, "Typed List item $item")
        }

        typedList
    }

    fun setTabList(list: List<BottomBarTab>) = viewModelScope.launch {
        context.datastore.edit {
            if (list.isEmpty()) {
                it[tabList] = getDefaultTabList()
                return@edit
            }

            var stringList = ""

            list.forEach { tab ->
                stringList += "$separator${tab.name}$separator${tab.index}"
            }

            it[tabList] = stringList
        }
    }

    fun getDefaultTab() = context.datastore.data.map {
        val default = it[defaultTab] ?: (DefaultTabs.TabTypes.photos.name + separator + DefaultTabs.TabTypes.photos.index)

        val pair = default.split(separator)

        BottomBarTab(
            name = pair[0],
            index = pair[1].toInt()
        )
    }

    fun setDefaultTab(tabName: String, tabIndex: Int) = viewModelScope.launch {
        context.datastore.edit {
            it[defaultTab] = tabName + separator + tabIndex
        }
    }

    private fun getDefaultTabList() = run {
        separator + DefaultTabs.TabTypes.photos.name + "${separator}${DefaultTabs.TabTypes.photos.index}" +
                separator + DefaultTabs.TabTypes.secure.name + "${separator}${DefaultTabs.TabTypes.secure.index}" +
                separator + DefaultTabs.TabTypes.albums.name + "${separator}${DefaultTabs.TabTypes.albums.index}" +
                separator + DefaultTabs.TabTypes.search.name + "${separator}${DefaultTabs.TabTypes.search.index}"
    }
}
