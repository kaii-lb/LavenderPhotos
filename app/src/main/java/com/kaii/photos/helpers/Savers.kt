package com.kaii.photos.helpers

import androidx.compose.runtime.saveable.mapSaver
import com.kaii.photos.datastore.BottomBarTab
import com.kaii.photos.datastore.StoredDrawable

@Suppress("UNCHECKED_CAST") // if the type changes and this doesn't then something always went really bad
val BottomBarTabSaver = run {
    val nameKey = "Name"
    val albumPathKey = "AlbumPath"
    val indexKey = "Index"
    val iconKey = "Icon"

    mapSaver(
        save = {
            mapOf(
                nameKey to it.name,
                albumPathKey to it.albumPaths,
                indexKey to it.index,
                iconKey to it.icon.storedId,
            )
        },
        restore = {
            BottomBarTab(
                name = it[nameKey] as String,
                albumPaths = it[albumPathKey] as List<String>,
                index = it[indexKey] as Int,
                icon = StoredDrawable.toResId(storedId = it[iconKey] as Int),
            )
        }
    )
}
