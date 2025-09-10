package com.kaii.photos.helpers.editing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.opengl.GLES20
import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.OverlaySettings
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.GlUtil.GlException
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BaseGlShaderProgram
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.CanvasOverlay
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.exoplayer.ExoPlayer
import com.kaii.photos.R
import org.intellij.lang.annotations.Language

private interface DrawingEffect {
    @get:StringRes val title: Int
    @get:StringRes val icon: Int

    @OptIn(UnstableApi::class)
    fun toEffect(
        value: Any,
        timespan: VideoModification.Trim?,
        ratio: Float,
        resolution: Size,
        context: Context,
        textMeasurer: TextMeasurer
    ): BitmapOverlay

    fun toDrawingPaint(): DrawingPaint
}

@OptIn(UnstableApi::class)
enum class DrawingItems : DrawingEffect {
    Pencil {
        override val title = R.string.editing_pencil
        override val icon = R.drawable.pencil

        override fun toEffect(
            value: Any,
            timespan: VideoModification.Trim?,
            ratio: Float,
            resolution: Size,
            context: Context,
            textMeasurer: TextMeasurer
        ): BitmapOverlay {
            if (value !is DrawablePath) throw IllegalArgumentException("${Pencil::class}.toEffect can only accept ${DrawablePath::class} as an input value")

            return TimePathOverlay(
                path = value,
                timespan = timespan,
                ratio = ratio,
                resolution = resolution
            )
        }

        override fun toDrawingPaint(): DrawingPaint = DrawingPaints.Pencil
    },

    Highlighter {
        override val title = R.string.editing_highlighter
        override val icon = R.drawable.highlighter

        override fun toEffect(
            value: Any,
            timespan: VideoModification.Trim?,
            ratio: Float,
            resolution: Size,
            context: Context,
            textMeasurer: TextMeasurer
        ): BitmapOverlay {
            if (value !is DrawablePath) throw IllegalArgumentException("${Highlighter::class}.toEffect can only accept ${DrawablePath::class} as an input value")

            return TimePathOverlay(
                path = value,
                timespan = timespan,
                ratio = ratio,
                resolution = resolution
            )
        }

        override fun toDrawingPaint(): DrawingPaint = DrawingPaints.Highlighter
    },

    Text {
        override val title = R.string.editing_text
        override val icon = R.drawable.text

        override fun toEffect(
            value: Any,
            timespan: VideoModification.Trim?,
            ratio: Float,
            resolution: Size,
            context: Context,
            textMeasurer: TextMeasurer
        ): BitmapOverlay {
            if (value !is VideoModification.DrawingText) throw IllegalArgumentException("${Text::class}.toEffect can only accept ${VideoModification.DrawingText::class} as an input value")

            return TimedTextOverlay(
                drawingText = value,
                resolution = resolution,
                textMeasurer = textMeasurer,
                timespan = timespan,
                ratio = ratio
            )
        }

        override fun toDrawingPaint(): DrawingPaint = DrawingPaints.Text
    },

    Image {
        override val title = R.string.editing_image
        override val icon = R.drawable.photogrid

        override fun toEffect(
            value: Any,
            timespan: VideoModification.Trim?,
            ratio: Float,
            resolution: Size,
            context: Context,
            textMeasurer: TextMeasurer
        ): BitmapOverlay {
            if (value !is DrawableImage) throw IllegalArgumentException("${Image::class}.toEffect can only accept ${DrawableImage::class} as an input value")

            val bitmap = context.contentResolver.openInputStream(value.bitmapUri).use {
                BitmapFactory.decodeStream(it)
            }

            return TimedBitmapOverlay(
                bitmap = bitmap,
                timespan = timespan,
                overlaySettings =
                    StaticOverlaySettings.Builder()
                        .setRotationDegrees(value.rotation)
                        .build()
            )
        }

        override fun toDrawingPaint(): DrawingPaint = DrawingPaints.Image
    }
}

