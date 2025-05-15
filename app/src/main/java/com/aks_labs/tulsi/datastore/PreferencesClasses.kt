package com.aks_labs.tulsi.datastore

import android.content.Context
import android.provider.MediaStore.Files.FileColumns
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bumptech.glide.Glide
import com.aks_labs.tulsi.helpers.MediaItemSortMode
import com.aks_labs.tulsi.helpers.baseInternalStorageDirectory
import com.aks_labs.tulsi.helpers.getAllAlbumsOnDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.io.path.Path

const val separator = "|-SEPARATOR-|"
private const val TAG = "PREFERENCES_CLASSES"

class Settings(val context: Context, val viewModelScope: CoroutineScope)

class SettingsAlbumsListImpl(
    private val context: Context,
    private val viewModelScope: CoroutineScope
) {
    private val albumsListKey = stringPreferencesKey("album_folder_path_list")
    private val sortModeKey = intPreferencesKey("album_sort_mode")
    private val sortModeOrderKey = booleanPreferencesKey("album_sort_mode_order")
    private val autoDetectAlbums = booleanPreferencesKey("album_auto_detect")

    fun addToAlbumsList(albumInfo: AlbumInfo) = viewModelScope.launch {
        if (albumInfo.name == "") return@launch

        context.datastore.edit {
            var stringList = it[albumsListKey]

            if (stringList == null) {
                it[albumsListKey] = Json.encodeToString(defaultAlbumsList)
                stringList = it[albumsListKey]
            }

            Log.d(TAG, "ALBUMS STRING LIST $stringList")

            val list = Json.decodeFromString<List<AlbumInfo>>(stringList!!).toMutableList()

            if (!list.contains(albumInfo)) {
                list.add(albumInfo)
                it[albumsListKey] = Json.encodeToString(list)
            }
        }
    }

    fun removeFromAlbumsList(id: Int) = viewModelScope.launch {
        context.datastore.edit { data ->
            val stringList = data[albumsListKey] ?: Json.encodeToString(defaultAlbumsList)

            val list = Json.decodeFromString<List<AlbumInfo>>(stringList).toMutableList()

            if (list.find { it.id == id } != null) {
                list.remove(list.first { it.id == id })
                data[albumsListKey] = Json.encodeToString(list)
            }
        }
    }

    fun editInAlbumsList(albumInfo: AlbumInfo, newInfo: AlbumInfo) = viewModelScope.launch {
        context.datastore.edit {
            val stringList = it[albumsListKey] ?: Json.encodeToString(defaultAlbumsList)

            val list = Json.decodeFromString<List<AlbumInfo>>(stringList).toMutableList()

            if (list.contains(albumInfo)) {
                val index = list.indexOf(albumInfo)
                list.remove(albumInfo)
                list.add(index, newInfo)

                it[albumsListKey] = Json.encodeToString(list)
            }
        }
    }

    fun getAlbumsList(): Flow<List<AlbumInfo>> = channelFlow {
        val prevList = context.datastore.data.map { data ->
            val list = data[albumsListKey]
            val isPreV083 =
                list?.startsWith(",") == true// if list starts with a , then its using an old version of list storing system, move to new version
            val isPreV095 = list?.startsWith(separator)

            if (list == null) {
                setAlbumsList(defaultAlbumsList)

                return@map defaultAlbumsList
            } else if (isPreV083) {
                val split = list.split(",").distinct().toMutableList()
                split.remove("")
                split.remove("/storage/emulated/0")

                return@map split.map { path ->
                    AlbumInfo(
                        id = path.hashCode(),
                        name = path.split("/").last(),
                        paths = listOf(path)
                    )
                }
            } else if (isPreV095 == true) {
                val split = list.split(separator).distinct().toMutableList()
                split.remove("")

                return@map split.map { path ->
                    AlbumInfo(
                        id = path.hashCode(),
                        name = path.split("/").last(),
                        paths = listOf(path)
                    )
                }
            }

            val split = Json.decodeFromString<List<AlbumInfo>>(list)

            return@map split
        }

        prevList.collectLatest { send(it) }

        val autoDetectAlbums = getAutoDetect()

        autoDetectAlbums.collectLatest {
            if (it) {
                val list = mutableListOf<AlbumInfo>()
                getAllAlbumsOnDevice().collectLatest { item ->
                    list.add(item)
                    send(list)
                }
                send(list)
            }
        }
    }

    fun setAlbumSortMode(sortMode: AlbumSortMode) = viewModelScope.launch {
        context.datastore.edit {
            it[sortModeKey] = sortMode.ordinal
        }
    }

    fun getAlbumSortMode(): Flow<AlbumSortMode> = context.datastore.data.map {
        AlbumSortMode.entries[it[sortModeKey] ?: AlbumSortMode.LastModified.ordinal]
    }

    fun setSortByDescending(descending: Boolean) = viewModelScope.launch {
        context.datastore.edit {
            it[sortModeOrderKey] = descending
        }
    }

    fun getSortByDescending() = context.datastore.data.map {
        it[sortModeOrderKey] != false
    }

    fun setAlbumsList(list: List<AlbumInfo>) = viewModelScope.launch {
        context.datastore.edit {
            it[albumsListKey] = Json.encodeToString(list)
        }
    }

    fun getAutoDetect() = context.datastore.data.map {
        it[autoDetectAlbums] != false
    }

    fun setAutoDetect(value: Boolean) = viewModelScope.launch {
        context.datastore.edit {
            it[autoDetectAlbums] = value
        }
    }

    val defaultAlbumsList =
        listOf(
            AlbumInfo(
                id = 0,
                name = "Camera",
                paths = listOf("DCIM/Camera")
            ),
            AlbumInfo(
                id = 1,
                name = "WhatsApp Images",
                paths = listOf("Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images")
            ),
            AlbumInfo(
                id = 2,
                name = "Screenshots",
                paths = listOf("Pictures/Screenshot")
            ),
            AlbumInfo(
                id = 3,
                name = "Pictures",
                paths = listOf("Pictures")
            ),
            AlbumInfo(
                id = 4,
                name = "Downloads",
                paths = listOf("Download")
            )
        )

    /** emits one album after the other */
    fun getAllAlbumsOnDevice(): Flow<AlbumInfo> =
        Path(baseInternalStorageDirectory).getAllAlbumsOnDevice()
}

