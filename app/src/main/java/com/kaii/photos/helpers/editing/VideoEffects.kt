package com.kaii.photos.helpers.editing

import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.util.UnstableApi


interface VideoModification {
    @OptIn(UnstableApi::class)
    fun toEffect(): Effect =
        if (this is Adjustment) {
            type.getVideoEffect(value)
        } else throw IllegalArgumentException("$this cannot be mapped to ${Effect::class}!")

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

    data class Rotation(
        val degrees: Float
    ) : VideoModification

    data class Volume(
        val percentage: Float
    ) : VideoModification

    data class Speed(
        val multiplier: Float
    ) : VideoModification

    data class FrameDrop(
        val targetFps: Int
    ) : VideoModification

    data class Adjustment(
        val type: MediaAdjustments,
        val value: Float
    ) : VideoModification

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

