package com.kaii.photos.datastore

import androidx.annotation.DrawableRes
import com.kaii.photos.R
import kotlinx.serialization.Serializable

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
            icon = StoredDrawable.PhotoGrid
        )

        val secure = BottomBarTab(
            name = "Secure",
            albumPaths = listOf("secure_folder"),
            index = 1,
            icon = StoredDrawable.SecureFolder
        )

        val albums = BottomBarTab(
            name = "Albums",
            albumPaths = listOf("albums_page"),
            index = 2,
            icon = StoredDrawable.Albums
        )

        val search = BottomBarTab(
            name = "Search",
            albumPaths = listOf("search_page"),
            index = 3,
            icon = StoredDrawable.Search
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
enum class StoredDrawable(
    @DrawableRes val filled: Int,
    @DrawableRes val nonFilled: Int,
    val storedId: Int
) {
    PhotoGrid(
        filled = R.drawable.photogrid_filled,
        nonFilled = R.drawable.photogrid,
        storedId = 0
    ),

    SecureFolder(
    	filled = R.drawable.locked_folder_filled,
        nonFilled = R.drawable.locked_folder,
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
    );

    companion object {
        fun toResId(storedId: Int) = entries.first { it.storedId == storedId }
    }
}

@Serializable
data class BottomBarTab(
    val name: String,
    val albumPaths: List<String>,
    val index: Int,
    val icon: StoredDrawable,
) {
    fun isCustom() = DefaultTabs.defaultList.all { it.albumPaths != albumPaths }

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
        result = 31 * result + isCustom().hashCode()
        return result
    }
}
