package com.kaii.photos.datastore

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bumptech.glide.Glide
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.EncryptionManager
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.TopBarDetailsFormat
import com.kaii.photos.helpers.baseInternalStorageDirectory
import com.kaii.photos.helpers.tryGetAllAlbums
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

const val separator = "|-SEPARATOR-|"
private const val TAG = "com.kaii.photos.datastore.PreferencesClasses"

class Settings(val context: Context, val viewModelScope: CoroutineScope)

class SettingsAlbumsListImpl(
    private val context: Context,
    private val viewModelScope: CoroutineScope
) {
    private val albumsListKey = stringPreferencesKey("album_folder_path_list")
    private val sortModeKey = intPreferencesKey("album_sort_mode")
    private val sortModeOrderKey = booleanPreferencesKey("album_sort_mode_order")
    private val autoDetectAlbumsKey = booleanPreferencesKey("album_auto_detect")

    val json = Json { ignoreUnknownKeys = true }

    fun addToAlbumsList(albumInfo: AlbumInfo) = viewModelScope.launch {
        if (albumInfo.name == "") return@launch

        context.datastore.edit {
            var stringList = it[albumsListKey]

            if (stringList == null) {
                it[albumsListKey] = json.encodeToString(defaultAlbumsList)
                stringList = it[albumsListKey]
            }

            Log.d(TAG, "ALBUMS STRING LIST $stringList")

            val list = json.decodeFromString<List<AlbumInfo>>(stringList!!).toMutableList()

            if (!list.contains(albumInfo)) {
                list.add(albumInfo)
                it[albumsListKey] = json.encodeToString(list)
            }
        }
    }

    fun removeFromAlbumsList(id: Int) = viewModelScope.launch {
        context.datastore.edit { data ->
            val stringList = data[albumsListKey] ?: json.encodeToString(defaultAlbumsList)

            val list = json.decodeFromString<List<AlbumInfo>>(stringList).toMutableList()

            if (list.find { it.id == id } != null) {
                list.remove(list.first { it.id == id })
                data[albumsListKey] = json.encodeToString(list)
            }
        }
    }

    fun editInAlbumsList(albumInfo: AlbumInfo, newInfo: AlbumInfo) = viewModelScope.launch {
        context.datastore.edit {
            val stringList = it[albumsListKey] ?: json.encodeToString(defaultAlbumsList)

            val list = json.decodeFromString<List<AlbumInfo>>(stringList).toMutableList()

            if (list.contains(albumInfo)) {
                val index = list.indexOf(albumInfo)
                list.remove(albumInfo)
                list.add(index, newInfo)

                it[albumsListKey] = json.encodeToString(list)
            }
        }
    }

    fun getCustomAlbums(): Flow<List<AlbumInfo>> = context.datastore.data.map { data ->
        val list = json.decodeFromString<List<AlbumInfo>>(data[albumsListKey] ?: "[]")

        list.filter {
            it.isCustomAlbum && it.name != ""
        }
    }

    fun getAutoDetectedAlbums(
        displayDateFormat: DisplayDateFormat
    ): Flow<List<AlbumInfo>> =
        tryGetAllAlbums(
            context = context,
            displayDateFormat = displayDateFormat
        )
            .combine(getCustomAlbums()) { first, second ->
                first + second
            }

    fun getNormalAlbums() = channelFlow {
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

            val split = json.decodeFromString<List<AlbumInfo>>(list)

            return@map split.map {
                it.copy(
                    paths = it.paths.map { path ->
                        if (!path.startsWith("/storage/")) baseInternalStorageDirectory + path
                        else path
                    }
                )
            }
        }

        prevList.collectLatest { send(it) }
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
            it[albumsListKey] = json.encodeToString(list)
        }
    }

    fun getAutoDetect() = context.datastore.data.map {
        it[autoDetectAlbumsKey] != false
    }//.stateIn(scope = viewModelScope, initialValue = true, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000))

    fun setAutoDetect(value: Boolean) = viewModelScope.launch {
        context.datastore.edit {
            it[autoDetectAlbumsKey] = value
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
    fun getAllAlbumsOnDevice(
        displayDateFormat: DisplayDateFormat
    ): Flow<List<AlbumInfo>> =
        tryGetAllAlbums(context = context, displayDateFormat = displayDateFormat)
}

class SettingsVersionImpl(
    private val context: Context,
    private val viewModelScope: CoroutineScope
) {
    private val showUpdateNotice = booleanPreferencesKey("show_update_notice")
    private val checkForUpdatesOnStartup = booleanPreferencesKey("check_for_updates_on_startup")
    private val clearGlideCache = booleanPreferencesKey("version_clear_glide_cache")

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

    fun getHasClearedGlideCache() =
        context.datastore.data.map {
            it[clearGlideCache] == true
        }

    fun setHasClearedGlideCache(value: Boolean) = viewModelScope.launch {
        context.datastore.edit {
            it[clearGlideCache] = value
        }
    }
}

class SettingsUserImpl(private val context: Context, private val viewModelScope: CoroutineScope) {
    private val firstStartup = booleanPreferencesKey("first_startup")

    fun getFirstStartup(): Flow<Boolean> =
        context.datastore.data.map {
            it[firstStartup] == true
        }

    fun setFirstStartup(value: Boolean) = viewModelScope.launch {
        context.datastore.edit {
            it[firstStartup] = value
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
    private val preserveDateOnMoveKey = booleanPreferencesKey("permissions_preserve_date_on_move_key")
    private val doNotTrashKey = booleanPreferencesKey("permissions_do_not_trash")

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

    fun getPreserveDateOnMove(): Flow<Boolean> =
        context.datastore.data.map {
            it[preserveDateOnMoveKey] != false
        }

    fun setPreserveDateOnMove(value: Boolean) = viewModelScope.launch {
        context.datastore.edit {
            it[preserveDateOnMoveKey] = value
        }
    }

    fun getDoNotTrash(): Flow<Boolean> =
        context.datastore.data.map {
            it[doNotTrashKey] == true
        }

    fun setDoNotTrash(value: Boolean) = viewModelScope.launch {
        context.datastore.edit {
            it[doNotTrashKey] = value
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
    private val displayDateFormat = intPreferencesKey("look_and_feel_display_date_format")
    private val columnSize = intPreferencesKey("look_and_feel_column_size")
    private val albumColumnSize = intPreferencesKey("look_and_feel_album_column_size")
    private val blackBackgroundForViews = booleanPreferencesKey("look_and_feel_black_background")
    private val showExtraSecureNav = booleanPreferencesKey("look_and_feel_extra_secure")
    private val useRoundedCorners = booleanPreferencesKey("look_and_feel_use_rounded_corners") // for photo grid
    private val topBarDetailsFormat = intPreferencesKey("look_and_feel_top_bar_details_format")

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
    fun setFollowDarkMode(value: Int) = viewModelScope.launch {
        context.datastore.edit {
            it[followDarkModeKey] = value
        }
    }

    fun getDisplayDateFormat(): Flow<DisplayDateFormat> =
        context.datastore.data.map {
            DisplayDateFormat.entries[it[displayDateFormat] ?: 0]
        }

    fun setDisplayDateFormat(format: DisplayDateFormat) = viewModelScope.launch {
        context.datastore.edit {
            it[displayDateFormat] = DisplayDateFormat.entries.indexOf(format)
        }
    }

    fun getColumnSize(): Flow<Int> =
        context.datastore.data.map {
            it[columnSize] ?: 3
        }

    fun setColumnSize(size: Int) = viewModelScope.launch {
        context.datastore.edit {
            it[columnSize] = size
        }
    }

    fun getAlbumColumnSize(): Flow<Int> =
        context.datastore.data.map {
            it[albumColumnSize] ?: 2
        }

    fun setAlbumColumnSize(size: Int) = viewModelScope.launch {
        context.datastore.edit {
            it[albumColumnSize] = size
        }
    }

    fun getUseBlackBackgroundForViews(): Flow<Boolean> =
        context.datastore.data.map {
            it[blackBackgroundForViews] == true
        }

    fun setUseBlackBackgroundForViews(value: Boolean) = viewModelScope.launch {
        context.datastore.edit {
            it[blackBackgroundForViews] = value
        }
    }

    /** shows an extra "navigate to secure page" in the main app dialog */
    fun getShowExtraSecureNav(): Flow<Boolean> =
        context.datastore.data.map {
            it[showExtraSecureNav] == true
        }

    fun setShowExtraSecureNav(value: Boolean) = viewModelScope.launch {
        context.datastore.edit {
            it[showExtraSecureNav] = value
        }
    }

    /** round the thumbnail corners in photo grids */
    fun getUseRoundedCorners(): Flow<Boolean> =
        context.datastore.data.map {
            it[useRoundedCorners] == true
        }

    fun setUseRoundedCorners(value: Boolean) = viewModelScope.launch {
        context.datastore.edit {
            it[useRoundedCorners] = value
        }
    }

    fun getTopBarDetailsFormat(): Flow<TopBarDetailsFormat> =
        context.datastore.data.map {
            TopBarDetailsFormat.entries[it[topBarDetailsFormat] ?: 0]
        }

    fun setTopBarDetailsFormat(format: TopBarDetailsFormat) = viewModelScope.launch {
        context.datastore.edit {
            it[topBarDetailsFormat] = TopBarDetailsFormat.entries.indexOf(format)
        }
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
    private val shouldShowEverything = booleanPreferencesKey("main_photos_show_everything")

    fun getAlbums(): Flow<List<String>> =
        context.datastore.data.map { data ->
            val string = data[mainPhotosAlbumsList] ?: defaultAlbumsList

            val list = mutableListOf<String>()
            string.split(separator).forEach { album ->
                if (!list.contains(album) && album != "") list.add(album.removeSuffix("/"))
            }

            list.map {
                if (!it.startsWith("/storage/")) baseInternalStorageDirectory + it
                else it
            }
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

    fun getShowEverything() =
        context.datastore.data.map {
            it[shouldShowEverything] == true
        }

    fun setShowEverything(value: Boolean) = viewModelScope.launch {
        context.datastore.edit {
            it[shouldShowEverything] = value
        }
    }

    private val defaultAlbumsList =
        "${baseInternalStorageDirectory}DCIM/Camera" + separator +
                "${baseInternalStorageDirectory}Pictures" + separator +
                "${baseInternalStorageDirectory}Pictures/Screenshot" + separator
}

class SettingsDefaultTabsImpl(
    private val context: Context,
    private val viewModelScope: CoroutineScope
) {
    private val defaultTab = stringPreferencesKey("default_tabs_first")
    private val tabList = stringPreferencesKey("default_tabs_list")

    val json = Json { ignoreUnknownKeys = true }

    fun getTabList() = context.datastore.data.map { data ->
        val list = data[tabList] ?: defaultTabList

        try {
            json.decodeFromString<List<BottomBarTab>>(list).map {
                if (it.resourceId != null) {
                    it.copy(
                        name = context.resources.getString(it.resourceId.id)
                    )
                } else {
                    it
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "BottomBarTab Impl has been changed, resetting default tab list...")
            Log.e(TAG, e.toString())
            e.printStackTrace()

            val default = json.decodeFromString<List<BottomBarTab>>(defaultTabList)
            setTabList(default)
            setDefaultTab(default.first())

            default
        }
    }

    fun setTabList(list: List<BottomBarTab>) = viewModelScope.launch {
        context.datastore.edit {
            if (list.isEmpty()) {
                it[tabList] = defaultTabList
                setDefaultTab(DefaultTabs.TabTypes.photos)

                return@edit
            }

            val default = try {
                it[defaultTab]?.let { tab -> json.decodeFromString<BottomBarTab>(tab) } ?: DefaultTabs.TabTypes.photos
            } catch (e: Throwable) {
                Log.e(TAG, "BottomBarTab Impl has been changed, default tab can't be decoded, failing back to DefaultTabs.TabTypes.photos.")
                Log.e(TAG, e.toString())
                e.printStackTrace()

                DefaultTabs.TabTypes.photos
            }

            if (default !in list) {
                setDefaultTab(list.first())
            }

            it[tabList] = json.encodeToString(list)
        }
    }

    fun getDefaultTab() = context.datastore.data.map {
        val default = it[defaultTab]

        try {
            default?.let { string -> json.decodeFromString<BottomBarTab>(string) } ?: DefaultTabs.TabTypes.photos
        } catch (e: Throwable) {
            Log.e(TAG, "BottomBarTab Impl has been changed, resetting default tab...")
            Log.e(TAG, e.toString())
            e.printStackTrace()

            setDefaultTab(DefaultTabs.TabTypes.photos)
            DefaultTabs.TabTypes.photos
        }
    }

    fun setDefaultTab(tab: BottomBarTab) = viewModelScope.launch {
        context.datastore.edit {
            it[defaultTab] = json.encodeToString(tab)
        }
    }

    private val defaultTabList = json.encodeToString(DefaultTabs.defaultList)
}

class SettingsPhotoGridImpl(
    private val context: Context,
    private val viewModelScope: CoroutineScope
) {
    private val mediaSortModeKey = stringPreferencesKey("media_sort_mode")

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
}

class SettingsImmichImpl(
    private val context: Context,
    private val viewModelScope: CoroutineScope
) {
    private val immichEncryptionIV = byteArrayPreferencesKey("immich_encryption_iv")
    private val immichEndpoint = stringPreferencesKey("immich_endpoint")
    private val immichToken = byteArrayPreferencesKey("immich_token")
    private val username = stringPreferencesKey("immich_username")
    private val pfpPath = stringPreferencesKey("immich_pfp_path")
    private val alwaysShowUserInfo =
        booleanPreferencesKey("immich_always_show_user_info") // always show the main app bar's pfp and user name, even if not logged in.

    fun getImmichBasicInfo() = context.datastore.data.map { data ->
        val endpoint = data[immichEndpoint] ?: return@map ImmichBasicInfo.Empty
        val token = data[immichToken] ?: return@map ImmichBasicInfo.Empty
        val iv = data[immichEncryptionIV] ?: return@map ImmichBasicInfo.Empty
        val username = data[username] ?: ""
        val pfpPath = data[pfpPath] ?: ""

        val decToken = EncryptionManager.decryptBytes(
            bytes = token,
            iv = iv
        )

        ImmichBasicInfo(
            endpoint = endpoint,
            bearerToken = decToken.decodeToString(),
            username = username,
            pfpPath = pfpPath
        )
    }

    fun setImmichBasicInfo(info: ImmichBasicInfo) = viewModelScope.launch {
        context.datastore.edit { data ->
            val (enc, iv) = EncryptionManager.encryptBytes(info.bearerToken.toByteArray())

            data[username] = info.username
            data[pfpPath] = info.pfpPath
            data[immichEndpoint] = info.endpoint
            data[immichToken] = enc
            data[immichEncryptionIV] = iv
        }
    }

    fun getAlwaysShowUserInfo() =
        context.datastore.data.map {
            it[alwaysShowUserInfo] == true
        }

    fun setAlwaysShowUserInfo(value: Boolean) = viewModelScope.launch {
        context.datastore.edit {
            it[alwaysShowUserInfo] = value
        }
    }
}

class SettingsBehaviourImpl(
    private val context: Context,
    private val viewModelScope: CoroutineScope
) {
    private val exitImmediately = booleanPreferencesKey("behaviour_exit_immediately")
    private val openVideosExternally = booleanPreferencesKey("behaviour_open_videos_externally")

    fun getExitImmediately() = context.datastore.data.map {
        it[exitImmediately] == true
    }

    fun setExitImmediately(value: Boolean) = viewModelScope.launch {
        context.datastore.edit {
            it[exitImmediately] = value
        }
    }

    fun getOpenVideosExternally() = context.datastore.data.map {
        it[openVideosExternally] == true
    }

    fun setOpenVideosExternally(value: Boolean) = viewModelScope.launch {
        context.datastore.edit {
            it[openVideosExternally] = value
        }
    }
}