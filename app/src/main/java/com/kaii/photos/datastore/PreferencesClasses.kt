package com.kaii.photos.datastore

import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.kaii.photos.R
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// order is important
enum class AlbumSortMode {
    LastModified,
    LastModifiedDesc,
    Alphabetically,
    AlphabeticallyDesc,
    Custom;

    val isDescending: Boolean
        get() = this == LastModifiedDesc || this == AlphabeticallyDesc

    fun flip(): AlbumSortMode =
        when (this) {
            LastModified -> LastModifiedDesc
            LastModifiedDesc -> LastModified
            Alphabetically -> AlphabeticallyDesc
            AlphabeticallyDesc -> Alphabetically
            else -> Custom
        }

    fun byDirection(descending: Boolean): AlbumSortMode =
        if (descending == isDescending) this
        else this.flip()
}

object DefaultTabs {
    object TabTypes {
        val photos = BottomBarTab(
            name = "Photos",
            albumPaths = setOf("main_photos"),
            icon = StoredDrawable.PhotoGrid,
            id = 0,
            storedNameIndex = StoredName.Photos.ordinal
        )

        val secure = BottomBarTab(
            name = "Secure",
            albumPaths = setOf("secure_folder"),
            icon = StoredDrawable.SecureFolder,
            id = 1,
            storedNameIndex = StoredName.Secure.ordinal
        )

        val albums = BottomBarTab(
            name = "Albums",
            albumPaths = setOf("albums_page"),
            icon = StoredDrawable.Albums,
            id = 2,
            storedNameIndex = StoredName.Albums.ordinal
        )

        val search = BottomBarTab(
            name = "Search",
            albumPaths = setOf("search_page"),
            icon = StoredDrawable.Search,
            id = 3,
            storedNameIndex = StoredName.Search.ordinal
        )

        val favourites = BottomBarTab(
            name = "Favourites",
            albumPaths = setOf("favourites_page"),
            icon = StoredDrawable.Favourite,
            id = 4,
            storedNameIndex = StoredName.Favourites.ordinal
        )

        val trash = BottomBarTab(
            name = "Trash",
            albumPaths = setOf("trash_page"),
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
    val albumPaths: Set<String>,
    val icon: StoredDrawable,
    val isCustom: Boolean = false,
    val storedNameIndex: Int? = null
) {
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

    fun toAlbum() =
        AlbumType.Folder(
            id = id.toString(),
            name = name,
            paths = albumPaths,
            pinned = false,
            immichId = null
        )
}

@Serializable
data class AlbumInfo(
    val id: Int,
    val name: String,
    val paths: Set<String>,
    val isCustomAlbum: Boolean = false,
    val isPinned: Boolean = false,
    val immichId: String = ""
)

@Serializable
data class ImmichBasicInfo(
    val endpoint: String,
    val accessToken: String,
    val username: String
) {
    companion object {
        val Empty = ImmichBasicInfo(
            endpoint = "",
            accessToken = "",
            username = ""
        )
    }
}

@Parcelize
@Serializable
sealed interface AlbumType : Parcelable {
    val id: String
    val name: String
    val pinned: Boolean
    val immichId: String?

    @Serializable
    data class Folder(
        override val id: String,
        override val name: String,
        override val pinned: Boolean,
        override val immichId: String?,
        val paths: Set<String>
    ) : AlbumType {
        class NavType : androidx.navigation.NavType<Folder>(isNullableAllowed = false) {
            override fun get(bundle: Bundle, key: String): Folder? {
                return bundle.getString(key)?.let { Json.decodeFromString<Folder>(it) }
            }

            override fun parseValue(value: String): Folder {
                return Json.decodeFromString(Uri.decode(value))
            }

            override fun put(bundle: Bundle, key: String, value: Folder) {
                bundle.putString(key, Json.encodeToString(value))
            }

            override fun serializeAsValue(value: Folder): String {
                return Uri.encode(Json.encodeToString(value))
            }
        }
    }

    @Serializable
    data class Custom(
        override val id: String,
        override val name: String,
        override val pinned: Boolean,
        override val immichId: String?,
    ) : AlbumType {
        class NavType : androidx.navigation.NavType<Custom>(isNullableAllowed = false) {
            override fun get(bundle: Bundle, key: String): Custom? {
                return bundle.getString(key)?.let { Json.decodeFromString<Custom>(it) }
            }

            override fun parseValue(value: String): Custom {
                return Json.decodeFromString(Uri.decode(value))
            }

            override fun put(bundle: Bundle, key: String, value: Custom) {
                bundle.putString(key, Json.encodeToString(value))
            }

            override fun serializeAsValue(value: Custom): String {
                return Uri.encode(Json.encodeToString(value))
            }
        }
    }

    @Serializable
    data class Cloud(
        override val id: String,
        override val name: String,
        override val pinned: Boolean,
        override val immichId: String = id
    ) : AlbumType {
        class NavType : androidx.navigation.NavType<Cloud>(isNullableAllowed = false) {
            override fun get(bundle: Bundle, key: String): Cloud? {
                return bundle.getString(key)?.let { Json.decodeFromString<Cloud>(it) }
            }

            override fun parseValue(value: String): Cloud {
                return Json.decodeFromString(Uri.decode(value))
            }

            override fun put(bundle: Bundle, key: String, value: Cloud) {
                bundle.putString(key, Json.encodeToString(value))
            }

            override fun serializeAsValue(value: Cloud): String {
                return Uri.encode(Json.encodeToString(value))
            }
        }
    }

    @Serializable
    object PlaceHolder : AlbumType {
        @IgnoredOnParcel override val id = ""
        @IgnoredOnParcel override val name = ""
        @IgnoredOnParcel override val pinned = false
        @IgnoredOnParcel override val immichId = null

        class NavType : androidx.navigation.NavType<PlaceHolder>(isNullableAllowed = false) {
            override fun get(bundle: Bundle, key: String): PlaceHolder? {
                return bundle.getString(key)?.let { Json.decodeFromString<PlaceHolder>(it) }
            }

            override fun parseValue(value: String): PlaceHolder {
                return Json.decodeFromString(Uri.decode(value))
            }

            override fun put(bundle: Bundle, key: String, value: PlaceHolder) {
                bundle.putString(key, Json.encodeToString(value))
            }

            override fun serializeAsValue(value: PlaceHolder): String {
                return Uri.encode(Json.encodeToString(value))
            }
        }
    }

    class NavType : androidx.navigation.NavType<AlbumType>(isNullableAllowed = false) {
        override fun get(bundle: Bundle, key: String): AlbumType? {
            return bundle.getString(key)?.let { Json.decodeFromString<AlbumType>(it) }
        }

        override fun parseValue(value: String): AlbumType {
            return Json.decodeFromString(Uri.decode(value))
        }

        override fun put(bundle: Bundle, key: String, value: AlbumType) {
            bundle.putString(key, Json.encodeToString(value))
        }

        override fun serializeAsValue(value: AlbumType): String {
            return Uri.encode(Json.encodeToString(value))
        }
    }
}

@Serializable
data class AlbumGroup(
    val id: String,
    val name: String,
    val pinned: Boolean,
    val albumIds: List<String>
)