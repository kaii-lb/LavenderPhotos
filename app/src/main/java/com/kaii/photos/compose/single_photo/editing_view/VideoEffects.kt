package com.kaii.photos.compose.single_photo.editing_view

import android.content.Context
import android.opengl.GLES20
import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.ColorMatrix
import androidx.media3.common.Effect
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.GlUtil.GlException
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BaseGlShaderProgram
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.RgbMatrix
import com.kaii.photos.R
import com.kaii.photos.helpers.MediaColorFilters
import com.kaii.photos.helpers.getColorFromLinearGradientList
import com.kaii.photos.helpers.gradientColorList
import org.intellij.lang.annotations.Language
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

    data class Filter(
        val type: MediaColorFilters
    ) : VideoModification
}

private interface ProcessingEffect {
    fun getMatrix(value: Float): FloatArray

    @OptIn(UnstableApi::class)
    fun getVideoEffect(value: Float): Effect

    @get:StringRes val title: Int
    @get:DrawableRes val icon: Int

    val startValue: Float
        get() = 0f
}

@OptIn(UnstableApi::class)
enum class MediaAdjustments : ProcessingEffect {
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
                isFilter = false,
                type = this
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
                isFilter = false,
                type = this
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
                isFilter = false,
                type = this
            )
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

@OptIn(UnstableApi::class)
class ColorMatrixGLShaderProgram(
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

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        return Size(inputWidth, inputHeight)
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
    val isFilter: Boolean,
    val type: MediaAdjustments? = null
) : GlEffect {
    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram = ColorMatrixGLShaderProgram(matrix = matrix)

    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean {
        return super.isNoOp(inputWidth, inputHeight)
    }
}
