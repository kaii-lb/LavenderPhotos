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
        val photos = BottomBarTab(name = "Photos", index = 0, selectedIcon = R.drawable.photogrid_filled, unselectedIcon = R.drawable.photogrid)
        val secure = BottomBarTab(name = "Secure", index = 1, selectedIcon = R.drawable.locked_folder_filled, unselectedIcon = R.drawable.locked_folder)
        val albums = BottomBarTab(name = "Albums", index = 2, selectedIcon = R.drawable.albums_filled, unselectedIcon = R.drawable.albums)
        val search = BottomBarTab(name = "Search", index = 3, selectedIcon = R.drawable.search, unselectedIcon = R.drawable.search)
    }

    val defaultList = listOf(
        TabTypes.photos,
        TabTypes.secure,
        TabTypes.albums,
        TabTypes.search
    )
}

@Serializable
data class BottomBarTab(
    val name: String,
    val albumPath: String? = null,
    val index: Int,
    @DrawableRes val selectedIcon: Int,
    @DrawableRes val unselectedIcon: Int,
) {
    val isCustom = albumPath != null

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
}
