package com.kaii.photos.helpers.editing

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.DefaultStrokeLineMiter
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.media3.common.Effect
import androidx.media3.common.util.UnstableApi
import com.kaii.photos.R
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow

class DrawingPaints {
    companion object {
        val Pencil =
            DrawingPaint().apply {
                type = PaintType.Pencil
                strokeWidth = 20f
                strokeCap = StrokeCap.Companion.Round
                strokeJoin = StrokeJoin.Companion.Round
                strokeMiterLimit = DefaultStrokeLineMiter
                pathEffect = PathEffect.Companion.cornerPathEffect(50f)
                blendMode = BlendMode.Companion.SrcOver
                color = DrawingColors.Red
                alpha = 1f
            }

        val Highlighter =
            DrawingPaint().apply {
                type = PaintType.Highlighter
                strokeWidth = 20f
                strokeCap = StrokeCap.Companion.Square
                strokeJoin = StrokeJoin.Companion.Miter
                strokeMiterLimit = DefaultStrokeLineMiter
                pathEffect = null
                blendMode = BlendMode.Companion.SrcOver
                color = DrawingColors.Red
                alpha = 0.5f
            }

        val Text =
            DrawingPaint().apply {
                type = PaintType.Text
                strokeWidth = 20f
                strokeCap = StrokeCap.Companion.Round
                strokeJoin = StrokeJoin.Companion.Round
                strokeMiterLimit = DefaultStrokeLineMiter
                pathEffect = PathEffect.Companion.cornerPathEffect(50f)
                blendMode = BlendMode.Companion.SrcOver
                color = DrawingColors.Red
                alpha = 1f
            }

        val Blur =
            DrawingPaint().apply {
                type = PaintType.Blur
                strokeWidth = 20f
                strokeCap = StrokeCap.Companion.Round
                strokeJoin = StrokeJoin.Companion.Round
                strokeMiterLimit = DefaultStrokeLineMiter
                pathEffect = PathEffect.Companion.cornerPathEffect(50f)
                blendMode = BlendMode.Companion.SrcOver
                color = DrawingColors.Black
                alpha = 1f
            }

        val Image =
            DrawingPaint().apply {
                type = PaintType.Image
                strokeWidth = 100f
                strokeCap = StrokeCap.Companion.Round
                strokeJoin = StrokeJoin.Companion.Round
                strokeMiterLimit = DefaultStrokeLineMiter
                pathEffect = PathEffect.Companion.cornerPathEffect(50f)
                blendMode = BlendMode.Companion.SrcOver
                color = Color.Transparent
                alpha = 1f
            }
    }
}

private interface ProcessingEffect {
    fun getMatrix(value: Float): FloatArray

    @androidx.annotation.OptIn(UnstableApi::class)
    fun getVideoEffect(value: Float): Effect

    @get:StringRes val title: Int
    @get:DrawableRes val icon: Int

    val startValue: Float
        get() = 0f
}

@androidx.annotation.OptIn(UnstableApi::class)
enum class MediaAdjustments : ProcessingEffect {
    Contrast {
        override val title = R.string.editing_contrast
        override val icon = R.drawable.contrast

        override fun getMatrix(value: Float): FloatArray {
            val contrast = (1 + value) / (1.0001f - value)
            val offset = 0.5f * (1f - contrast) * 255f

            val floatArray = floatArrayOf(
                contrast, 0f, 0f, 0f, offset,
                0f, contrast, 0f, 0f, offset,
                0f, 0f, contrast, 0f, offset,
                0f, 0f, 0f, 1f, 0f
            )

            return floatArray
        }

        override fun getVideoEffect(value: Float): Effect =
            ColorMatrixEffect(
                matrix = ColorMatrix(getMatrix(value)),
                isFilter = false
            )
    },

    Brightness {
        override val title = R.string.editing_brightness
        override val icon = R.drawable.brightness

        override fun getMatrix(value: Float): FloatArray {
            val offset = value * 127f
            return floatArrayOf(
                1f, 0f, 0f, 0f, offset,
                0f, 1f, 0f, 0f, offset,
                0f, 0f, 1f, 0f, offset,
                0f, 0f, 0f, 1f, 0f
            )
        }

        override fun getVideoEffect(value: Float): Effect =
            ColorMatrixEffect(
                matrix = ColorMatrix(getMatrix(value)),
                isFilter = false
            )
    },

    Saturation {
        override val title = R.string.editing_saturation
        override val icon = R.drawable.saturation

        override fun getMatrix(value: Float): FloatArray {
            return ColorMatrix().let {
                it.setToSaturation(value + 1f)
                it.values
            }
        }

        override fun getVideoEffect(value: Float): Effect =
            ColorMatrixEffect(
                matrix = ColorMatrix(getMatrix(value)),
                isFilter = false
            )
    },

    BlackPoint {
        override val title = R.string.editing_black_point
        override val icon = R.drawable.file_is_selected_background

        override fun getMatrix(value: Float): FloatArray {
            val blackPoint = 150f * value
            return floatArrayOf(
                1f, 0f, 0f, 0f, blackPoint,
                0f, 1f, 0f, 0f, blackPoint,
                0f, 0f, 1f, 0f, blackPoint,
                0f, 0f, 0f, 1f, 0f
            )
        }

        override fun getVideoEffect(value: Float): Effect =
            ColorMatrixEffect(
                matrix = ColorMatrix(getMatrix(value)),
                isFilter = false
            )
    },

