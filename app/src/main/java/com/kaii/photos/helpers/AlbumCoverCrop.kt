package com.kaii.photos.helpers

import kotlin.math.min
import kotlin.math.roundToInt

/** A square in source-image pixels. Used by both the preview and the saved cover. */
data class AlbumCoverCrop(val left: Int, val top: Int, val size: Int) {
    companion object {
        fun calculate(
            width: Int,
            height: Int,
            zoom: Float,
            horizontalPosition: Float,
            verticalPosition: Float
        ): AlbumCoverCrop {
            require(width > 0 && height > 0)
            val size = (min(width, height) / zoom.coerceIn(1f, 5f))
                .roundToInt().coerceIn(1, min(width, height))
            return AlbumCoverCrop(
                left = ((width - size) * horizontalPosition.coerceIn(0f, 1f)).roundToInt(),
                top = ((height - size) * verticalPosition.coerceIn(0f, 1f)).roundToInt(),
                size = size
            )
        }
    }
}
