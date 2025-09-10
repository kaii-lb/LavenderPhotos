package com.kaii.photos.helpers.editing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.util.lerp
import kotlin.math.abs


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
        override val type: DrawingItems = DrawingItems.Text,
        override val text: DrawableText,
        val keyframes: List<DrawingTextKeyframe>? = null,
        val timespan: Trim? = null
    ) : VideoModification, SharedModification.DrawingText

    data class DrawingImage(
        override val type: DrawingItems = DrawingItems.Image,
        override val image: DrawableImage,
        val timespan: Trim? = null
    ) : VideoModification, SharedModification.DrawingImage
}

data class DrawingTextKeyframe(
    val position: Offset,
    val strokeWidth: Float,
    val rotation: Float,
    val color: Color,
    val time: Float
)

fun interpolateKeyframes(
    keyframes: List<DrawingTextKeyframe>,
    timeMs: Float
): DrawingTextKeyframe? {
    if (keyframes.isEmpty()) {
        return null
    }

    val sortedKeyframes = keyframes.sortedBy { it.time }

    if (timeMs <= sortedKeyframes.first().time) {
        return sortedKeyframes.first()
    }

    if (timeMs >= sortedKeyframes.last().time) {
        return sortedKeyframes.last()
    }

    var i = 0
    while (i < sortedKeyframes.size - 1) {
        val keyframe1 = sortedKeyframes[i]
        val keyframe2 = sortedKeyframes[i + 1]

        if (timeMs >= keyframe1.time && timeMs <= keyframe2.time) {
            val diff1 = abs(timeMs - keyframe1.time)
            val diff2 = abs(timeMs - keyframe2.time)

            val t = if (keyframe1.time == keyframe2.time) {
                0f
            } else {
                (timeMs - keyframe1.time) / (keyframe2.time - keyframe1.time)
            }

            // don't lerp color, only change color when we reach the intended timestamp
            val color = if (timeMs >= keyframe2.time) keyframe2.color else keyframe1.color

            return if (diff1 <= diff2) {
                keyframe1.copy(
                    position = lerp(keyframe1.position, keyframe2.position, t),
                    rotation = lerp(keyframe1.rotation, keyframe2.rotation, t),
                    strokeWidth = lerp(keyframe1.strokeWidth, keyframe2.strokeWidth, t),
                    color = color
                )
            } else {
                keyframe2.copy(
                    position = lerp(keyframe1.position, keyframe2.position, t),
                    rotation = lerp(keyframe1.rotation, keyframe2.rotation, t),
                    strokeWidth = lerp(keyframe1.strokeWidth, keyframe2.strokeWidth, t),
                    color = color
                )
            }
        }
        i++
    }

    return null
}