    WhitePoint {
        override val title = R.string.editing_white_point
        override val icon = R.drawable.file_not_selected_background

        override fun getMatrix(value: Float): FloatArray {
            val whitePoint = -value + 1f
            return floatArrayOf(
                whitePoint, 0f, 0f, 0f, 0f,
                0f, whitePoint, 0f, 0f, 0f,
                0f, 0f, whitePoint, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        }

        override fun getVideoEffect(value: Float): Effect =
            ColorMatrixEffect(
                matrix = ColorMatrix(getMatrix(value)),
                isFilter = false
            )
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
                red, 0f, 0f, 0f, 0f,
                0f, green, 0f, 0f, 0f,
                0f, 0f, blue, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )

            return floatArray
        }

        override fun getVideoEffect(value: Float): Effect =
            ColorMatrixEffect(
                matrix = ColorMatrix(getMatrix(value)),
                isFilter = false
            )
    },

    ColorTint {
        override val title = R.string.editing_color_tint
        override val icon = R.drawable.colors
        override val startValue = -1.2f

        override fun getMatrix(value: Float): FloatArray {
            if (value == -1.2f) {
                return ColorMatrix().values
            }

            val tint =
                (value * 0.5f + 0.5f).coerceIn(0f, gradientColorList.size - 1f)

            val resolvedColor = getColorFromLinearGradientList(tint, gradientColorList)

            return floatArrayOf(
                0.6f, 0.2f, 0.2f, 0f, 0.2f * resolvedColor.red * 255f,
                0.2f, 0.6f, 0.2f, 0f, 0.2f * resolvedColor.green * 255f,
                0.2f, 0.2f, 0.6f, 0f, 0.2f * resolvedColor.blue * 255f,
                0f, 0f, 0f, 1f, 0f
            )
        }

        override fun getVideoEffect(value: Float): Effect =
            ColorMatrixEffect(
                matrix = ColorMatrix(getMatrix(value)),
                isFilter = false
            )
    },

    Highlights {
        override val title = R.string.editing_highlights
        override val icon = R.drawable.highlights

        override fun getMatrix(value: Float): FloatArray {
            val highlight = -value
            val floatArray = floatArrayOf(
                1 - highlight, 0f, 0f, 0f, 0f,
                0f, 1 - highlight, 0f, 0f, 0f,
                0f, 0f, 1 - highlight, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )

            return floatArray
        }

        override fun getVideoEffect(value: Float): Effect =
            ColorMatrixEffect(
                matrix = ColorMatrix(getMatrix(value)),
                isFilter = false
            )
    }
}

fun ImageBitmap.withColorFilter(filter: ColorMatrix): ImageBitmap {
    val holder = ImageBitmap(this.width, this.height)
    val canvas = Canvas(holder)

    canvas.drawImage(
        image = this,
        topLeftOffset = Offset(0f, 0f),
        paint = Paint().apply {
            colorFilter = ColorFilter.colorMatrix(filter)
        }
    )

    return holder
}

interface Modification

data class DrawablePath(
    val path: Path,
    val paint: DrawingPaint
) : Modification

data class DrawableBlur(
    val path: Path,
    val paint: DrawingPaint
) : Modification

data class DrawableImage(
    val bitmapUri: Uri,
    var rotation: Float,
    val paint: DrawingPaint
) : Modification

data class DrawableText(
    var text: String,
    var position: Offset,
    val paint: DrawingPaint,
    var rotation: Float,
    var size: IntSize
) : Modification {
    @JvmInline
    value class Styles(val style: TextStyle) {
        companion object {
            val Default =
                TextStyle(
                    textAlign = TextAlign.Center,
                    platformStyle = PlatformTextStyle(
                        includeFontPadding = false
                    ),
                    lineHeightStyle = LineHeightStyle(
                        trim = LineHeightStyle.Trim.Both,
                        alignment = LineHeightStyle.Alignment.Center
                    ),
                    baselineShift = BaselineShift.None,
                )
        }
    }
}

enum class CroppingAspectRatio(
    var ratio: Float,
    @param:StringRes val title: Int
) {
    FreeForm(0f, R.string.bottom_sheets_freeform),
    ByImage(-1f, R.string.bottom_sheets_image_ratio),
    Square(1f, R.string.bottom_sheets_square),
    SixteenByNine(16f / 9f, R.string.bottom_sheets_sixteen_by_nine),
    NineBySixteen(9f / 16f, R.string.bottom_sheets_nine_by_sixteen),
    NineByTwentyOne(9f / 21f, R.string.bottom_sheets_nine_by_twentyone),
    FiveByFour(5f / 4f, R.string.bottom_sheets_five_by_four),
    FourByThree(4f / 3f, R.string.bottom_sheets_four_by_three),
    ThreeByTwo(3f / 2f, R.string.bottom_sheets_three_by_two)
}

enum class SelectedCropArea {
    TopLeftCorner,
    TopRightCorner,
    BottomLeftCorner,
    BottomRightCorner,
    TopEdge,
    LeftEdge,
    BottomEdge,
    RightEdge,
    None,
    Whole
}

interface SharedModification {
    interface DrawingText : SharedModification {
        val type: DrawingItems
        val text: DrawableText
    }

    interface DrawingPath : SharedModification {
        val type: DrawingItems
        val path: DrawablePath
    }

    interface DrawingImage : SharedModification {
        val type: DrawingItems
        val image: DrawableImage
    }
}

fun DrawableText.checkTapInText(
    tapPosition: Offset,
    padding: Float
) =
    tapPosition.x in position.x - padding / 2..position.x + padding / 2 + size.width // not subtracting on left or top since position is from top left
            && tapPosition.y in position.y - padding..position.y + padding + size.height
