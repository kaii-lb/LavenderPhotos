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
import androidx.compose.runtime.Immutable
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
import kotlinx.serialization.Serializable

private const val TAG = "com.kaii.photos.helpers.editing.Paints"

/** @param shader is always null
 * @param colorFilter is always null */
@Suppress("SERIALIZER_TYPE_INCOMPATIBLE") // for nullable types below, they are always null, don't care about them
@Serializable
data class DrawingPaint(
    override var alpha: Float = 1.0f,
    @Serializable(with = BlendModeSerializer::class) override var blendMode: BlendMode = BlendMode.SrcOver,
    @Serializable(with = ColorSerializer::class) override var color: Color = DrawingColors.Black,
    @Serializable(with = AlwaysNullSerializer::class) override var colorFilter: ColorFilter? = null,
    @Serializable(with = FilterQualitySerializer::class) override var filterQuality: FilterQuality = FilterQuality.Low,
    override var isAntiAlias: Boolean = true,
    override var pathEffect: PathEffect? = null,
    @Serializable(with = AlwaysNullSerializer::class) override var shader: Shader? = null,
    @Serializable(with = StrokeCapSerializer::class) override var strokeCap: StrokeCap = StrokeCap.Round,
    @Serializable(with = StrokeJoinSerializer::class) override var strokeJoin: StrokeJoin = StrokeJoin.Round,
    override var strokeMiterLimit: Float = DefaultStrokeLineMiter,
    override var strokeWidth: Float = 20f,
    @Serializable(with = PaintingStyleSerializer::class) override var style: PaintingStyle = PaintingStyle.Stroke,
    var type: PaintType = PaintType.Pencil,
) : Paint {
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

@Immutable
enum class PaintType {
    Pencil,
    Highlighter,
    Text,
    Blur,
    Image
}

fun IntSize.toOffset(): Offset = Offset(this.width.toFloat(), this.height.toFloat())

@RequiresApi(Build.VERSION_CODES.S)
fun Bitmap.blur(blurRadius: Float): Bitmap {
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