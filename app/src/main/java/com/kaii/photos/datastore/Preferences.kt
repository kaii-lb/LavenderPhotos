package com.kaii.photos.datastore

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bumptech.glide.Glide
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.EncryptionManager
import com.kaii.photos.helpers.TopBarDetailsFormat
import com.kaii.photos.helpers.baseInternalStorageDirectory
import com.kaii.photos.helpers.filename
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

const val separator = "|-SEPARATOR-|"
private const val TAG = "com.kaii.photos.datastore.PreferencesClasses"

val Context.datastore by preferencesDataStore(name = "settings")

class Settings(val context: Context, val scope: CoroutineScope) {
    val debugging = SettingsDebuggingImpl(context, scope)

    val permissions = SettingsPermissionsImpl(context, scope)

    val albums = SettingsAlbumsListImpl(context, scope)

    val versions = SettingsVersionImpl(context, scope)

    val storage = SettingsStorageImpl(context, scope)

    val video = SettingsVideoImpl(context, scope)

    val lookAndFeel = SettingsLookAndFeelImpl(context, scope)

    val editing = SettingsEditingImpl(context, scope)

    val mainPhotosView = SettingMainPhotosViewImpl(context, scope)

    val defaultTabs = SettingsDefaultTabsImpl(context, scope)

    val photoGrid = SettingsPhotoGridImpl(context, scope)

    val immich = SettingsImmichImpl(context, scope)

    val behaviour = SettingsBehaviourImpl(context, scope)
}

class SettingsAlbumsListImpl(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val oldAlbumsKey = stringPreferencesKey("album_folder_path_list")
    private val sortModeKey = intPreferencesKey("album_sort_mode")
    private val autoDetectAlbumsKey = booleanPreferencesKey("album_auto_detect")
    private val albumsKey = stringPreferencesKey("album_items_key")

    val json = Json { ignoreUnknownKeys = true }

    fun add(list: List<AlbumInfo>) = scope.launch {
        context.datastore.edit { data ->
            var stringList = data[albumsKey]

            if (stringList == null) {
                set(defaultAlbumsList)
                stringList = jsonDefaultAlbumsList
            }

            val present = json.decodeFromString<List<AlbumInfo>>(stringList).toMutableList()

            val missing = list.filter { album ->
                album.name.isNotBlank() && !present.any { album.equalsIgnoringPinned(it) }
            }

            present.addAll(missing)

            data[albumsKey] = json.encodeToString(present)
        }
    }

    fun get() = context.datastore.data.map { data ->
        var list = data[oldAlbumsKey] ?: jsonDefaultAlbumsList

        val isPreV083 = list.startsWith(",") // if list starts with a "," then its using an old version of list storing system, move to new version
        val isPreV095 = list.startsWith(separator)

        when {
            isPreV083 -> {
                val split = list.split(",").distinct().toMutableList()
                split.remove("")
                split.remove(baseInternalStorageDirectory.removeSuffix("/"))

                val oldList = split.map { path ->
                    AlbumInfo(
                        id = path.hashCode(),
                        name = path.filename(),
                        paths = setOf(path)
                    )
                }
                set(oldList)
                resetOld()
                oldList
            }

            isPreV095 -> {
                val split = list.split(separator).distinct().toMutableList()
                split.remove("")

                val oldList = split.map { path ->
                    AlbumInfo(
                        id = path.hashCode(),
                        name = path.filename(),
                        paths = setOf(baseInternalStorageDirectory + path)
                    )
                }

                set(oldList)
                resetOld()
                oldList
            }

            list.isNotBlank() && !list.startsWith("RESET") -> {
                val split = json.decodeFromString<List<AlbumInfo>>(list)
                set(split)
                resetOld()

                split
            }

            else -> {
                list = data[albumsKey] ?: jsonDefaultAlbumsList
                val split = json.decodeFromString<List<AlbumInfo>>(list)

                split.map {
                    it.copy(
                        paths = it.paths.map { path ->
                            if (!path.startsWith("/storage/")) baseInternalStorageDirectory + path.removePrefix("/")
                            else path
                        }.toSet()
                    )
                }
            }
        }
    }

    fun set(list: List<AlbumInfo>) = scope.launch {
        context.datastore.edit {
            it[albumsKey] = json.encodeToString(list)
        }
    }

    fun remove(albumId: Int) = scope.launch {
        context.datastore.edit { data ->
            val list = data[albumsKey] ?: jsonDefaultAlbumsList
            val present = json.decodeFromString<List<AlbumInfo>>(list).toMutableList()

            present.removeIf {
                it.id == albumId
            }

            data[albumsKey] = json.encodeToString(present)
        }
    }

    fun removeAll(albums: List<Int>) = scope.launch {
        context.datastore.edit { data ->
            val list = data[albumsKey] ?: jsonDefaultAlbumsList
            val present = json.decodeFromString<List<AlbumInfo>>(list).toMutableList()

            present.removeIf {
                it.id in albums
            }

            data[albumsKey] = json.encodeToString(present)
        }
    }

    fun edit(
        id: Int,
        newInfo: AlbumInfo
    ) = scope.launch {
        context.datastore.edit { data ->
            val list = data[albumsKey] ?: jsonDefaultAlbumsList

            val present = json.decodeFromString<List<AlbumInfo>>(list).toMutableList()
            val index = present.indexOfFirst { it.id == id }

            present[index] = newInfo.copy(id = id)

            data[albumsKey] = json.encodeToString(present)
        }
    }

    fun reset() = scope.launch {
        context.datastore.edit {
            it[albumsKey] = jsonDefaultAlbumsList
        }
    }

    private fun resetOld() = scope.launch {
        context.datastore.edit {
            it[oldAlbumsKey] = "RESET" + it[oldAlbumsKey]
        }
    }

    fun setAlbumSortMode(sortMode: AlbumSortMode) = scope.launch {
        context.datastore.edit {
            it[sortModeKey] = sortMode.ordinal
        }
    }

    fun getAlbumSortMode(): Flow<AlbumSortMode> = context.datastore.data.map {
        AlbumSortMode.entries[it[sortModeKey] ?: AlbumSortMode.LastModifiedDesc.ordinal]
    }

    fun getAutoDetect() = context.datastore.data.map {
        it[autoDetectAlbumsKey] != false
    }

    fun setAutoDetect(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[autoDetectAlbumsKey] = value
        }
    }

    val defaultAlbumsList =
        listOf(
            AlbumInfo(
                id = 0,
                name = "Camera",
                paths = setOf("${baseInternalStorageDirectory}DCIM/Camera"),
                isPinned = false
            ),
            AlbumInfo(
                id = 3,
                name = "Pictures",
                paths = setOf("${baseInternalStorageDirectory}Pictures")
            ),
            AlbumInfo(
                id = 4,
                name = "Downloads",
                paths = setOf("${baseInternalStorageDirectory}Download")
            )
        )

    val jsonDefaultAlbumsList = json.encodeToString(defaultAlbumsList)
}

