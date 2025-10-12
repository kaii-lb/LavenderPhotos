package com.kaii.photos.compose.editing_view

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntSize
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.editing.DrawablePath
import com.kaii.photos.helpers.editing.DrawableText
import com.kaii.photos.helpers.editing.DrawingItems
import com.kaii.photos.helpers.editing.DrawingKeyframe
import com.kaii.photos.helpers.editing.DrawingPaintState
import com.kaii.photos.helpers.editing.ImageModification
import com.kaii.photos.helpers.editing.SharedModification
import com.kaii.photos.helpers.editing.VideoModification
import com.kaii.photos.helpers.editing.checkTapInBitmap
import com.kaii.photos.helpers.editing.checkTapInText
import com.kaii.photos.helpers.editing.toOffset
import kotlin.math.PI
import kotlin.math.abs

@Composable
fun Modifier.makeDrawCanvas(
    drawingPaintState: DrawingPaintState,
    currentVideoPosition: MutableFloatState,
    textMeasurer: TextMeasurer,
    enabled: Boolean,
    addText: (position: Offset) -> Unit
): Modifier {
    var zoom by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(enabled) {
        if (!enabled) {
            zoom = 1f
            offset = Offset.Zero
        }
    }

    val animatedZoom by animateFloatAsState(
        targetValue = zoom,
        animationSpec = tween(
            durationMillis = if (zoom == 1f) AnimationConstants.DURATION else 0
        )
    )

    val animatedOffset by animateOffsetAsState(
        targetValue = offset,
        animationSpec = tween(
            durationMillis = if (offset == Offset.Zero) AnimationConstants.DURATION else 0
        )
    )

    val modifier = Modifier
        .graphicsLayer {
            translationX = -animatedOffset.x * animatedZoom
            translationY = -animatedOffset.y * animatedZoom
            scaleX = animatedZoom
            scaleY = animatedZoom
            transformOrigin = TransformOrigin(0f, 0f)
        }
        .pointerInput(enabled, drawingPaintState.paintType, drawingPaintState.color, drawingPaintState.recordKeyframes) {
            if (!enabled) return@pointerInput

            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val initialEvent = awaitPointerEvent(PointerEventPass.Initial)

                if (initialEvent.changes.size > 1) {
                    var localZoom = 1f
                    var pan = Offset.Zero
                    var localRotation = 0f
                    var pastTouchSlop = false
                    val touchSlop = viewConfiguration.touchSlop
                    val currentText: MutableState<SharedModification.DrawingText?> = mutableStateOf(null)
                    val currentImage: MutableState<SharedModification.DrawingImage?> = mutableStateOf(null)

                    do {
                        val event = awaitPointerEvent()
                        val canceled = event.changes.fastAny { it.isConsumed }

                        if (!canceled) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val rotationChange = event.calculateRotation()

                            if (!pastTouchSlop) {
                                localZoom *= zoomChange
                                pan += panChange
                                localRotation += rotationChange

                                val centroidSize = event.calculateCentroidSize(useCurrent = false)
                                val zoomMotion = abs(1 - localZoom) * centroidSize
                                val panMotion = pan.getDistance()
                                val rotationMotion = abs(localRotation * PI.toFloat() * centroidSize / 180f)

                                pastTouchSlop = zoomMotion > touchSlop || rotationMotion > touchSlop || panMotion > touchSlop
                            }

                            if (pastTouchSlop) {
                                val centroid = event.calculateCentroid(useCurrent = false)

                                val tappedOnText = mutableStateOf(drawingPaintState.modifications.find {
                                    it as? SharedModification.DrawingText != null
                                            && it.text.checkTapInText(
                                        tapPosition = centroid,
                                        padding = 0f
                                    ) && drawingPaintState.paintType == DrawingItems.Text // check if were in text mode
                                } as? SharedModification.DrawingText)

                                if (currentText.value != null) tappedOnText.value = currentText.value
                                else if (tappedOnText.value != null) currentText.value = tappedOnText.value as SharedModification.DrawingText

                                val tappedOnImage = mutableStateOf(drawingPaintState.modifications.find {
                                    it as? SharedModification.DrawingImage != null
                                            && it.image.checkTapInBitmap(
                                        tapPosition = centroid,
                                        padding = 0f
                                    ) && drawingPaintState.paintType == DrawingItems.Image // check if were in image mode
                                } as? SharedModification.DrawingImage)

                                if (currentImage.value != null) tappedOnImage.value = currentImage.value
                                else if (tappedOnImage.value != null) currentImage.value = tappedOnImage.value as SharedModification.DrawingImage

                                if (tappedOnText.value != null) {
                                    handleTextTransform(
                                        drawingPaintState = drawingPaintState,
                                        tappedOnText = tappedOnText,
                                        zoomChange = zoomChange,
                                        panChange = panChange,
                                        rotationChange = rotationChange,
                                        textMeasurer = textMeasurer,
                                        currentVideoPosition = currentVideoPosition,
                                        currentText = currentText
                                    )
                                } else if (tappedOnImage.value != null) {
                                    handleImageTransform(
                                        drawingPaintState = drawingPaintState,
                                        tappedOnImage = tappedOnImage,
                                        zoomChange = zoomChange,
                                        panChange = panChange,
                                        rotationChange = rotationChange,
                                        currentVideoPosition = currentVideoPosition,
                                        currentImage = currentImage
                                    )
                                } else if (zoomChange != 1f || panChange != Offset.Zero) {
                                    val oldScale = zoom

                                    val newScale = (zoom * zoomChange).coerceIn(1f, 5f)
                                    val newOffset =
                                        (offset + centroid / oldScale) - (centroid / newScale + panChange)

                                    zoom = newScale
                                    offset = newOffset
                                }

                                event.changes.fastForEach {
                                    if (it.positionChanged()) {
                                        it.consume()
                                    }
                                }
                            }
                        }
                    } while (!canceled && event.changes.fastAny { it.pressed } && event.changes.size == 2)
                } else {
                    when (drawingPaintState.paintType) {
                        DrawingItems.Text -> {
                            handleTextDrawing(
                                drawingPaintState = drawingPaintState,
                                initialDown = initialEvent.changes.first(),
                                down = down,
                                currentVideoPosition = currentVideoPosition,
                                addText = addText
                            )
                        }

                        DrawingItems.Image -> {
                            handleImageDrawing(
                                drawingPaintState = drawingPaintState,
                                initialDown = initialEvent.changes.first(),
                                down = down,
                                currentVideoPosition = currentVideoPosition
                            )
                        }

                        else -> {
                            handlePencilHighlighterDrawing(
                                drawingPaintState = drawingPaintState,
                                down = down
                            )
                        }
                    }
                }
            }
        }

    return this.then(modifier)
}