class SettingsVersionImpl(
    private val context: Context,
    private val viewModelScope: CoroutineScope
) {
    private val showUpdateNotice = booleanPreferencesKey("show_update_notice")
    private val checkForUpdatesOnStartup = booleanPreferencesKey("check_for_updates_on_startup")

    fun getShowUpdateNotice(): Flow<Boolean> =
        context.datastore.data.map {
            it[showUpdateNotice] != false
        }

    fun setShowUpdateNotice(value: Boolean) = viewModelScope.launch {
        context.datastore.edit {
            it[showUpdateNotice] = value
        }
    }

    fun getCheckUpdatesOnStartup(): Flow<Boolean> =
        context.datastore.data.map {
            it[checkForUpdatesOnStartup] == true
        }

    fun setCheckUpdatesOnStartup(value: Boolean) = viewModelScope.launch {
        context.datastore.edit {
            it[checkForUpdatesOnStartup] = value
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

class SettingsDebuggingImpl(
    private val context: Context,
    private val viewModelScope: CoroutineScope
) {
    private val recordLogsKey = booleanPreferencesKey("debugging_record_logs")

    fun getRecordLogs(): Flow<Boolean> =
        context.datastore.data.map {
            it[recordLogsKey] != false
        }

    fun setRecordLogs(value: Boolean) = viewModelScope.launch {
        context.datastore.edit {
            it[recordLogsKey] = value
        }
    }
}

class SettingsPermissionsImpl(
    private val context: Context,
    private val viewModelScope: CoroutineScope
) {
    private val isMediaManagerKey = booleanPreferencesKey("is_media_manager")
    private val confirmToDelete = booleanPreferencesKey("confirm_to_delete")
    private val overwriteDateOnMoveKey = booleanPreferencesKey("overwrite_data_on_move")

    fun getIsMediaManager(): Flow<Boolean> =
        context.datastore.data.map {
            it[isMediaManagerKey] == true
        }

    fun setIsMediaManager(value: Boolean) = viewModelScope.launch {
        context.datastore.edit {
            it[isMediaManagerKey] = value
        }
    }

    fun getConfirmToDelete(): Flow<Boolean> =
        context.datastore.data.map {
            it[confirmToDelete] != false
        }

    fun setConfirmToDelete(value: Boolean) = viewModelScope.launch {
        context.datastore.edit {
            it[confirmToDelete] = value
        }
    }

    fun getOverwriteDateOnMove(): Flow<Boolean> =
        context.datastore.data.map {
            it[overwriteDateOnMoveKey] != false
        }

    fun setOverwriteDateOnMove(value: Boolean) = viewModelScope.launch {
        context.datastore.edit {
            it[overwriteDateOnMoveKey] = value
        }
    }
}

class SettingsTrashBinImpl(
    private val context: Context,
    private val viewModelScope: CoroutineScope
) {
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

class SettingsStorageImpl(
    private val context: Context,
    private val viewModelScope: CoroutineScope
) {
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
            it[cacheThumbnailsKey] != false
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
            it[shouldAutoPlayKey] != false
        }

    fun setShouldAutoPlay(value: Boolean) = viewModelScope.launch {
        context.datastore.edit {
            it[shouldAutoPlayKey] = value
        }
    }

    fun getMuteOnStart(): Flow<Boolean> =
        context.datastore.data.map {
            it[muteOnStartKey] == true
        }

    fun setMuteOnStart(value: Boolean) = viewModelScope.launch {
        context.datastore.edit {
            it[muteOnStartKey] = value
        }
    }
}

class SettingsLookAndFeelImpl(
    private val context: Context,
    private val viewModelScope: CoroutineScope
) {
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
            when (value) {
                1 -> AppCompatDelegate.MODE_NIGHT_YES
                2 -> AppCompatDelegate.MODE_NIGHT_NO

                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }
}

class SettingsEditingImpl(
    private val context: Context,
    private val viewModelScope: CoroutineScope
) {
    private val overwriteByDefaultKey = booleanPreferencesKey("editing_overwrite_by_default")
    private val exitOnSaveKey = booleanPreferencesKey("exit_on_save")

    fun getOverwriteByDefault(): Flow<Boolean> =
        context.datastore.data.map {
            it[overwriteByDefaultKey] == true
        }

    fun setOverwriteByDefault(value: Boolean) = viewModelScope.launch {
        context.datastore.edit {
            it[overwriteByDefaultKey] = value
        }
    }

    fun getExitOnSave(): Flow<Boolean> =
        context.datastore.data.map {
            it[exitOnSaveKey] == true
        }

    fun setExitOnSave(value: Boolean) = viewModelScope.launch {
        context.datastore.edit {
            it[exitOnSaveKey] = value
        }
    }
}

class SettingMainPhotosViewImpl(
    private val context: Context,
    private val viewModelScope: CoroutineScope
) {
    private val mainPhotosAlbumsList = stringPreferencesKey("main_photos_albums_list")

    fun getAlbums(): Flow<List<String>> =
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

    /** returns the media store query and the individual paths
     * albums needed cuz the query has ? instead of the actual paths for...reasons */
    fun getSQLiteQuery(albums: List<String>): SQLiteQuery {
        if (albums.isEmpty()) {
            return SQLiteQuery(query = "AND false", paths = null)
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
            val album = albums[i].removeSuffix("/")

            string += " OR $base"
            list.add("$album/")
        }

        val query = "AND ($string)"
        return SQLiteQuery(query = query, paths = list)
    }

    private val defaultAlbumsList =
        "DCIM/Camera" + separator +
                "Pictures" + separator +
                "Pictures/Screenshot" + separator
}

class SettingsDefaultTabsImpl(
    private val context: Context,
    private val viewModelScope: CoroutineScope
) {
    private val defaultTab = stringPreferencesKey("default_bottom_tab")
    private val tabList = stringPreferencesKey("bottom_tab_list")

    fun getTabList() = context.datastore.data.map {
        val list = it[tabList] ?: getDefaultTabList()

        val separatedList = list.split(separator)

        try {
            val typedList = separatedList
                .toMutableList()
                .apply {
                    removeAll { item ->
                        item == ""
                    }
                }
                .map { serialized ->
                    Json.decodeFromString<BottomBarTab>(serialized)
                }

            typedList.forEach { item ->
                Log.d(TAG, "Typed List item $item")
            }

            typedList
        } catch (e: Throwable) {
            Log.e(TAG, "BottomBarTab Impl has been changed, resetting tabs...")
            Log.e(TAG, e.toString())
            e.printStackTrace()

            val tabs = getDefaultTabList()
                .split(separator)
                .toMutableList()
                .apply { removeAll { string -> string == "" } }
                .map { tab -> Json.decodeFromString<BottomBarTab>(tab) }

            setTabList(tabs)

            tabs
        }
    }

    fun setTabList(list: List<BottomBarTab>) = viewModelScope.launch {
        context.datastore.edit {
            if (list.isEmpty()) {
                it[tabList] = getDefaultTabList()
                return@edit
            }

            var stringList = ""

            list.forEach { tab ->
                stringList += Json.encodeToString(tab) + separator
            }

            it[tabList] = stringList
        }
    }

    fun getDefaultTab() = context.datastore.data.map {
        val default = it[defaultTab] ?: Json.encodeToString(DefaultTabs.TabTypes.search)

        try {
            Json.decodeFromString<BottomBarTab>(default)
        } catch (e: Throwable) {
            Log.e(TAG, "BottomBarTab Impl has been changed, resetting default tab...")
            Log.e(TAG, e.toString())
            e.printStackTrace()

            setDefaultTab(DefaultTabs.TabTypes.search)
            DefaultTabs.TabTypes.search
        }
    }

    fun setDefaultTab(tab: BottomBarTab) = viewModelScope.launch {
        context.datastore.edit {
            val serialized = Json.encodeToString(tab)
            it[defaultTab] = serialized
        }
    }

    private fun getDefaultTabList() = run {
        val search = Json.encodeToString(DefaultTabs.TabTypes.search)
        val photos = Json.encodeToString(DefaultTabs.TabTypes.photos)
        val albums = Json.encodeToString(DefaultTabs.TabTypes.albums)
        val secure = Json.encodeToString(DefaultTabs.TabTypes.secure)

        search + separator + photos + separator + albums + separator + secure
    }
}

class SettingsPhotoGridImpl(
    private val context: Context,
    private val viewModelScope: CoroutineScope
) {
    private val mediaSortModeKey = stringPreferencesKey("media_sort_mode")
    private val gridViewModeKey = booleanPreferencesKey("grid_view_mode")

    fun getSortMode() = context.datastore.data.map {
        val name = it[mediaSortModeKey] ?: MediaItemSortMode.DateTaken.name

        MediaItemSortMode.entries.find { entry ->
            entry.name == name
        }
            ?: throw IllegalArgumentException("Sort mode $name does not exist! This should never happen!")
    }

    fun setSortMode(mode: MediaItemSortMode) = viewModelScope.launch {
        context.datastore.edit {
            it[mediaSortModeKey] = mode.name
        }
    }

    // Get the current view mode (true = grid view, false = date-grouped view)
    fun getGridViewMode() = context.datastore.data.map {
        it[gridViewModeKey] ?: true // Default to grid view
    }

    // Set the view mode (true = grid view, false = date-grouped view)
    fun setGridViewMode(isGridView: Boolean) = viewModelScope.launch {
        context.datastore.edit {
            it[gridViewModeKey] = isGridView
        }
    }
}