class SettingsVersionImpl(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val showUpdateNotice = booleanPreferencesKey("show_update_notice")
    private val checkForUpdatesOnStartup = booleanPreferencesKey("check_for_updates_on_startup")
    private val clearGlideCache = booleanPreferencesKey("version_clear_glide_cache")
    private val migrateFav = booleanPreferencesKey("version_migrate_fav")

    fun getShowUpdateNotice(): Flow<Boolean> =
        context.datastore.data.map {
            it[showUpdateNotice] != false
        }

    fun setShowUpdateNotice(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[showUpdateNotice] = value
        }
    }

    fun getCheckUpdatesOnStartup(): Flow<Boolean> =
        context.datastore.data.map {
            it[checkForUpdatesOnStartup] == true
        }

    fun setCheckUpdatesOnStartup(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[checkForUpdatesOnStartup] = value
        }
    }

    fun getHasClearedGlideCache() =
        context.datastore.data.map {
            it[clearGlideCache] == true
        }

    fun setHasClearedGlideCache(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[clearGlideCache] = value
        }
    }

    fun getUpdateFav(): Flow<Boolean> =
        context.datastore.data.map { data ->
            data[migrateFav] != false
        }

    fun setUpdateFav(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[migrateFav] = value
        }
    }
}
class SettingsDebuggingImpl(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val recordLogsKey = booleanPreferencesKey("debugging_record_logs")

    fun getRecordLogs(): Flow<Boolean> =
        context.datastore.data.map {
            it[recordLogsKey] != false
        }

    fun setRecordLogs(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[recordLogsKey] = value
        }
    }
}

class SettingsPermissionsImpl(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val isMediaManagerKey = booleanPreferencesKey("is_media_manager")
    private val confirmToDelete = booleanPreferencesKey("confirm_to_delete")
    private val preserveDateOnMoveKey = booleanPreferencesKey("permissions_preserve_date_on_move_key")
    private val doNotTrashKey = booleanPreferencesKey("permissions_do_not_trash")

    fun getIsMediaManager(): Flow<Boolean> =
        context.datastore.data.map {
            it[isMediaManagerKey] == true
        }

    fun setIsMediaManager(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[isMediaManagerKey] = value
        }
    }

    fun getConfirmToDelete(): Flow<Boolean> =
        context.datastore.data.map {
            it[confirmToDelete] != false
        }

    fun setConfirmToDelete(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[confirmToDelete] = value
        }
    }

    fun getPreserveDateOnMove(): Flow<Boolean> =
        context.datastore.data.map {
            it[preserveDateOnMoveKey] != false
        }

    fun setPreserveDateOnMove(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[preserveDateOnMoveKey] = value
        }
    }

    fun getDoNotTrash(): Flow<Boolean> =
        context.datastore.data.map {
            it[doNotTrashKey] == true
        }

    fun setDoNotTrash(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[doNotTrashKey] = value
        }
    }
}