@OptIn(UnstableApi::class)
class TimePathOverlay(
    val path: DrawablePath,
    private val timespan: VideoModification.Trim? = null,
    private val ratio: Float,
    private val resolution: Size
) : CanvasOverlay(true) {
    override fun onDraw(canvas: Canvas, presentationTimeUs: Long) {
        val drawScope = CanvasDrawScope()
        val composeCanvas = androidx.compose.ui.graphics.Canvas(canvas)

        if (timespan == null || presentationTimeUs / 1000f in (timespan.start * 1000)..(timespan.end * 1000)) {
            val scaledDrawingWidth = resolution.width * ratio
            val scaledDrawingHeight = resolution.height * ratio
            val translateX = (canvas.width - scaledDrawingWidth) / 2f
            val translateY = (canvas.height - scaledDrawingHeight) / 2f

            // optimize to not draw again since canvas doesn't change always
            drawScope.draw(
                density = Density(1f),
                layoutDirection = LayoutDirection.Ltr,
                canvas = composeCanvas,
                size = Size(
                    canvas.width.toFloat(),
                    canvas.height.toFloat()
                )
            ) {
                translate(left = translateX, top = translateY) {
                    scale(ratio, Offset(1f, 1f)) {
                        drawPath(
                            path = path.path,
                            style = Stroke(
                                width = path.paint.strokeWidth,
                                cap = path.paint.strokeCap,
                                join = path.paint.strokeJoin,
                                miter = path.paint.strokeMiterLimit,
                                pathEffect = path.paint.pathEffect
                            ),
                            blendMode = path.paint.blendMode,
                            color = path.paint.color,
                            alpha = path.paint.alpha
                        )
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
class TimedTextOverlay(
    private val drawingText: VideoModification.DrawingText,
    private val resolution: Size,
    private val ratio: Float,
    private val textMeasurer: TextMeasurer,
    private val timespan: VideoModification.Trim? = null
) : CanvasOverlay(true) {
    override fun onDraw(canvas: Canvas, presentationTimeUs: Long) {
        val drawScope = CanvasDrawScope()
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR) // clear canvas from previous state
        val composeCanvas = androidx.compose.ui.graphics.Canvas(canvas)

        if (timespan == null || presentationTimeUs / 1000f in (timespan.start * 1000)..(timespan.end * 1000)) {
            var text = drawingText.text
            val scaledDrawingWidth = resolution.width * ratio
            val scaledDrawingHeight = resolution.height * ratio
            val translateX = (canvas.width - scaledDrawingWidth) / 2f
            val translateY = (canvas.height - scaledDrawingHeight) / 2f

            val keyframes = drawingText.keyframes

            if (keyframes != null) {
                val keyframe = interpolateKeyframes(
                    keyframes = keyframes,
                    timeMs = presentationTimeUs / 1000f
                )

                if (keyframe != null) {
                    text = text.copy(
                        position = keyframe.position,
                        paint = text.paint.copy(
                            color = keyframe.color,
                            strokeWidth = keyframe.strokeWidth
                        ),
                        rotation = keyframe.rotation
                    )
                }
            }

            // optimize to not draw again since canvas doesn't change always
            drawScope.draw(
                density = Density(1f),
                layoutDirection = LayoutDirection.Ltr,
                canvas = composeCanvas,
                size = Size(
                    canvas.width.toFloat(),
                    canvas.height.toFloat()
                )
            ) {
                translate(left = translateX, top = translateY) {
                    scale(ratio, Offset(1f, 1f)) {
                        rotate(text.rotation, text.position + text.size.toOffset() / 2f) {
                            val textLayout = textMeasurer.measure(
                                text = text.text,
                                style = DrawableText.Styles.Default.copy(
                                    color = text.paint.color,
                                    fontSize = text.paint.strokeWidth.sp
                                ),
                                softWrap = false
                            )

                            drawText(
                                textLayoutResult = textLayout,
                                topLeft = text.position,
                                blendMode = text.paint.blendMode
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
class TimedBitmapOverlay(
    private val bitmap: Bitmap,
    private val overlaySettings: StaticOverlaySettings,
    private val timespan: VideoModification.Trim? = null
) : BitmapOverlay() {
    private val emptyBitmap = createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings = overlaySettings

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        return if (timespan == null || presentationTimeUs / 1000f in (timespan.start * 1000)..(timespan.end * 1000)) bitmap
        else emptyBitmap
    }
}

@OptIn(UnstableApi::class)
private class ColorMatrixGLShaderProgram(
    matrix: ColorMatrix
) : BaseGlShaderProgram(false, 1) {
    @Language("GLSL")
    val vertexShader = """
        #version 100
        
        attribute vec4 aFramePosition;
        varying vec2 vTexSamplingCoord;
        
        void main() {
          gl_Position = aFramePosition;
          vTexSamplingCoord = vec2(aFramePosition.x * 0.5 + 0.5, aFramePosition.y * 0.5 + 0.5);
        }
    """.trimIndent()

    @Language("GLSL")
    val fragmentShader = """
        #version 100
        
        precision mediump float;
        uniform sampler2D uTexSampler;
        varying vec2 vTexSamplingCoord;
        
        mat4 matrix = mat4(
            ${matrix[0, 0]}, ${matrix[0, 1]}, ${matrix[0, 2]}, ${matrix[0, 3]},
            ${matrix[1, 0]}, ${matrix[1, 1]}, ${matrix[1, 2]}, ${matrix[1, 3]},
            ${matrix[2, 0]}, ${matrix[2, 1]}, ${matrix[2, 2]}, ${matrix[2, 3]},
            ${matrix[3, 0]}, ${matrix[3, 1]}, ${matrix[3, 2]}, ${matrix[3, 3]}
        );
        
        vec4 offsets = vec4(
            ${matrix[0, 4]} / 255.0,
            ${matrix[1, 4]} / 255.0,
            ${matrix[2, 4]} / 255.0,
            ${matrix[3, 4]} / 255.0
        );
        
        void main() {
            vec4 src = texture2D(uTexSampler, vTexSamplingCoord);
            
            vec4 outputColor = src * matrix + offsets;
            
            gl_FragColor = outputColor;
        }
    """.trimIndent()

    val glProgram = GlProgram(vertexShader, fragmentShader)

    init {
        glProgram.setBufferAttribute(
            "aFramePosition",
            GlUtil.getNormalizedCoordinateBounds(),
            GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE
        )
        // glProgram.setBufferAttribute("aRedMultiplier", floatArrayOf(0.5f), 1)
        GlUtil.checkGlError()
    }

    override fun configure(inputWidth: Int, inputHeight: Int): androidx.media3.common.util.Size {
        return androidx.media3.common.util.Size(inputWidth, inputHeight)
    }

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        try {
            checkNotNull(glProgram)

            glProgram.setSamplerTexIdUniform("uTexSampler", inputTexId, 0)
            glProgram.bindAttributesAndUniforms()

            GlUtil.checkGlError()

            glProgram.use()

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        } catch (e: GlException) {
            throw VideoFrameProcessingException(e, presentationTimeUs)
        }
    }

    override fun release() {
        super.release()

        try {
            glProgram.delete()
        } catch (e: GlException) {
            throw VideoFrameProcessingException(e)
        }
    }
}

@UnstableApi
class ColorMatrixEffect(
    private val matrix: ColorMatrix,
    val isFilter: Boolean
) : GlEffect {
    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram = ColorMatrixGLShaderProgram(matrix = matrix)

    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean {
        return matrix == ColorMatrix()
    }
}

@OptIn(UnstableApi::class)
fun ExoPlayer.applyEffects(
    uri: Uri,
    effectList: List<Effect>
) {
    stop()
    val mediaItem = MediaItem.Builder()
        .setUri(uri)
        .build()

    setMediaItem(mediaItem)
    setVideoEffects(effectList)
    prepare()
}

data class BasicVideoData(
    val duration: Float,
    val frameRate: Float,
    val absolutePath: String,
    val width: Int,
    val height: Int
) {
    val aspectRatio = if (height == 0) -1f else width.toFloat() / height
}

enum class VideoEditorTabs(
    @param:StringRes val title: Int
) {
    Crop(R.string.editing_crop),
    Trim(R.string.editing_trim),
    Video(R.string.video),
    Draw(R.string.editing_draw),
    Adjust(R.string.editing_adjust),
    Filters(R.string.editing_filters)
}