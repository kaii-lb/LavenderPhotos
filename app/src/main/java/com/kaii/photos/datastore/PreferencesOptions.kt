package com.kaii.photos.datastore

import android.net.Uri
import android.os.Bundle
import androidx.annotation.DrawableRes
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
            index = 0,
            icon = StoredDrawable.PhotoGrid,
            id = 0
        )

        val secure = BottomBarTab(
            name = "Secure",
            albumPaths = listOf("secure_folder"),
            index = 1,
            icon = StoredDrawable.SecureFolder,
            id = 1
        )

        val albums = BottomBarTab(
            name = "Albums",
            albumPaths = listOf("albums_page"),
            index = 2,
            icon = StoredDrawable.Albums,
            id = 2
        )

        val search = BottomBarTab(
            name = "Search",
            albumPaths = listOf("search_page"),
            index = 3,
            icon = StoredDrawable.Search,
            id = 3
        )

        val favourites = BottomBarTab(
            name = "Favourites",
            albumPaths = listOf("favourites_page"),
            index = 4,
            icon = StoredDrawable.Favourite,
            id = 4
        )

        val trash = BottomBarTab(
            name = "Trash",
            albumPaths = listOf("trash_page"),
            index = 5,
            icon = StoredDrawable.Trash,
            id = 5
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
enum class StoredDrawable(
    @param:DrawableRes val filled: Int,
    @param:DrawableRes val nonFilled: Int,
    val storedId: Int
) {
    PhotoGrid(
        filled = R.drawable.photogrid_filled,
        nonFilled = R.drawable.photogrid,
        storedId = 0
    ),

    SecureFolder(
        filled = R.drawable.secure_folder_filled,
        nonFilled = R.drawable.secure_folder,
        storedId = 2
    ),

    Albums(
        filled = R.drawable.albums_filled,
        nonFilled = R.drawable.albums,
        storedId = 4
    ),

    Search(
        filled = R.drawable.search,
        nonFilled = R.drawable.search,
        storedId = 6
    ),

    Favourite(
        filled = R.drawable.favourite_filled,
        nonFilled = R.drawable.favourite,
        storedId = 7
    ),

    Star(
        filled = R.drawable.star_filled,
        nonFilled = R.drawable.star,
        storedId = 8
    ),

    Bolt(
        filled = R.drawable.bolt_filled,
        nonFilled = R.drawable.bolt,
        storedId = 9
    ),

    Face(
        filled = R.drawable.face_filled,
        nonFilled = R.drawable.face,
        storedId = 10
    ),

    Pets(
        filled = R.drawable.pets,
        nonFilled = R.drawable.pets,
        storedId = 11
    ),

    Motorcycle(
        filled = R.drawable.motorcycle_filled,
        nonFilled = R.drawable.motorcycle,
        storedId = 12
    ),

    Motorsports(
        filled = R.drawable.motorsports_filled,
        nonFilled = R.drawable.motorsports,
        storedId = 13
    ),

    Trash(
        filled = R.drawable.trash,
        nonFilled = R.drawable.trash_filled,
        storedId = 14
    );

    companion object {
        fun toResId(storedId: Int) = entries.first { it.storedId == storedId }
    }
}

@Serializable
data class BottomBarTab(
    val id: Int,
    val name: String,
    val albumPaths: List<String>,
    val index: Int,
    val icon: StoredDrawable,
    val isCustom: Boolean = false,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BottomBarTab

        // ignore index since it changes often and doesn't affect the tab itself
        if (name != other.name) return false
        if (albumPaths != other.albumPaths) return false
        if (icon != other.icon) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + albumPaths.hashCode()
        result = 31 * result + icon.hashCode()
        result = 31 * result + isCustom.hashCode()
        return result
    }
}

@Serializable
data class SQLiteQuery(
    val query: String,
    val paths: List<String>?,
    val includedBasePaths: List<String>?
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
        fun createPathOnlyAlbum(paths: List<String>) =
            AlbumInfo(name = "", id = 0, isCustomAlbum = false, paths = paths)
    }

    val mainPath = run {
        paths.firstOrNull() ?: ""
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AlbumInfo

        if (id != other.id) return false
        if (isCustomAlbum != other.isCustomAlbum) return false
        if (name != other.name) return false
        if (paths.toSet() != other.paths.toSet()) return false
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

object AlbumInfoNavType : NavType<AlbumInfo>(
    isNullableAllowed = false
) {
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

@Serializable
data class ImmichBackupMedia(
    val deviceAssetId: String,
    val absolutePath: String,
    val checksum: String
)

@Serializable
data class ImmichBasicInfo(
    val endpoint: String,
    val bearerToken: String
)