class SettingsStorageImpl(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val thumbnailSizeKey = intPreferencesKey("thumbnail_size_key")
    private val cacheThumbnailsKey = booleanPreferencesKey("cache_thumbnails_key")

    fun getThumbnailSize(): Flow<Int> =
        context.datastore.data.map {
            it[thumbnailSizeKey] ?: 256
        }

    fun setThumbnailSize(value: Int) = scope.launch {
        context.datastore.edit {
            it[thumbnailSizeKey] = value
        }
    }

    fun getCacheThumbnails(): Flow<Boolean> =
        context.datastore.data.map {
            it[cacheThumbnailsKey] != false
        }

    fun setCacheThumbnails(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[cacheThumbnailsKey] = value
        }
    }

    fun clearThumbnailCache() = scope.launch {
        withContext(Dispatchers.IO) {
            Glide.get(context.applicationContext).clearDiskCache()
        }
    }
}

class SettingsVideoImpl(private val context: Context, private val scope: CoroutineScope) {
    private val shouldAutoPlayKey = booleanPreferencesKey("video_should_autoplay")
    private val muteOnStartKey = booleanPreferencesKey("video_mute_on_start")

    fun getShouldAutoPlay(): Flow<Boolean> =
        context.datastore.data.map {
            it[shouldAutoPlayKey] != false
        }

    fun setShouldAutoPlay(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[shouldAutoPlayKey] = value
        }
    }

    fun getMuteOnStart(): Flow<Boolean> =
        context.datastore.data.map {
            it[muteOnStartKey] == true
        }

    fun setMuteOnStart(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[muteOnStartKey] = value
        }
    }
}

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

class SettingsEditingImpl(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val overwriteByDefaultKey = booleanPreferencesKey("editing_overwrite_by_default")
    private val exitOnSaveKey = booleanPreferencesKey("exit_on_save")

    fun getOverwriteByDefault(): Flow<Boolean> =
        context.datastore.data.map {
            it[overwriteByDefaultKey] == true
        }

    fun setOverwriteByDefault(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[overwriteByDefaultKey] = value
        }
    }

    fun getExitOnSave(): Flow<Boolean> =
        context.datastore.data.map {
            it[exitOnSaveKey] == true
        }

    fun setExitOnSave(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[exitOnSaveKey] = value
        }
    }
}

