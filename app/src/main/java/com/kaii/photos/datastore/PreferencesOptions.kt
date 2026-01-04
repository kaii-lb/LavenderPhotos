package com.kaii.photos.datastore

import android.net.Uri
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.saveable.listSaver
import androidx.navigation.NavType
import com.kaii.photos.R
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// order is important
enum class AlbumSortMode {
    LastModified,
    Alphabetically,
    Custom
}

object DefaultTabs {
    object TabTypes {
        val photos = BottomBarTab(
            name = "Photos",
            albumPaths = listOf("main_photos"),
            icon = StoredDrawable.PhotoGrid,
            id = 0,
            storedNameIndex = StoredName.Photos.ordinal
        )

        val secure = BottomBarTab(
            name = "Secure",
            albumPaths = listOf("secure_folder"),
            icon = StoredDrawable.SecureFolder,
            id = 1,
            storedNameIndex = StoredName.Secure.ordinal
        )

        val albums = BottomBarTab(
            name = "Albums",
            albumPaths = listOf("albums_page"),
            icon = StoredDrawable.Albums,
            id = 2,
            storedNameIndex = StoredName.Albums.ordinal
        )

        val search = BottomBarTab(
            name = "Search",
            albumPaths = listOf("search_page"),
            icon = StoredDrawable.Search,
            id = 3,
            storedNameIndex = StoredName.Search.ordinal
        )

        val favourites = BottomBarTab(
            name = "Favourites",
            albumPaths = listOf("favourites_page"),
            icon = StoredDrawable.Favourite,
            id = 4,
            storedNameIndex = StoredName.Favourites.ordinal
        )

        val trash = BottomBarTab(
            name = "Trash",
            albumPaths = listOf("trash_page"),
            icon = StoredDrawable.Trash,
            id = 5,
            storedNameIndex = StoredName.Trash.ordinal
        )
    }

    val defaultList = listOf(
        TabTypes.photos,
        TabTypes.secure,
        TabTypes.albums,
        TabTypes.search
    )

    val extendedList = defaultList + listOf(TabTypes.favourites, TabTypes.trash)
}

@Serializable
enum class StoredName(
    @param:StringRes val id: Int
) {
    Photos(id = R.string.navigation_photos),
    Secure(id = R.string.navigation_secure),
    Albums(id = R.string.navigation_albums),
    Search(id = R.string.navigation_search),
    Favourites(id = R.string.navigation_favourites),
    Trash(id = R.string.navigation_trash)
}

@Serializable
enum class StoredDrawable(
    @param:DrawableRes val filled: Int,
    @param:DrawableRes val nonFilled: Int
) {
    PhotoGrid(
        filled = R.drawable.photogrid_filled,
        nonFilled = R.drawable.photogrid
    ),

    SecureFolder(
        filled = R.drawable.secure_folder_filled,
        nonFilled = R.drawable.secure_folder
    ),

    Albums(
        filled = R.drawable.albums_filled,
        nonFilled = R.drawable.albums
    ),

    Search(
        filled = R.drawable.search,
        nonFilled = R.drawable.search
    ),

    Favourite(
        filled = R.drawable.favourite_filled,
        nonFilled = R.drawable.favourite
    ),

    Star(
        filled = R.drawable.star_filled,
        nonFilled = R.drawable.star
    ),

    Bolt(
        filled = R.drawable.bolt_filled,
        nonFilled = R.drawable.bolt
    ),

    Face(
        filled = R.drawable.face_filled,
        nonFilled = R.drawable.face
    ),

    Pets(
        filled = R.drawable.pets,
        nonFilled = R.drawable.pets
    ),

    Motorcycle(
        filled = R.drawable.motorcycle_filled,
        nonFilled = R.drawable.motorcycle
    ),

    Motorsports(
        filled = R.drawable.motorsports_filled,
        nonFilled = R.drawable.motorsports
    ),

    Trash(
        filled = R.drawable.trash,
        nonFilled = R.drawable.trash_filled
    )
}

@Serializable
data class BottomBarTab(
    val id: Int,
    val name: String,
    val albumPaths: List<String>,
    val icon: StoredDrawable,
    val isCustom: Boolean = false,
    val storedNameIndex: Int? = null
) {
    companion object {
        @Suppress("UNCHECKED_CAST") // if the type changes and this doesn't then something always went really bad
        val TabSaver =
            listSaver(
                save = {
                    listOf(
                        it.name,
                        it.albumPaths,
                        it.icon.ordinal,
                        it.id,
                        it.isCustom,
                        it.storedNameIndex
                    )
                },
                restore = {
                    BottomBarTab(
                        name = it[0] as String,
                        albumPaths = it[1] as List<String>,
                        icon = StoredDrawable.entries[it[2] as Int],
                        id = it[3] as Int,
                        isCustom = it[4] as Boolean,
                        storedNameIndex = it[2] as Int
                    )
                }
            )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BottomBarTab

        // ignore resource id since it changes often and doesn't affect the tab itself
        // ignore name since its language dependent
        if (albumPaths != other.albumPaths) return false
        if (icon != other.icon) return false
        if (isCustom != other.isCustom) return false
        if (storedNameIndex != other.storedNameIndex) return false

        return true
    }

    override fun hashCode(): Int {
        var result = albumPaths.hashCode()
        result = 31 * result + icon.hashCode()
        result = 31 * result + isCustom.hashCode()
        return result
    }

    fun toAlbumInfo() =
        AlbumInfo(
            id = id,
            name = name,
            paths = albumPaths,
            isCustomAlbum = false
        )
}

@Serializable
data class SQLiteQuery(
    val query: String,
    val paths: List<String>?,
    val basePaths: List<String>?
)

@Serializable
data class AlbumInfo(
    val id: Int,
    val name: String,
    val paths: List<String>,
    val isCustomAlbum: Boolean = false,
    val isPinned: Boolean = false,
    val immichId: String = ""
) {
    companion object {
        fun createPathOnlyAlbum(paths: List<String>) = AlbumInfo(id = 0, name = "", paths = paths)
    }

    object AlbumNavType : NavType<AlbumInfo>(isNullableAllowed = false) {
        override fun get(bundle: Bundle, key: String): AlbumInfo? {
            return bundle.getString(key)?.let { Json.decodeFromString<AlbumInfo>(it) }
        }

        override fun parseValue(value: String): AlbumInfo {
            return Json.decodeFromString(Uri.decode(value))
        }

        override fun put(bundle: Bundle, key: String, value: AlbumInfo) {
            bundle.putString(key, Json.encodeToString(value))
        }

        override fun serializeAsValue(value: AlbumInfo): String {
            return Uri.encode(Json.encodeToString(value))
        }
    }

    val mainPath
        get() = paths.firstOrNull() ?: ""

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AlbumInfo

        if (id != other.id) return false
        if (isCustomAlbum != other.isCustomAlbum) return false
        if (name != other.name) return false
        if (paths.toSet() != other.paths.toSet()) return false // as a set since we don't care about the order
        if (mainPath != other.mainPath) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + isCustomAlbum.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + paths.toSet().hashCode()
        result = 31 * result + mainPath.hashCode()
        return result
    }
}

@Serializable
data class ImmichBackupMedia(
    val deviceAssetId: String,
    val absolutePath: String,
    val checksum: String
)

@Serializable
data class ImmichBasicInfo(
    val endpoint: String,
    val bearerToken: String,
    val username: String,
    val pfpPath: String
) {
    companion object {
        val Empty = ImmichBasicInfo("", "", "", "")
    }
}

