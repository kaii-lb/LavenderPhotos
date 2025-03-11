package com.kaii.photos.helpers

import androidx.compose.runtime.saveable.mapSaver
import com.kaii.photos.datastore.BottomBarTab
import com.kaii.photos.datastore.StoredDrawable

val BottomBarTabSaver = run {
    val nameKey = "Name"
    val albumPathKey = "AlbumPath"
    val indexKey = "Index"
    val selectedIconKey = "SelectedIcon"
    val unselectedIconKey = "UnselectedIcon"

    mapSaver(
        save = {
            mapOf(
                nameKey to it.name,
                albumPathKey to it.albumPath,
                indexKey to it.index,
                selectedIconKey to it.selectedIcon.storedId,
                unselectedIconKey to it.unselectedIcon.storedId,
            )
        },
        restore = {
            BottomBarTab(
                name = it[nameKey] as String,
                albumPath = it[albumPathKey] as String,
                index = it[indexKey] as Int,
                selectedIcon = StoredDrawable.toResId(storedId = it[selectedIconKey] as Int),
                unselectedIcon = StoredDrawable.toResId(storedId = it[unselectedIconKey] as Int)
            )
        }
    )
}
