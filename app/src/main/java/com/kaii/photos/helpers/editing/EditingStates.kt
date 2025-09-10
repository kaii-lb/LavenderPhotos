package com.kaii.photos.helpers.editing

import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.sp
import androidx.media3.common.Effect
import androidx.media3.common.util.UnstableApi
import com.kaii.photos.helpers.editing.DrawingPaintState.Companion.Saver
import com.kaii.photos.helpers.editing.VideoEditingState.Companion.Saver

class DrawingPaintState(
    initialPaintType: DrawingItems,
    initialStrokeWidth: Float,
    initialColor: Color
) {
    var paintType by mutableStateOf(initialPaintType)
        private set
    var strokeWidth by mutableFloatStateOf(initialStrokeWidth)
        private set
    var color by mutableStateOf(initialColor)
        private set
    var recordKeyframes by mutableStateOf(false)
        private set
    var selectedText: SharedModification.DrawingText? by mutableStateOf(null)
        private set

    val modifications = mutableStateListOf<SharedModification>()

    val paint by derivedStateOf {
        paintType.toDrawingPaint().copy(
            color = color,
            strokeWidth = strokeWidth
        )
    }

    @JvmName("privateSetPaintType")
    fun setPaintType(paintType: DrawingItems) {
        this.paintType = paintType
    }

    @JvmName("privateSetStrokeWidth")
    fun setStrokeWidth(strokeWidth: Float, textMeasurer: TextMeasurer) {
        if (this.selectedText != null) {
            if (this.selectedText is VideoModification.DrawingText) {
                val drawingText = this.selectedText as VideoModification.DrawingText

                modifications.removeAll {
                    it == drawingText
                }

                val newSize = textMeasurer.measure(
                    text = drawingText.text.text,
                    style = DrawableText.Styles.Default.copy(
                        color = drawingText.text.paint.color,
                        fontSize = drawingText.text.paint.strokeWidth.sp
                    )
                ).size

                val new =
                    drawingText.copy(
                        text = drawingText.text.copy(
                            paint = drawingText.text.paint.copy(
                                strokeWidth = strokeWidth
                            ),
                            size = newSize,
                            position = drawingText.text.position - (newSize.toOffset() - drawingText.text.size.toOffset()) / 2f
                        )
                    )

                modifications.add(new)
                this.selectedText = new
            } // TODO: for images
        }

        this.strokeWidth = strokeWidth
    }

    @JvmName("privateSetColor")
    fun setColor(color: Color) {
        if (this.selectedText != null) {
            if (this.selectedText is VideoModification.DrawingText) {
                val drawingText = this.selectedText as VideoModification.DrawingText

                modifications.removeAll {
                    it == drawingText
                }

                val new =
                    drawingText.copy(
                        text = drawingText.text.copy(
                            paint = drawingText.text.paint.copy(
                                color = color
                            )
                        )
                    )

                modifications.add(new)
                this.selectedText = new
            } // TODO: for images
        }

        this.color = color
    }

    @JvmName("privateSetRecordKeyframes")
    fun setRecordKeyframes(record: Boolean) {
        this.recordKeyframes = record
    }

    @JvmName("privateSetSelectedText")
    fun setSelectedText(text: SharedModification.DrawingText?) {
        this.selectedText = text
    }

    fun undoModification() {
        modifications.removeLastOrNull()
    }

    fun clearModifications() {
        modifications.clear()
        this.selectedText = null
    }

    companion object {
        /** The default [Saver] implementation for [DrawingPaintState]. */
        val Saver: Saver<DrawingPaintState, *> =
            listSaver(
                save = { listOf(it.paintType, it.strokeWidth, it.color.toArgb()) },
                restore = {
                    DrawingPaintState(
                        initialPaintType = it[0] as DrawingItems,
                        initialStrokeWidth = it[1] as Float,
                        initialColor = Color(it[2] as Int)
                    )
                },
            )
    }
}

@Composable
fun rememberDrawingPaintState(
    initialPaintType: DrawingItems = DrawingItems.Pencil,
    initialStrokeWidth: Float = 20f,
    initialColor: Color = DrawingColors.Red
): DrawingPaintState {
    return rememberSaveable(saver = DrawingPaintState.Saver) {
        DrawingPaintState(
            initialPaintType = initialPaintType,
            initialStrokeWidth = initialStrokeWidth,
            initialColor = initialColor
        )
    }
}