private suspend fun AwaitPointerEventScope.handlePencilHighlighterDrawing(
    drawingPaintState: DrawingPaintState,
    down: PointerInputChange
) {
    var currentPath: VideoModification.DrawingPath
    var lastPoint: Offset
    var hasDragged = false

    val newPath = VideoModification.DrawingPath(
        type = DrawingItems.Pencil,
        path = DrawablePath(
            path = Path().apply {
                moveTo(down.position.x, down.position.y)
            },
            paint = drawingPaintState.paint
        )
    )
    drawingPaintState.modifications.add(newPath)
    currentPath = newPath
    lastPoint = down.position

    do {
        val event = awaitPointerEvent()
        val canceled = event.changes.fastAny { it.isConsumed }
        val isUp = event.changes.fastAny { it.id == down.id && !it.pressed }

        if (isUp) {
            if (!hasDragged) {
                drawingPaintState.modifications.remove(currentPath)
                currentPath.path.path.lineTo(down.position.x, down.position.y)
                drawingPaintState.modifications.add(currentPath)
            }

            break
        }

        val dragChange = event.changes.firstOrNull { it.id == down.id }

        if (dragChange != null) {
            hasDragged = dragChange.positionChanged()

            drawingPaintState.modifications.remove(currentPath)
            currentPath.path.path.apply {
                quadraticTo(
                    lastPoint.x,
                    lastPoint.y,
                    (lastPoint.x + dragChange.position.x) / 2,
                    (lastPoint.y + dragChange.position.y) / 2
                )
            }
            drawingPaintState.modifications.add(currentPath)

            lastPoint = dragChange.position
            dragChange.consume()
        } else {
            break
        }
    } while (!canceled && event.changes.fastAny { it.pressed })
}

