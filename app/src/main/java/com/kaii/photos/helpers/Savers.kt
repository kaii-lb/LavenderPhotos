package com.kaii.photos.helpers

import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.ui.geometry.Offset
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.BottomBarTab
import com.kaii.photos.datastore.StoredDrawable
import kotlinx.serialization.json.Json

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
                id = it[indexKey] as Int
            )
        }
    )
}

val OffsetSaver = run {
        val xKey = "x"
        val yKey = "y"

        mapSaver(
            save = {
                mapOf(
                    xKey to it.x,
                    yKey to it.y
                )
            },
            restore = {
                Offset(
                    it[xKey] as Float,
                    it[yKey] as Float
                )
            }
        )
    }

val AlbumInfoSaver = run {
    val idKey = "id"
    val nameKey = "name"
    val pathsKey = "paths"
    val isCustomKey = "is_custom"
    val isPinnedKey = "is_pinned"
    val immichIdKey = "immich_id"

    mapSaver(
        save = {
            mapOf(
                idKey to it.id,
                nameKey to it.name,
                pathsKey to Json.encodeToString(it.paths),
                isCustomKey to it.isCustomAlbum,
                isPinnedKey to it.isPinned,
                immichIdKey to it.immichId
            )
        },
        restore = {
            AlbumInfo(
                id = it[idKey] as Int,
                name = it[nameKey] as String,
                paths = Json.decodeFromString<List<String>>(it[pathsKey] as String),
                isCustomAlbum = it[isCustomKey] as Boolean,
                isPinned = it[isPinnedKey] as Boolean,
                immichId = it[immichIdKey] as String
            )
        }
    )
}