@OptIn(UnstableApi::class)
class VideoEditingState(
    initialEffects: List<Effect>?,
    initialCroppingAspectRatio: CroppingAspectRatio,
    initialRotation: Float,
    initialSpeed: Float,
    initialFrameRate: Float,
    initialVolume: Float,
    val duration: Float,
) {
    var croppingAspectRatio by mutableStateOf(initialCroppingAspectRatio)
        private set
    var rotation by mutableFloatStateOf(initialRotation)
        private set
    var startTrimPosition by mutableFloatStateOf(0f)
        private set
    var endTrimPosition by mutableFloatStateOf(duration)
        private set
    var resetCrop by mutableStateOf(false)
        private set
    var speed by mutableFloatStateOf(initialSpeed)
        private set
    var frameRate by mutableFloatStateOf(initialFrameRate)
        private set
    var volume by mutableFloatStateOf(initialVolume)
        private set

    private val effects = mutableStateListOf<Effect?>()
    val effectList by derivedStateOf { effects.toList().mapNotNull { it } }

    init {
        if (initialEffects == null) {
            // prefill for order of application
            (1..MediaAdjustments.entries.size).forEach { _ ->
                effects.add(null)
            }
        } else {
            effects.clear()
            effects.addAll(initialEffects)

            // prefill for order of application
            (effects.size..12).forEach { _ ->
                effects.add(null)
            }
        }
    }

    @JvmName("privateSetCroppingAspectRatio")
    fun setCroppingAspectRatio(ratio: CroppingAspectRatio) {
        this.croppingAspectRatio = ratio
    }

    @JvmName("privateSetRotation")
    fun setRotation(angle: Float) {
        this.rotation = angle
    }

    @JvmName("privateSetStartTrimPosition")
    fun setStartTrimPosition(position: Float) {
        Log.d(TAG, "Duration is $duration start $startTrimPosition wanted $position")
        this.startTrimPosition = position.coerceIn(0f, endTrimPosition - (duration * 0.1f))
    }

    @JvmName("privateSetEndTrimPosition")
    fun setEndTrimPosition(position: Float) {
        this.endTrimPosition = position.coerceIn(startTrimPosition + (duration * 0.1f), duration)
    }

    @JvmName("privateSetResetCrop")
    fun setResetCrop(value: Boolean) {
        this.resetCrop = value
    }

    @JvmName("privateSetSpeed")
    fun setSpeed(speed: Float) {
        this.speed = speed
    }

    @JvmName("privateSetFrameRate")
    fun setFrameRate(fps: Float) {
        this.frameRate = fps
    }

    @JvmName("privateSetVolume")
    fun setVolume(volume: Float) {
        this.volume = volume
    }

    fun addEffect(effect: Effect, effectIndex: Int? = null) {
        if (effectIndex != null) {
            // cuz order of application matters, so just do this and have it be constant
            effects.add(effectIndex, effect)
            effects.removeAt(effectIndex + 1) // remove null after shirting right
        } else {
            effects.removeAll {
                if (it == null) false else it::class == effect::class
            }
            effects.add(effect)
        }

        Log.d(TAG, "Effect list $effects")
    }

    fun removeAllEffects(predicate: (Effect?) -> Boolean) = effects.removeAll { predicate(it) }

    companion object {
        private const val TAG = "com.kaii.photos.helpers.editing.VideoEditingState"

        /** The default [Saver] implementation for [VideoEditingState]. */
        val Saver: Saver<VideoEditingState, *> =
            listSaver(
                save = { listOf(it.croppingAspectRatio, it.rotation, it.effects, it.speed, it.volume, it.frameRate, it.duration) },
                restore = {
                    VideoEditingState(
                        initialCroppingAspectRatio = it[0] as CroppingAspectRatio,
                        initialRotation = it[1] as Float,
                        initialEffects = (it[2] as? List<*>)?.filterIsInstance<Effect>() ?: emptyList(),
                        initialSpeed = it[3] as Float,
                        initialVolume = it[4] as Float,
                        initialFrameRate = it[5] as Float,
                        duration = it[6] as Float
                    )
                },
            )
    }
}

@OptIn(UnstableApi::class)
@Composable
fun rememberVideoEditingState(
    duration: Float,
    initialCroppingAspectRatio: CroppingAspectRatio = CroppingAspectRatio.FreeForm,
    initialRotation: Float = 0f,
    initialEffects: List<Effect>? = null,
): VideoEditingState {
    return rememberSaveable(duration, saver = VideoEditingState.Saver) {
        VideoEditingState(
            duration = duration,
            initialCroppingAspectRatio = initialCroppingAspectRatio,
            initialRotation = initialRotation,
            initialEffects = initialEffects,
            initialVolume = 1f,
            initialSpeed = 1f,
            initialFrameRate = 0f
        )
    }
}