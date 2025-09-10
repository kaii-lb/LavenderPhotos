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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.editing.DrawableBlur
import com.kaii.photos.helpers.editing.DrawablePath
import com.kaii.photos.helpers.editing.DrawableText
import com.kaii.photos.helpers.editing.DrawingItems
import com.kaii.photos.helpers.editing.DrawingPaint
import com.kaii.photos.helpers.editing.DrawingPaintState
import com.kaii.photos.helpers.editing.DrawingTextKeyframe
import com.kaii.photos.helpers.editing.Modification
import com.kaii.photos.helpers.editing.PaintType
import com.kaii.photos.helpers.editing.VideoModification
import com.kaii.photos.helpers.editing.checkTapInText
import com.kaii.photos.helpers.editing.toOffset
import kotlin.math.PI
import kotlin.math.abs

// private const val TAG = "EDITING_VIEW_INPUT"

/** @param allowedToDraw no drawing happens if this is false
 * @param modifications a list of [Modification] which is all the new [DrawablePath]s or [DrawableText]s drawn
 * @param paint the paint to draw with
 * @param isDrawing is the user drawing right now? */
@Composable
fun Modifier.makeDrawCanvas(
    allowedToDraw: State<Boolean>,
    modifications: SnapshotStateList<Modification>,
    paint: MutableState<DrawingPaint>,
    isDrawing: MutableState<Boolean>,
    changesSize: MutableIntState,
    rotationMultiplier: MutableIntState,
    manualScale: MutableFloatState,
    manualOffset: MutableState<Offset>,
    selectedText: MutableState<DrawableText?>
): Modifier {
    val textMeasurer = rememberTextMeasurer()
    val localTextStyle = LocalTextStyle.current
    val defaultTextStyle = DrawableText.Styles.Default

    var lastPoint = Offset.Unspecified
    var lastText: DrawableText? = null

    return this
        .pointerInput(Unit) {
            if (allowedToDraw.value) {
                var selectedTextInGesture: DrawableText? = null

                detectTransformGestures { centroid, pan, zoom, rotation ->
                    if (paint.value.type == PaintType.Text) {
                        val tappedOnText =
                            modifications.filterIsInstance<DrawableText>()
                                .minByOrNull {
                                    (centroid - getTextBoundingBox(text = it).center).getDistanceSquared()
                                }?.let {
                                    val dstSquared = (centroid - getTextBoundingBox(text = it).center).getDistanceSquared()

                                    if (checkIfClickedOnText(
                                            text = it,
                                            clickPosition = centroid,
                                            extraPadding = dstSquared
                                        )
                                    ) {
                                        it
                                    } else null
                                }

                        tappedOnText?.let {
                            selectedTextInGesture = it
                        }

                        selectedTextInGesture?.let { text ->
                            val index =
                                modifications.indexOf(text)

                            if (index < modifications.size && index >= 0) {
                                modifications.removeAt(index)

                                // move topLeft of textbox to the text's position
                                // basically de-centers the text so we can center it to that position with the new size
                                val oldPosition =
                                    text.position + (text.size.toOffset() / 2f)
                                val newWidth = text.paint.strokeWidth * zoom

                                val textLayout = textMeasurer.measure(
                                    text = text.text,
                                    style = localTextStyle.copy(
                                        color = paint.value.color,
                                        fontSize = TextUnit(
                                            newWidth,
                                            TextUnitType.Sp
                                        ),
                                        textAlign = defaultTextStyle.textAlign,
                                        platformStyle = defaultTextStyle.platformStyle,
                                        lineHeightStyle = defaultTextStyle.lineHeightStyle,
                                        baselineShift = defaultTextStyle.baselineShift
                                    )
                                )

                                val zoomedText = text.copy(
                                    paint = text.paint.copy(
                                        strokeWidth = newWidth
                                    ),
                                    size = textLayout.size,
                                    position = oldPosition - (textLayout.size.toOffset() / 2f), // move from old topLeft to new center
                                    rotation = if (zoom != 1f) text.rotation + rotation else text.rotation
                                )

                                modifications.add(index, zoomedText)
                            }
                        }
                    }

                    selectedText.value = null
                }
            }
        }
        .pointerInput(Unit) {
            if (allowedToDraw.value) {
                var touchOffset: Offset = Offset.Zero

                detectDragGestures(
                    onDragStart = { position ->
                        if (paint.value.type == PaintType.Text) {
                            val tappedOnText =
                                modifications.filterIsInstance<DrawableText>().minByOrNull {
                                    (position - getTextBoundingBox(text = it).center).getDistanceSquared()
                                }?.let {
                                    if (checkIfClickedOnText(
                                            text = it,
                                            clickPosition = position
                                        )
                                    ) {
                                        it
                                    } else null
                                }

                            if (tappedOnText != null) {
                                lastText = tappedOnText
                                lastText!!.position.let {
                                    touchOffset = position - it
                                }
                            } else {
                                lastText = null
                            }
                        } else {
                            val path = Path().apply {
                                moveTo(position.x, position.y)
                            }

                            modifications.add(
                                if (paint.value.type == PaintType.Blur) {
                                    DrawableBlur(
                                        path,
                                        paint.value
                                    )
                                } else {
                                    DrawablePath(
                                        path,
                                        paint.value
                                    )
                                }
                            )

                            lastPoint = position
                        }
                        isDrawing.value = true
                        changesSize.intValue += 1
                        selectedText.value = null
                    },

                    onDrag = { change, difference ->
                        if (paint.value.type == PaintType.Text) {
                            if (lastText != null && modifications.remove(lastText!!)) {
                                lastText!!.position += (change.position - lastText!!.position - touchOffset)
                                modifications.add(lastText!!)
                            }

                            isDrawing.value = true
                        } else {
                            val paintIsBlur = paint.value.type == PaintType.Blur

                            var path =
                                (modifications.findLast {
                                    if (paintIsBlur) it is DrawableBlur
                                    else it is DrawablePath
                                })?.let {
                                    if (it is DrawableBlur) it.path
                                    else (it as DrawablePath).path
                                }

                            if (path == null) {
                                val newPath =
                                    if (paintIsBlur) {
                                        val new = DrawableBlur(
                                            Path().apply {
                                                moveTo(change.position.x, change.position.y)
                                            },
                                            paint.value
                                        )
                                        path = new.path
                                        new
                                    } else {
                                        val new = DrawablePath(
                                            Path().apply {
                                                moveTo(change.position.x, change.position.y)
                                            },
                                            paint.value
                                        )
                                        path = new.path
                                        new
                                    }

                                modifications.add(newPath)
                            } else {
                                modifications.removeAll {
                                    when (it) {
                                        is DrawablePath -> {
                                            it.path == path && it.paint == paint.value
                                        }

                                        is DrawableBlur -> {
                                            it.path == path && it.paint == paint.value
                                        }

                                        else -> {
                                            false
                                        }
                                    }
                                }
                            }

                            path.quadraticTo(
                                lastPoint.x,
                                lastPoint.y,
                                (lastPoint.x + change.position.x) / 2,
                                (lastPoint.y + change.position.y) / 2
                            )

                            modifications.add(
                                if (paintIsBlur) {
                                    DrawableBlur(
                                        path,
                                        paint.value
                                    )
                                } else {
                                    DrawablePath(
                                        path,
                                        paint.value
                                    )
                                }
                            )

                            lastPoint = change.position
                        }

                        isDrawing.value = true
                        changesSize.intValue += 1
                        selectedText.value = null
                    },

                    onDragEnd = {
                        isDrawing.value = false
                        changesSize.intValue += 1
                        selectedText.value = null
                    },

                    onDragCancel = {
                        isDrawing.value = false
                        changesSize.intValue += 1
                        selectedText.value = null
                    }
                )
            }
        }
        .pointerInput(Unit) {
            if (allowedToDraw.value) {
                detectDragGesturesAfterLongPress { change, offset ->
                    manualOffset.value += (offset * manualScale.floatValue)
                    selectedText.value = null
                }
            }
        }
        .pointerInput(Unit) {
            if (allowedToDraw.value) {
                detectTapGestures(
                    onTap = { position ->
                        if (paint.value.type == PaintType.Text) {
                            val tappedOnText =
                                modifications.filterIsInstance<DrawableText>().minByOrNull {
                                    (position - getTextBoundingBox(text = it).center).getDistanceSquared()
                                }?.let {
                                    if (checkIfClickedOnText(
                                            text = it,
                                            clickPosition = position
                                        )
                                    ) {
                                        it
                                    } else null
                                }

                            if (tappedOnText == null) {
                                val textLayout = textMeasurer.measure(
                                    text = "text",
                                    style = localTextStyle.copy(
                                        color = paint.value.color,
                                        fontSize = TextUnit(
                                            paint.value.strokeWidth,
                                            TextUnitType.Sp
                                        ),
                                        textAlign = defaultTextStyle.textAlign,
                                        platformStyle = defaultTextStyle.platformStyle,
                                        lineHeightStyle = defaultTextStyle.lineHeightStyle,
                                        baselineShift = defaultTextStyle.baselineShift
                                    )
                                )

                                val text = DrawableText(
                                    text = "text",
                                    position = Offset(
                                        position.x - textLayout.size.width / 2f,
                                        position.y - textLayout.size.height / 2f
                                    ),
                                    paint = paint.value,
                                    rotation = 90f * rotationMultiplier.intValue,
                                    size = textLayout.size
                                )

                                modifications.add(text)
                                lastText = text
                            } else {
                                if (selectedText.value == tappedOnText) selectedText.value = null else selectedText.value = tappedOnText
                                lastText = tappedOnText
                            }
                        } else {
                            val path = Path().apply {
                                moveTo(position.x, position.y)
                            }

                            modifications.add(
                                if (paint.value.type == PaintType.Blur) {
                                    DrawableBlur(
                                        path,
                                        paint.value
                                    )
                                } else {
                                    DrawablePath(
                                        path.apply {
                                            lineTo(position.x + 1, position.y + 1)
                                        },
                                        paint.value
                                    )
                                }
                            )

                            lastPoint = position
                            selectedText.value = null
                        }

                        changesSize.intValue += 1
                    },

                    onDoubleTap = {
                        selectedText.value = null
                        manualScale.floatValue = 0f
                        manualOffset.value = Offset.Zero
                    }
                )
            }
        }
}

