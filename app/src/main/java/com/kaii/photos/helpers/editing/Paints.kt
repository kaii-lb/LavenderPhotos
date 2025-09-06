package com.kaii.photos.helpers.editing

import android.graphics.Bitmap
import android.graphics.HardwareRenderer
import android.graphics.PixelFormat
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.NativePaint
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.DefaultStrokeLineMiter
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.createBitmap

private const val TAG = "PAINTS"

class DrawingPaint(
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
        type: PaintType = this.type,
        filterQuality: FilterQuality = this.filterQuality,
        isAntiAlias: Boolean = this.isAntiAlias,
        strokeMiterLimit: Float = this.strokeMiterLimit,
        shader: Shader? = this.shader,
        colorFilter: ColorFilter? = this.colorFilter
    ) = DrawingPaint().also { paint ->
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
        paint.type = type
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

enum class PaintType {
    Pencil,
    Highlighter,
    Text,
    Blur
}

fun IntSize.toOffset() : Offset = Offset(this.width.toFloat(), this.height.toFloat())

@RequiresApi(Build.VERSION_CODES.S)
fun Bitmap.blur(blurRadius: Float) : Bitmap {
    val imageReader = ImageReader.newInstance(
        width, height,
        PixelFormat.RGBA_8888, 1,
        HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
    )

    val renderNode = RenderNode("BlurEffect")
    val hardwareRenderer = HardwareRenderer()

    hardwareRenderer.setSurface(imageReader.surface)
    hardwareRenderer.setContentRoot(renderNode)
    renderNode.setPosition(0, 0, imageReader.width, imageReader.height)

    val blurRenderEffect = RenderEffect.createBlurEffect(
        blurRadius, blurRadius,
        android.graphics.Shader.TileMode.MIRROR
    )

    renderNode.setRenderEffect(blurRenderEffect)

    val renderCanvas = renderNode.beginRecording()
    renderCanvas.drawBitmap(this, 0f, 0f, null)
    renderNode.endRecording()
    hardwareRenderer.createRenderRequest()
        .setWaitForPresent(true)
        .syncAndDraw()

    val emptyBitmap = createBitmap(512, 512)

    val image = imageReader.acquireNextImage() ?: run {
    	Log.e(TAG, "image reader was empty")
    	return emptyBitmap
   	}
    val hardwareBuffer = image.hardwareBuffer ?: run {
    	Log.e(TAG, "hardware buffer was empty")
    	return emptyBitmap
   	}
    val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, null) ?: run {
    	Log.e(TAG, "decoded bitmap was empty")
    	emptyBitmap
	}

    image.close()
    hardwareBuffer.close()
    hardwareRenderer.destroy()
    imageReader.close()
    renderNode.discardDisplayList()

    return bitmap.copy(Bitmap.Config.ARGB_8888, false)
}