private suspend fun AwaitPointerEventScope.handleTextDrawing(
    drawingPaintState: DrawingPaintState,
    initialDown: PointerInputChange,
    down: PointerInputChange,
    currentVideoPosition: MutableFloatState,
    addText: (Offset) -> Unit
) {
    var currentText: SharedModification.DrawingText? = null
    var touchOffset = Offset.Zero

    val tappedOnText = drawingPaintState.modifications.find {
        it as? SharedModification.DrawingText != null
                && it.text.checkTapInText(
            tapPosition = down.position,
            padding = 16.dp.toPx()
        )
    }

    if (tappedOnText != null) {
        if (tappedOnText == drawingPaintState.selectedItem && initialDown.position == down.position) {
            drawingPaintState.setSelectedItem(null)
        } else {
            currentText = tappedOnText as SharedModification.DrawingText
            touchOffset = down.position - currentText.text.position
            drawingPaintState.setSelectedItem(tappedOnText)
        }
    } else {
        if (drawingPaintState.selectedItem == null) {
            drawingPaintState.setRecordKeyframes(
                record = false,
                currentTime = currentVideoPosition.floatValue
            )
            addText(down.position)
        } else {
            drawingPaintState.setSelectedItem(null)
        }
    }

    do {
        val event = awaitPointerEvent()
        val canceled = event.changes.fastAny { it.isConsumed }

        val dragChange = event.changes.firstOrNull { it.id == down.id }

        if (dragChange != null) {
            if (currentText != null) {
                drawingPaintState.modifications.remove(currentText)

                val position = dragChange.position - touchOffset
                if (drawingPaintState.strokeWidth != currentText.text.paint.strokeWidth) return

                val keyframe =
                    if (drawingPaintState.recordKeyframes) {
                        DrawingKeyframe.DrawingTextKeyframe(
                            position = position,
                            color = currentText.text.paint.color,
                            time = currentVideoPosition.floatValue * 1000f,
                            strokeWidth = currentText.text.paint.strokeWidth,
                            rotation = currentText.text.rotation
                        )
                    } else {
                        null
                    }

                val keyframeList =
                    if (tappedOnText is VideoModification.DrawingText) {
                        currentText as VideoModification.DrawingText
                        if (keyframe != null) {
                            (currentText.keyframes ?: emptyList()).toMutableList().apply {
                                removeAll {
                                    it.time == keyframe.time
                                }
                                add(keyframe)
                            }
                        } else {
                            currentText.keyframes
                        }
                    } else {
                        null
                    }

                if (currentText is VideoModification.DrawingText) {
                    currentText = currentText.copy(
                        text = currentText.text.copy(
                            position = position,
                            paint = currentText.text.paint.copy(
                                color = currentText.text.paint.color
                            )
                        ),
                        keyframes = keyframeList
                    )
                } else {
                    currentText = (currentText as ImageModification.DrawingText).copy(
                        text = currentText.text.copy(
                            position = position,
                            paint = currentText.text.paint.copy(
                                color = currentText.text.paint.color
                            )
                        )
                    )
                }


                drawingPaintState.modifications.add(currentText)
                drawingPaintState.setSelectedItem(currentText)
            }

            dragChange.consume()
        } else {
            break
        }
    } while (!canceled && event.changes.fastAny { it.pressed })
}

