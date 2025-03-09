package com.kaii.photos.datastore

// order is important
enum class AlbumSortMode {
    LastModified,
    Alphabetically,
    Custom
}

object DefaultTabs {
    object TabTypes {
        val photos = BottomBarTab(name = "Photos", index = 0)
        val secure = BottomBarTab(name = "Secure", index = 1)
        val albums = BottomBarTab(name = "Albums", index = 2)
        val search = BottomBarTab(name = "Search", index = 3)
    }
}

data class BottomBarTab(
    val name: String,
    val albumPath: String? = null,
    val index: Int
) {
    val isCustom = albumPath != null
}