@Composable
fun Modifier.makeVideoDrawCanvas(
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
                    var currentText: VideoModification.DrawingText? = null

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

                                var tappedOnText = drawingPaintState.modifications.find {
                                    it as? VideoModification.DrawingText != null
                                            && it.text.checkTapInText(
                                        tapPosition = centroid,
                                        padding = 0f
                                    )
                                }

                                if (currentText != null) tappedOnText = currentText
                                else if (tappedOnText != null) currentText = tappedOnText as VideoModification.DrawingText

                                if (tappedOnText != null) {
                                    drawingPaintState.modifications.remove(tappedOnText)

                                    val strokeWidth = (tappedOnText.text.paint.strokeWidth * zoomChange).coerceIn(1f, 128f)
                                    val newSize = textMeasurer.measure(
                                        text = tappedOnText.text.text,
                                        style = DrawableText.Styles.Default.copy(
                                            color = drawingPaintState.paint.color,
                                            fontSize = TextUnit(strokeWidth, TextUnitType.Sp)
                                        )
                                    ).size

                                    drawingPaintState.setStrokeWidth(strokeWidth)

                                    val positon = tappedOnText.text.position - (newSize.toOffset() - tappedOnText.text.size.toOffset()) / 2f
                                    val keyframe =
                                        if (drawingPaintState.recordKeyframes) {
                                            DrawingTextKeyframe(
                                                position = positon,
                                                color = drawingPaintState.color,
                                                time = currentVideoPosition.floatValue * 1000f,
                                                strokeWidth = strokeWidth,
                                                rotation = tappedOnText.text.rotation + rotationChange
                                            )
                                        } else {
                                            null
                                        }

                                    val keyframeList =
                                        if (keyframe != null) {
                                            (currentText?.keyframes ?: emptyList()).toMutableList().apply {
                                                removeAll {
                                                    it.time == keyframe.time
                                                }
                                                add(keyframe)
                                            }
                                        } else {
                                            currentText?.keyframes
                                        }

                                    tappedOnText = tappedOnText.copy(
                                        text = tappedOnText.text.copy(
                                            size = newSize,
                                            paint = tappedOnText.text.paint.copy(
                                                color = drawingPaintState.color,
                                                strokeWidth = strokeWidth
                                            ),
                                            position = positon,
                                            rotation = tappedOnText.text.rotation + rotationChange
                                        ),
                                        keyframes = keyframeList
                                    )

                                    drawingPaintState.modifications.add(tappedOnText)
                                    currentText = tappedOnText
                                } else if (zoomChange != 1f || panChange != Offset.Zero) {
                                    val oldScale = zoom
                                    val isPanning = panChange.getDistanceSquared() >= 20f

                                    val newScale = if (isPanning) zoom else (zoom * zoomChange).coerceIn(1f, 5f)
                                    val newOffset =
                                        (offset + centroid / oldScale) - (centroid / newScale + panChange)

                                    zoom = newScale
                                    offset = if (zoom != 1f) newOffset else Offset.Zero
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
                            var currentText: VideoModification.DrawingText? = null
                            var touchOffset = Offset.Zero

                            val tappedOnText = drawingPaintState.modifications.find {
                                it as? VideoModification.DrawingText != null
                                        && it.text.checkTapInText(
                                    tapPosition = down.position,
                                    padding = 16.dp.toPx()
                                )
                            }

                            if (tappedOnText != null) {
                                currentText = tappedOnText as VideoModification.DrawingText
                                touchOffset = down.position - currentText.text.position
                            } else {
                                addText(down.position)
                            }

                            do {
                                val event = awaitPointerEvent()
                                val canceled = event.changes.fastAny { it.isConsumed }

                                val dragChange = event.changes.firstOrNull { it.id == down.id }

                                if (dragChange != null) {
                                    if (currentText != null) {
                                        drawingPaintState.modifications.remove(currentText)

                                        val position = dragChange.position - touchOffset
                                        val keyframe =
                                            if (drawingPaintState.recordKeyframes) {
                                                DrawingTextKeyframe(
                                                    position = position,
                                                    color = drawingPaintState.color,
                                                    time = currentVideoPosition.floatValue * 1000f,
                                                    strokeWidth = drawingPaintState.strokeWidth,
                                                    rotation = currentText.text.rotation
                                                )
                                            } else {
                                                null
                                            }

                                        val keyframeList =
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

                                        currentText = currentText.copy(
                                            text = currentText.text.copy(
                                                position = position,
                                                paint = currentText.text.paint.copy(
                                                    color = drawingPaintState.color
                                                )
                                            ),
                                            keyframes = keyframeList
                                        )

                                        drawingPaintState.modifications.add(currentText)
                                    }

                                    dragChange.consume()
                                } else {
                                    break
                                }
                            } while (!canceled && event.changes.fastAny { it.pressed })
                        }

                        DrawingItems.Image -> {

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
    // .pointerInput(enabled, isDrawing) {
    //     if (!enabled || isDrawing) return@pointerInput
    //
    //     detectTapGestures(
    //         onDoubleTap = { centroid ->
    //             if (zoom != 1f) {
    //                 zoom = 1f
    //                 offset = Offset.Zero
    //             } else {
    //                 offset = offset + centroid / 2f
    //                 zoom = 2f
    //             }
    //         }
    //     )
    // }

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