private fun handleTextTransform(
    drawingPaintState: DrawingPaintState,
    tappedOnText: MutableState<SharedModification.DrawingText?>,
    zoomChange: Float,
    panChange: Offset,
    rotationChange: Float,
    textMeasurer: TextMeasurer,
    currentVideoPosition: MutableFloatState,
    currentText: MutableState<SharedModification.DrawingText?>
) {
    if (tappedOnText.value == null) return

    drawingPaintState.modifications.remove(tappedOnText.value!!)

    val strokeWidth = (tappedOnText.value!!.text.paint.strokeWidth * zoomChange).coerceIn(1f, 128f)
    if (drawingPaintState.strokeWidth != tappedOnText.value!!.text.paint.strokeWidth) return

    val newSize = textMeasurer.measure(
        text = tappedOnText.value!!.text.text,
        style = DrawableText.Styles.Default.copy(
            color = drawingPaintState.paint.color,
            fontSize = strokeWidth.sp
        )
    ).size

    val positon = tappedOnText.value!!.text.position - (newSize.toOffset() - tappedOnText.value!!.text.size.toOffset()) / 2f
    val keyframe =
        if (drawingPaintState.recordKeyframes) {
            DrawingKeyframe.DrawingTextKeyframe(
                position = positon,
                color = tappedOnText.value!!.text.paint.color,
                time = currentVideoPosition.floatValue * 1000f,
                strokeWidth = strokeWidth,
                rotation = tappedOnText.value!!.text.rotation + rotationChange
            )
        } else {
            null
        }

    val keyframeList =
        if (currentText.value is VideoModification.DrawingText) {
            if (keyframe != null) {
                ((currentText.value as? VideoModification.DrawingText)?.keyframes ?: emptyList()).toMutableList().apply {
                    removeAll {
                        it.time == keyframe.time
                    }
                    add(keyframe)
                }
            } else {
                (currentText.value as? VideoModification.DrawingText)?.keyframes
            }
        } else {
            null
        }

    if (tappedOnText.value is VideoModification.DrawingText) {
        tappedOnText.value =
            (tappedOnText.value as VideoModification.DrawingText).copy(
                text = tappedOnText.value!!.text.copy(
                    size = newSize,
                    paint = tappedOnText.value!!.text.paint.copy(
                        color = tappedOnText.value!!.text.paint.color,
                        strokeWidth = strokeWidth
                    ),
                    position = positon + panChange,
                    rotation = tappedOnText.value!!.text.rotation + rotationChange
                ),
                keyframes = keyframeList
            )
    } else {
        tappedOnText.value =
            (tappedOnText.value as ImageModification.DrawingText).copy(
                text = tappedOnText.value!!.text.copy(
                    size = newSize,
                    paint = tappedOnText.value!!.text.paint.copy(
                        color = tappedOnText.value!!.text.paint.color,
                        strokeWidth = strokeWidth
                    ),
                    position = positon + panChange,
                    rotation = tappedOnText.value!!.text.rotation + rotationChange
                )
            )
    }

    drawingPaintState.modifications.add(tappedOnText.value!!)
    drawingPaintState.setSelectedItem(tappedOnText.value!!)
    currentText.value = tappedOnText.value!!
}

private suspend fun AwaitPointerEventScope.handleImageDrawing(
    drawingPaintState: DrawingPaintState,
    initialDown: PointerInputChange,
    down: PointerInputChange,
    currentVideoPosition: MutableFloatState
) {
    var currentImage: SharedModification.DrawingImage? = null
    var touchOffset = Offset.Zero

    val tappedOnImage = drawingPaintState.modifications.find {
        it as? SharedModification.DrawingImage != null
                && it.image.checkTapInBitmap(
            tapPosition = down.position,
            padding = 16.dp.toPx()
        )
    }

    if (tappedOnImage != null) {
        if (tappedOnImage == drawingPaintState.selectedItem && initialDown.position == down.position) {
            drawingPaintState.setSelectedItem(null)
        } else {
            currentImage = tappedOnImage as SharedModification.DrawingImage
            touchOffset = down.position - currentImage.image.position
            drawingPaintState.setSelectedItem(tappedOnImage)
        }
    } else {
        if (drawingPaintState.selectedItem is SharedModification.DrawingImage) {
            val selectedImage = drawingPaintState.selectedItem as SharedModification.DrawingImage
            val aspectRatio = selectedImage.image.size.width.toFloat() / selectedImage.image.size.height

            val width = size.width.toFloat() / 3
            val newSize = Size(width, width / aspectRatio)

            val item =
                if (selectedImage is VideoModification.DrawingImage) {
                    selectedImage.copy(
                        image = selectedImage.image.copy(
                            position = down.position - newSize.toOffset() / 2f,
                            size = newSize.roundToIntSize()
                        )
                    )
                } else {
                    (selectedImage as ImageModification.DrawingImage).copy(
                        image = selectedImage.image.copy(
                            position = down.position - newSize.toOffset() / 2f,
                            size = newSize.roundToIntSize()
                        )
                    )
                }

            drawingPaintState.modifications.add(item)

            drawingPaintState.setSelectedItem(item)

            drawingPaintState.setRecordKeyframes(
                record = false,
                currentTime = currentVideoPosition.floatValue
            )
        } else {
            drawingPaintState.setSelectedItem(null)
        }
    }

    do {
        val event = awaitPointerEvent()
        val canceled = event.changes.fastAny { it.isConsumed }

        val dragChange = event.changes.firstOrNull { it.id == down.id }

        if (dragChange != null) {
            if (currentImage != null) {
                drawingPaintState.modifications.remove(currentImage)

                val position = dragChange.position - touchOffset
                val keyframe =
                    if (drawingPaintState.recordKeyframes) {
                        DrawingKeyframe.DrawingImageKeyframe(
                            position = position,
                            time = currentVideoPosition.floatValue * 1000f,
                            rotation = currentImage.image.rotation,
                            size = currentImage.image.size
                        )
                    } else {
                        null
                    }

                val keyframeList =
                    if (currentImage is VideoModification.DrawingImage) {
                        if (keyframe != null) {
                            (currentImage.keyframes ?: emptyList()).toMutableList().apply {
                                removeAll {
                                    it.time == keyframe.time
                                }
                                add(keyframe)
                            }
                        } else {
                            currentImage.keyframes
                        }
                    } else {
                        null
                    }

                if (currentImage is VideoModification.DrawingImage) {
                    currentImage = currentImage.copy(
                        image = currentImage.image.copy(
                            position = position,
                            paint = currentImage.image.paint.copy(
                                color = currentImage.image.paint.color
                            )
                        ),
                        keyframes = keyframeList
                    )
                } else {
                    currentImage = (currentImage as ImageModification.DrawingImage).copy(
                        image = currentImage.image.copy(
                            position = position,
                            paint = currentImage.image.paint.copy(
                                color = currentImage.image.paint.color
                            )
                        )
                    )
                }

                drawingPaintState.modifications.add(currentImage)
                drawingPaintState.setSelectedItem(currentImage)
            }

            dragChange.consume()
        } else {
            break
        }
    } while (!canceled && event.changes.fastAny { it.pressed })
}

