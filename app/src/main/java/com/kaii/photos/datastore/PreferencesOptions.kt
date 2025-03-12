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
            albumPath = "main_photos",
            index = 0,
            selectedIcon = StoredDrawable.PhotoGridFilled,
            unselectedIcon = StoredDrawable.PhotoGrid
        )

        val secure = BottomBarTab(
            name = "Secure",
            albumPath = "secure_folder",
            index = 1,
            selectedIcon = StoredDrawable.SecureFolderFilled,
            unselectedIcon = StoredDrawable.SecureFolder
        )

        val albums = BottomBarTab(
            name = "Albums",
            albumPath = "albums_page",
            index = 2,
            selectedIcon = StoredDrawable.AlbumsFilled,
            unselectedIcon = StoredDrawable.Albums
        )

        val search = BottomBarTab(
            name = "Search",
            albumPath = "search_page",
            index = 3,
            selectedIcon = StoredDrawable.Search,
            unselectedIcon = StoredDrawable.Search
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
    @DrawableRes val resId: Int,
    val storedId: Int
) {
    PhotoGrid(
        resId = R.drawable.photogrid,
        storedId = 0
    ),
    PhotoGridFilled(
        resId = R.drawable.photogrid_filled,
        storedId = 1
    ),

    SecureFolder(
        resId = R.drawable.locked_folder,
        storedId = 2
    ),
    SecureFolderFilled(
        resId = R.drawable.locked_folder_filled,
        storedId = 3
    ),

    Albums(
        resId = R.drawable.albums,
        storedId = 4
    ),
    AlbumsFilled(
        resId = R.drawable.albums_filled,
        storedId = 5
    ),

    Search(
        resId = R.drawable.search,
        storedId = 6
    );

    companion object {
        fun toResId(storedId: Int) = entries.first { it.storedId == storedId }
    }
}

@Serializable
data class BottomBarTab(
    val name: String,
    val albumPath: String,
    val index: Int,
    val selectedIcon: StoredDrawable,
    val unselectedIcon: StoredDrawable,
) {
    fun isCustom() = DefaultTabs.defaultList.map { it.albumPath }.contains(albumPath)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BottomBarTab

        // ignore index since it changes often and doesn't affect the tab itself
        if (name != other.name) return false
        if (albumPath != other.albumPath) return false
        if (selectedIcon != other.selectedIcon) return false
        if (unselectedIcon != other.unselectedIcon) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + albumPath.hashCode()
        result = 31 * result + selectedIcon.hashCode()
        result = 31 * result + unselectedIcon.hashCode()
        result = 31 * result + isCustom().hashCode()
        return result
    }
}
