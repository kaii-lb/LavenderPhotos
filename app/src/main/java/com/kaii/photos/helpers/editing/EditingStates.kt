package com.kaii.photos.helpers.editing

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
    var selectedItem: SharedModification? by mutableStateOf(null)
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
    fun setStrokeWidth(strokeWidth: Float, textMeasurer: TextMeasurer, currentTime: Float) {
        this.strokeWidth = strokeWidth

        if (this.selectedItem != null) {
            if (this.selectedItem is VideoModification.DrawingText) {
                val drawingText = this.selectedItem as VideoModification.DrawingText

                modifications.removeAll {
                    it == drawingText
                }

                val newSize = textMeasurer.measure(
                    text = drawingText.text.text,
                    style = DrawableText.Styles.Default.copy(
                        color = drawingText.text.paint.color,
                        fontSize = strokeWidth.sp
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
                        ),
                        keyframes = getTextKeyframes(drawingText, currentTime)
                    )

                modifications.add(new)
                this.selectedItem = new
            } else if (this.selectedItem is VideoModification.DrawingImage) {
                val drawingImage = this.selectedItem as VideoModification.DrawingImage

                modifications.removeAll {
                    it == drawingImage
                }

                val new =
                    drawingImage.copy(
                        keyframes = getImageKeyframes(drawingImage, currentTime)
                    )

                modifications.add(new)
                this.selectedItem = new
            } // TODO: for images
        }
    }

    @JvmName("privateSetColor")
    fun setColor(color: Color, currentTime: Float) {
        this.color = color

        if (this.selectedItem != null) {
            if (this.selectedItem is VideoModification.DrawingText) {
                val drawingText = this.selectedItem as VideoModification.DrawingText

                modifications.removeAll {
                    it == drawingText
                }

                val new =
                    drawingText.copy(
                        text = drawingText.text.copy(
                            paint = drawingText.text.paint.copy(
                                color = color
                            )
                        ),
                        keyframes = getTextKeyframes(drawingText, currentTime)
                    )

                modifications.add(new)
                this.selectedItem = new
            } else if (this.selectedItem is VideoModification.DrawingImage) {
                val drawingImage = this.selectedItem as VideoModification.DrawingImage

                modifications.removeAll {
                    it == drawingImage
                }

                val new =
                    drawingImage.copy(
                        keyframes = getImageKeyframes(drawingImage, currentTime)
                    )

                modifications.add(new)
                this.selectedItem = new
            } // TODO: for images
        }
    }

    @JvmName("privateSetRecordKeyframes")
    fun setRecordKeyframes(record: Boolean, currentTime: Float) {
        this.recordKeyframes = record

        if (this.selectedItem is VideoModification.DrawingText) {
            val drawingText = this.selectedItem as VideoModification.DrawingText

            modifications.removeAll {
                it == drawingText
            }

            val new =
                drawingText.copy(
                    keyframes = getTextKeyframes(drawingText, currentTime)
                )

            modifications.add(new)
            this.selectedItem = new
        } else if (this.selectedItem is VideoModification.DrawingImage) {
            val drawingImage = this.selectedItem as VideoModification.DrawingImage

            modifications.removeAll {
                it == drawingImage
            }

            val new =
                drawingImage.copy(
                    keyframes = getImageKeyframes(drawingImage, currentTime)
                )

            modifications.add(new)
            this.selectedItem = new
        } // TODO: for images
    }

    @JvmName("privateSetSelectedText")
    fun setSelectedItem(text: SharedModification?) {
        if (text is VideoModification.DrawingText) {
            this.color = text.text.paint.color
            this.strokeWidth = text.text.paint.strokeWidth
            this.paintType = text.type
        }

        this.selectedItem = text
    }

    fun undoModification() {
        modifications.removeLastOrNull()
    }

    fun clearModifications() {
        modifications.clear()
        this.selectedItem = null
    }

    private fun getTextKeyframes(
        drawingText: VideoModification.DrawingText,
        currentTime: Float
    ) =
        if (this.recordKeyframes && drawingText.keyframes != null) {
            drawingText.keyframes.toMutableList().apply {
                val last = lastOrNull()

                if (last == null || last.time == currentTime * 1000f
                    || last.strokeWidth == this@DrawingPaintState.strokeWidth
                    || last.color == this@DrawingPaintState.color
                    || last.position == drawingText.text.position
                    || last.rotation == drawingText.text.rotation
                ) {
                    removeLastOrNull()
                }

                add(
                    DrawingKeyframe.DrawingTextKeyframe(
                        position = drawingText.text.position,
                        strokeWidth = drawingText.text.paint.strokeWidth,
                        rotation = drawingText.text.rotation,
                        color = this@DrawingPaintState.color,
                        time = currentTime * 1000f
                    )
                )
            }
        } else if (this.recordKeyframes) {
            listOf(
                DrawingKeyframe.DrawingTextKeyframe(
                    position = drawingText.text.position,
                    strokeWidth = drawingText.text.paint.strokeWidth,
                    rotation = drawingText.text.rotation,
                    color = this@DrawingPaintState.color,
                    time = currentTime * 1000f
                )
            )
        } else {
            drawingText.keyframes
        }

    private fun getImageKeyframes(
        drawingImage: VideoModification.DrawingImage,
        currentTime: Float
    ) =
        if (this.recordKeyframes && drawingImage.keyframes != null) {
            drawingImage.keyframes.toMutableList().apply {
                val last = lastOrNull()

                if (last == null || last.time == currentTime * 1000f
                    || last.size == drawingImage.image.size
                    || last.position == drawingImage.image.position
                    || last.rotation == drawingImage.image.rotation
                ) {
                    removeLastOrNull()
                }

                add(
                    DrawingKeyframe.DrawingImageKeyframe(
                        position = drawingImage.image.position,
                        size = drawingImage.image.size,
                        rotation = drawingImage.image.rotation,
                        time = currentTime * 1000f
                    )
                )
            }
        } else if (this.recordKeyframes) {
            listOf(
                DrawingKeyframe.DrawingImageKeyframe(
                    position = drawingImage.image.position,
                    size = drawingImage.image.size,
                    rotation = drawingImage.image.rotation,
                    time = currentTime * 1000f
                )
            )
        } else {
            drawingImage.keyframes
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
    }

    fun removeAllEffects(predicate: (Effect?) -> Boolean) = effects.removeAll { predicate(it) }

    companion object {
        // private const val TAG = "com.kaii.photos.helpers.editing.VideoEditingState"

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