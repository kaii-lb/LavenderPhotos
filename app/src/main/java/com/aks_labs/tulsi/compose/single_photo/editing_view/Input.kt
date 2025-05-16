package com.aks_labs.tulsi.compose.single_photo.editing_view

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import com.aks_labs.tulsi.compose.single_photo.checkIfClickedOnText
import com.aks_labs.tulsi.compose.single_photo.getTextBoundingBox
import com.aks_labs.tulsi.helpers.DrawableBlur
import com.aks_labs.tulsi.helpers.DrawablePath
import com.aks_labs.tulsi.helpers.DrawableText
import com.aks_labs.tulsi.helpers.ExtendedPaint
import com.aks_labs.tulsi.helpers.Modification
import com.aks_labs.tulsi.helpers.PaintType
import com.aks_labs.tulsi.helpers.toOffset

// private const val TAG = "EDITING_VIEW_INPUT"

/** @param allowedToDraw no drawing happens if this is false
 * @param modifications a list of [Modification] which is all the new [DrawablePath]s or [DrawableText]s drawn
 * @param paint the paint to draw with
 * @param isDrawing is the user drawing right now? */
@Composable
fun Modifier.makeDrawCanvas(
    allowedToDraw: State<Boolean>,
    modifications: SnapshotStateList<Modification>,
    paint: MutableState<ExtendedPaint>,
    isDrawing: MutableState<Boolean>,
    changesSize: MutableIntState,
    rotationMultiplier: MutableIntState,
    manualScale: MutableFloatState,
    manualOffset: MutableState<Offset>,
    selectedText: MutableState<DrawableText?>
): Modifier {
    val textMeasurer = rememberTextMeasurer()
    val localTextStyle = LocalTextStyle.current
    val defaultTextStyle = DrawableText.Styles.Default.style

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
                                    if (checkIfClickedOnText(
                                            text = it,
                                            clickPosition = centroid,
                                            extraPadding = it.size.width.toFloat() / 2
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
                                        DrawableBlur(
                                            Path().apply {
                                                moveTo(change.position.x, change.position.y)
                                            },
                                            paint.value
                                        )
                                    } else {
                                        DrawablePath(
                                            Path().apply {
                                                moveTo(change.position.x, change.position.y)
                                            },
                                            paint.value
                                        )
                                    }

                                path = newPath.path!!
                                modifications.add(newPath)
                            } else {
                                modifications.removeAll {
                                    if (it is DrawablePath) {
                                        it.path == path && it.paint == paint.value
                                    } else if (it is DrawableBlur) {
                                        it.path == path && it.paint == paint.value
                                    } else {
                                        false
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

