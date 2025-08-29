package com.kaii.photos.compose.single_photo.editing_view

import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.media3.common.Effect
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.RgbMatrix
import com.kaii.photos.R
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow


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
}

private interface MediaAdjustmentsImpl {
    fun getMatrix(value: Float): FloatArray

    @OptIn(UnstableApi::class)
    fun getVideoEffect(value: Float): Effect

    @get:StringRes val title: Int
    @get:DrawableRes val icon: Int
    val startValue: Float
        get() = 0f
    val availableOnVideos: Boolean
        get() = true
}

@OptIn(UnstableApi::class)
enum class MediaAdjustments : MediaAdjustmentsImpl {
    Contrast {
        override val title = R.string.editing_contrast
        override val icon = R.drawable.contrast

        override fun getMatrix(value: Float): FloatArray {
            return floatArrayOf(value)
        }

        override fun getVideoEffect(value: Float): Effect {
            return Contrast(value)
        }
    },

    Brightness {
        override val title = R.string.editing_brightness
        override val icon = R.drawable.brightness

        override fun getMatrix(value: Float): FloatArray {
            return floatArrayOf(value)
        }

        override fun getVideoEffect(value: Float): Effect {
            return Brightness(value)
        }
    },

    Saturation {
        override val title = R.string.editing_saturation
        override val icon = R.drawable.saturation

        override fun getMatrix(value: Float): FloatArray {
            return floatArrayOf(value * 100f)
        }

        override fun getVideoEffect(value: Float): Effect {
            return HslAdjustment.Builder().adjustSaturation(value * 100f).build()
        }
    },

    BlackPoint {
        override val title = R.string.editing_black_point
        override val icon = R.drawable.file_is_selected_background
        override val availableOnVideos = false

        override fun getMatrix(value: Float): FloatArray = TODO()

        override fun getVideoEffect(value: Float): Effect = TODO("Black point adjustment is not available for video")
    },

    WhitePoint {
        override val title = R.string.editing_white_point
        override val icon = R.drawable.file_not_selected_background
        override val availableOnVideos = false

        override fun getMatrix(value: Float): FloatArray = TODO()

        override fun getVideoEffect(value: Float): Effect = TODO("White point adjustment is not available for video")
    },

    Warmth {
        override val title = R.string.editing_warmth
        override val icon = R.drawable.skillet

        override fun getMatrix(value: Float): FloatArray {
            // linear equation y = ax + b
            // shifts input by 0.65f
            val slider = (-value * 0.4375f + 0.65f)

            // taken from https://tannerhelland.com/2012/09/18/convert-temperature-rgb-algorithm-code.html and modified for brighter blues
            // values rounded because idk if the quality difference is enough to warrant casting 700 things to float and double
            val warmth = slider * 100f
            var red: Float
            var green: Float
            var blue: Float

            if (warmth <= 66f) {
                red = 255f
            } else {
                red = warmth - 60f
                red = 329.69873f * red.pow(-0.13320476f)
                red = red.coerceIn(0f, 255f)
            }

            if (warmth <= 66) {
                green = ln(warmth) * 99.4708f
                green -= 161.11957f
                green = green.coerceIn(0f, 255f)
            } else {
                green = warmth - 60f
                green = 288.12216f * green.pow(-0.075514846f)
                green = green.coerceIn(0f, 255f)
            }

            if (warmth <= 19f) {
                blue = 0f
            } else {
                blue = warmth
                blue = 138.51773f * ln(blue) - 305.0448f

                blue = max(blue, blue * (1.25f * slider))
                blue = blue.coerceAtLeast(0f)
            }

            red /= 255f
            green /= 255f
            blue /= 255f

            val floatArray = floatArrayOf(
                red, 0f, 0f, 0f,
                0f, green, 0f, 0f,
                0f, 0f, blue, 0f,
                0f, 0f, 0f, 1f
            )

            return floatArray
        }

        override fun getVideoEffect(value: Float): Effect = RgbMatrix { _, _ -> getMatrix(value) }
    },

    ColorTint {
        override val title = R.string.editing_color_tint
        override val icon = R.drawable.colors
        override val startValue = -1.2f
        override val availableOnVideos = false

        override fun getMatrix(value: Float): FloatArray = TODO()

        override fun getVideoEffect(value: Float): Effect = TODO("ColorTint adjustment is not available for video")
    },

    Highlights {
        override val title = R.string.editing_highlights
        override val icon = R.drawable.highlights

        override fun getMatrix(value: Float): FloatArray {
            val highlight = -value
            val floatArray = floatArrayOf(
                1 - highlight, 0f, 0f, 0f,
                0f, 1 - highlight, 0f, 0f,
                0f, 0f, 1 - highlight, 0f,
                0f, 0f, 0f, 1f
            )

            return floatArray
        }

        override fun getVideoEffect(value: Float): Effect = RgbMatrix { _, _ -> getMatrix(value) }
    }
}
