package com.kaii.photos.helpers

import androidx.compose.runtime.saveable.mapSaver
import com.kaii.photos.datastore.BottomBarTab

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
                selectedIconKey to it.selectedIcon,
                unselectedIconKey to it.unselectedIcon
            )
        },
        restore = {
            BottomBarTab(
                name = it[nameKey] as String,
                albumPath = it[albumPathKey] as String,
                index = it[indexKey] as Int,
                selectedIcon = it[selectedIconKey] as Int,
                unselectedIcon = it[unselectedIconKey] as Int
            )
        }
    )
}