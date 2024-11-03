package com.kaii.photos.helpers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.NativePaint
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.DefaultStrokeLineMiter

class ExtendedPaint(
    override var alpha: Float = 1.0f,
    override var blendMode: BlendMode = BlendMode.SrcOver,
    override var color: Color = DrawingColors.Black,
    override var colorFilter: ColorFilter? = null,
    override var filterQuality: FilterQuality = FilterQuality.Low,
    override var isAntiAlias: Boolean = true,
    override var pathEffect: PathEffect? = null,
    override var shader: Shader? = null,
    override var strokeCap: StrokeCap = StrokeCap.Round,
    override var strokeJoin: StrokeJoin = StrokeJoin.Round,
    override var strokeMiterLimit: Float = DefaultStrokeLineMiter,
    override var strokeWidth: Float = 20f,
    override var style: PaintingStyle = PaintingStyle.Stroke,
    var type: PaintType = PaintType.Pencil,
) : Paint {
    fun copy(
        color: Color = this.color,
        strokeCap: StrokeCap = this.strokeCap,
        strokeWidth: Float = this.strokeWidth,
        strokeJoin: StrokeJoin = this.strokeJoin,
        style: PaintingStyle = this.style,
        blendMode: BlendMode = this.blendMode,
        alpha: Float = this.alpha,
        pathEffect: PathEffect? = this.pathEffect,
        label: PaintType = this.type,
        filterQuality: FilterQuality = this.filterQuality,
        isAntiAlias: Boolean = this.isAntiAlias,
        strokeMiterLimit: Float = this.strokeMiterLimit,
        shader: Shader? = this.shader,
        colorFilter: ColorFilter? = this.colorFilter
    ) = ExtendedPaint().also { paint ->
        paint.color = color
        paint.strokeCap = strokeCap
        paint.strokeWidth = strokeWidth
        paint.strokeJoin = strokeJoin
        paint.style = style
        paint.blendMode = blendMode
        paint.alpha = alpha
        paint.pathEffect = pathEffect
        paint.filterQuality = filterQuality
        paint.isAntiAlias = isAntiAlias
        paint.strokeMiterLimit = strokeMiterLimit
        paint.shader = shader
        paint.colorFilter = colorFilter
        paint.type = label
    }

    override fun asFrameworkPaint(): NativePaint {
        return Paint().also { paint ->
            paint.color = color
            paint.strokeCap = strokeCap
            paint.strokeWidth = strokeWidth
            paint.strokeJoin = strokeJoin
            paint.style = style
            paint.blendMode = blendMode
            paint.alpha = alpha
            paint.pathEffect = pathEffect
            paint.filterQuality = filterQuality
            paint.isAntiAlias = isAntiAlias
            paint.strokeMiterLimit = strokeMiterLimit
            paint.shader = shader
            paint.colorFilter = colorFilter
        }.asFrameworkPaint()
    }
}

class DrawingPaints {
    companion object {
        val Pencil =
            ExtendedPaint().apply {
                type = PaintType.Pencil
                strokeWidth = 20f
                strokeCap = StrokeCap.Round
                strokeJoin = StrokeJoin.Round
                strokeMiterLimit = DefaultStrokeLineMiter
                pathEffect = PathEffect.cornerPathEffect(50f)
                blendMode = BlendMode.SrcOver
                color = DrawingColors.Red
                alpha = 1f
            }

        val Highlighter =
            ExtendedPaint().apply {
                type = PaintType.Highlighter
                strokeWidth = 20f
                strokeCap = StrokeCap.Square
                strokeJoin = StrokeJoin.Miter
                strokeMiterLimit = DefaultStrokeLineMiter
                pathEffect = null
                blendMode = BlendMode.SrcOver
                color = DrawingColors.Red
                alpha = 0.5f
            }

        val Text =
            ExtendedPaint().apply {
                type = PaintType.Text
                strokeWidth = 20f
                strokeCap = StrokeCap.Round
                strokeJoin = StrokeJoin.Round
                strokeMiterLimit = DefaultStrokeLineMiter
                pathEffect = PathEffect.cornerPathEffect(50f)
                blendMode = BlendMode.SrcOver
                color = DrawingColors.Red
                alpha = 1f
            }
    }
}

abstract class DrawableItem (
    val type: ModificationType
)

data class PathWithPaint(
    val path: Path,
    val paint: ExtendedPaint
) : DrawableItem(ModificationType.Paint)

enum class PaintType {
    Pencil,
    Highlighter,
    Text
}

enum class ModificationType {
    Paint,
    Text,
}

data class DrawableText(
    val text: String,
    var position: Offset,
    val paint: ExtendedPaint,
    var rotation: Float
) : DrawableItem(ModificationType.Text)