private fun handleImageTransform(
    drawingPaintState: DrawingPaintState,
    tappedOnImage: MutableState<SharedModification.DrawingImage?>,
    zoomChange: Float,
    panChange: Offset,
    rotationChange: Float,
    currentVideoPosition: MutableFloatState,
    currentImage: MutableState<SharedModification.DrawingImage?>
) {
    if (tappedOnImage.value == null) return

    drawingPaintState.modifications.remove(tappedOnImage.value!!)

    val newSize = (tappedOnImage.value!!.image.size.toSize() * zoomChange).roundToIntSize()

    val positon = tappedOnImage.value!!.image.position - (newSize.toOffset() - tappedOnImage.value!!.image.size.toOffset()) / 2f
    val keyframe =
        if (drawingPaintState.recordKeyframes) {
            DrawingKeyframe.DrawingImageKeyframe(
                position = positon,
                time = currentVideoPosition.floatValue * 1000f,
                rotation = tappedOnImage.value!!.image.rotation + rotationChange,
                size = newSize
            )
        } else {
            null
        }

    val keyframeList =
        if (currentImage.value is VideoModification.DrawingImage) {
            if (keyframe != null) {
                ((currentImage.value as? VideoModification.DrawingImage)?.keyframes ?: emptyList()).toMutableList().apply {
                    removeAll {
                        it.time == keyframe.time
                    }
                    add(keyframe)
                }
            } else {
                (currentImage.value as? VideoModification.DrawingImage)?.keyframes
            }
        } else {
            null
        }

    if (currentImage.value is VideoModification.DrawingImage) {
        tappedOnImage.value =
            (currentImage.value as VideoModification.DrawingImage).copy(
                image = tappedOnImage.value!!.image.copy(
                    size = newSize,
                    position = positon + panChange,
                    rotation = tappedOnImage.value!!.image.rotation + rotationChange
                ),
                keyframes = keyframeList
            )
    } else {
        tappedOnImage.value =
            (currentImage.value as ImageModification.DrawingImage).copy(
                image = tappedOnImage.value!!.image.copy(
                    size = newSize,
                    position = positon + panChange,
                    rotation = tappedOnImage.value!!.image.rotation + rotationChange
                )
            )
    }

    drawingPaintState.modifications.add(tappedOnImage.value!!)
    drawingPaintState.setSelectedItem(tappedOnImage.value!!)
    currentImage.value = tappedOnImage.value!!
}