class SettingMainPhotosViewImpl(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val mainPhotosAlbumsList = stringPreferencesKey("main_photos_albums_list")
    private val shouldShowEverything = booleanPreferencesKey("main_photos_show_everything")

    fun getAlbums(): Flow<Set<String>> =
        context.datastore.data.map { data ->
            val string = data[mainPhotosAlbumsList] ?: defaultAlbumsList

            val list = mutableListOf<String>()
            string.split(separator).forEach { album ->
                if (!list.contains(album) && album != "") list.add(album.removeSuffix("/"))
            }

            list.map {
                if (!it.startsWith("/storage/")) baseInternalStorageDirectory + it
                else it
            }.toSet()
        }

    fun addAlbum(relativePath: String) = scope.launch {
        context.datastore.edit {
            var list = it[mainPhotosAlbumsList] ?: defaultAlbumsList

            val addedPath = relativePath.removeSuffix("/") + separator

            if (!list.contains(addedPath)) list += addedPath

            it[mainPhotosAlbumsList] = list
        }
    }

    fun clear() = scope.launch {
        context.datastore.edit {
            it[mainPhotosAlbumsList] = ""
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

    private val defaultAlbumsList =
        "${baseInternalStorageDirectory}DCIM/Camera" + separator +
                "${baseInternalStorageDirectory}Pictures" + separator +
                "${baseInternalStorageDirectory}Pictures/Screenshot" + separator
}

class SettingsDefaultTabsImpl(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val defaultTab = stringPreferencesKey("default_tabs_first")
    private val tabList = stringPreferencesKey("default_tabs_list")

    fun getTabList() = context.datastore.data.map { data ->
        var list = data[tabList] ?: defaultTabListJson

        try {
            if (list.contains("resourceId")) {
                val regex = """("resourceId"):"([^"]+)"""".toRegex()

                list = list.replace(regex) { matchResult ->
                    val value = matchResult.groupValues[2]
                    val index = StoredName.entries.map { it.name }.indexOf(value)

                    if (index != -1) {
                        "\"storedNameIndex\":\"$index\""
                    } else {
                        matchResult.value
                    }
                }

                setTabList(Json.decodeFromString<List<BottomBarTab>>(list))
            }

            Json.decodeFromString<List<BottomBarTab>>(list).map {
                if (it.storedNameIndex != null) {
                    it.copy(
                        name = context.resources.getString(
                            StoredName.entries[it.storedNameIndex].id
                        )
                    )
                } else {
                    it
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "BottomBarTab Impl has been changed, resetting default tab list...")
            Log.e(TAG, e.toString())
            e.printStackTrace()

            setTabList(defaultTabList)
            setDefaultTab(defaultTabItem)

            defaultTabList
        }
    }

    fun setTabList(list: List<BottomBarTab>) = scope.launch {
        context.datastore.edit {
            if (list.isEmpty()) {
                it[tabList] = defaultTabListJson
                setDefaultTab(defaultTabItem)

                return@edit
            }

            val default = try {
                it[defaultTab]?.let { tab -> Json.decodeFromString<BottomBarTab>(tab) } ?: defaultTabItem
            } catch (e: Throwable) {
                Log.e(TAG, "BottomBarTab Impl has been changed, default tab can't be decoded, failing back to DefaultTabs.TabTypes.photos.")
                Log.e(TAG, e.toString())
                e.printStackTrace()

                defaultTabItem
            }

            if (default !in list) {
                setDefaultTab(list.first())
            }

            it[tabList] = Json.encodeToString(list)
        }
    }

    fun getDefaultTab() = context.datastore.data.map {
        val default = it[defaultTab]

        try {
            default?.let { string -> Json.decodeFromString<BottomBarTab>(string) } ?: defaultTabItem
        } catch (e: Throwable) {
            Log.e(TAG, "BottomBarTab Impl has been changed, resetting default tab...")
            Log.e(TAG, e.toString())
            e.printStackTrace()

            setDefaultTab(defaultTabItem)
            defaultTabItem
        }
    }

    fun setDefaultTab(tab: BottomBarTab) = scope.launch {
        context.datastore.edit {
            it[defaultTab] = Json.encodeToString(tab)
        }
    }

    val defaultTabList =
        DefaultTabs.defaultList.map {
            if (it.storedNameIndex != null) {
                it.copy(
                    name = context.resources.getString(
                        StoredName.entries[it.storedNameIndex].id
                    )
                )
            } else {
                it
            }
        }

    private val defaultTabListJson = Json.encodeToString(defaultTabList)

    val defaultTabItem =
        DefaultTabs.TabTypes.photos.copy(
            name = context.resources.getString(
                StoredName.entries[DefaultTabs.TabTypes.photos.storedNameIndex!!].id
            )
        )
}

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

class SettingsImmichImpl(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val immichEncryptionIV = byteArrayPreferencesKey("immich_encryption_iv")
    private val immichEndpoint = stringPreferencesKey("immich_endpoint")
    private val immichToken = byteArrayPreferencesKey("immich_token")
    private val username = stringPreferencesKey("immich_username")
    private val alwaysShowUserInfo =
        booleanPreferencesKey("immich_always_show_user_info") // always show the main app bar's pfp and username, even if not logged in.

    fun getImmichBasicInfo() = context.datastore.data.map { data ->
        val endpoint = data[immichEndpoint] ?: return@map ImmichBasicInfo.Empty
        val token = data[immichToken] ?: return@map ImmichBasicInfo.Empty
        val iv = data[immichEncryptionIV] ?: return@map ImmichBasicInfo.Empty
        val username = data[username] ?: ""

        val decToken = EncryptionManager.decryptBytes(
            bytes = token,
            iv = iv
        )

        ImmichBasicInfo(
            endpoint = endpoint,
            accessToken = decToken.decodeToString(),
            username = username
        )
    }

    fun setImmichBasicInfo(info: ImmichBasicInfo) = scope.launch {
        context.datastore.edit { data ->
            val (enc, iv) = EncryptionManager.encryptBytes(info.accessToken.toByteArray())

            data[username] = info.username
            data[immichEndpoint] = info.endpoint
            data[immichToken] = enc
            data[immichEncryptionIV] = iv
        }
    }

    fun getAlwaysShowUserInfo() =
        context.datastore.data.map {
            it[alwaysShowUserInfo] == true
        }

    fun setAlwaysShowUserInfo(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[alwaysShowUserInfo] = value
        }
    }
}

class SettingsBehaviourImpl(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val exitImmediately = booleanPreferencesKey("behaviour_exit_immediately")
    private val openVideosExternally = booleanPreferencesKey("behaviour_open_videos_externally")

    fun getExitImmediately() = context.datastore.data.map {
        it[exitImmediately] == true
    }

    fun setExitImmediately(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[exitImmediately] = value
        }
    }

    fun getOpenVideosExternally() = context.datastore.data.map {
        it[openVideosExternally] == true
    }

    fun setOpenVideosExternally(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[openVideosExternally] = value
        }
    }
}