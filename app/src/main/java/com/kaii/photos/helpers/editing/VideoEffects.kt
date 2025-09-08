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

    data class DrawingPath(
        override val type: DrawingItems,
        override val path: DrawablePath,
        val timespan: Trim? = null
    ) : VideoModification, SharedModification.DrawingPath

    data class DrawingText(
        override val type: DrawingItems,
        override val text: DrawableText,
        val timespan: Trim? = null
    ) : VideoModification, SharedModification.DrawingText

    data class DrawingImage(
        override val type: DrawingItems,
        override val image: DrawableImage,
        val timespan: Trim? = null
    ) : VideoModification, SharedModification.DrawingImage
}

