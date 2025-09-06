package com.kaii.photos.helpers.editing


interface VideoModification {
    data class Trim(
        val start: Float,
        val end: Float
    ) : VideoModification

    data class Crop(
        val top: Float,
        val left: Float,
        val width: Float,
        val height: Float
    ) : VideoModification {
        val right = left + width
        val bottom = top + height
    }

    data class Adjustment(
        val type: MediaAdjustments,
        val value: Float
    ) : VideoModification {
        fun toEffect() =
            type.getVideoEffect(value)
    }

    data class Filter(
        val type: MediaColorFilters
    ) : VideoModification

    data class DrawingText(
        val type: DrawingItems,
        val text: DrawableText,
        val timespan: Trim? = null
    ) : VideoModification

    data class DrawingPath(
        val type: DrawingItems,
        val path: DrawablePath,
        val timespan: Trim? = null
    ) : VideoModification

    data class DrawingImage(
        val type: DrawingItems,
        val image: DrawableImage,
        val timespan: Trim? = null
    ) : VideoModification
}

