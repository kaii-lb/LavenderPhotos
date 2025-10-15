package com.kaii.photos.helpers

import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.ui.geometry.Offset

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