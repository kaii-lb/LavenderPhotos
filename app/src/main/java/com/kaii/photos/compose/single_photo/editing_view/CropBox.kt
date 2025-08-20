package com.kaii.photos.compose.single_photo.editing_view

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.kaii.photos.helpers.AnimationConstants
import kotlin.math.abs

private const val TAG = "CROP_BOX"

@Composable
fun CropBox(
    containerWidth: Float,
    containerHeight: Float,
    mediaWidth: Float,
    mediaHeight: Float,
    modifier: Modifier = Modifier,
    onAreaChanged: (area: Rect) -> Unit
) {
    val containerAspectRatio = remember {
        containerWidth / containerHeight
    }
    val videoAspectRatio = remember {
        mediaWidth / mediaHeight
    }
    val originalWidth = remember {
        if (containerAspectRatio > videoAspectRatio) {
            containerHeight * videoAspectRatio
        } else {
            containerHeight / videoAspectRatio
        }
    }
    val originalHeight = remember {
        if (containerAspectRatio > videoAspectRatio) {
            containerHeight
        } else {
            containerWidth
        }
    }

    var width by remember { mutableFloatStateOf(
        originalWidth
    )}
    var height by remember { mutableFloatStateOf(
        originalHeight
    )}
    var top by remember { mutableFloatStateOf(
        (containerHeight - height) / 2
    )}
    var left by remember { mutableFloatStateOf(
        (containerWidth - width) / 2
    )}

    LaunchedEffect(top, left, width, height) {
        onAreaChanged(
            Rect(
                top = top,
                left = left,
                bottom = top + height,
                right = left + width
            )
        )
    }

    var selectedArea by remember { mutableStateOf(SelectedCropArea.None) }

    val animatedColor by animateColorAsState(
        targetValue = Color.White.copy(alpha = if (selectedArea == SelectedCropArea.None) 0f else 0.6f),
        animationSpec = tween(
            durationMillis = AnimationConstants.DURATION
        )
    )

    Box(
        modifier = modifier
            .size(
                width = containerWidth.dp,
                height = containerHeight.dp
            )
            .drawWithContent {
                drawContent()

                val strokeWidth = 4.dp.toPx()

                drawOutline(
                    outline = Outline.Rounded(
                        roundRect = RoundRect(
                            left = left,
                            top = top,
                            right = left + width,
                            bottom = top + height,
                            cornerRadius = CornerRadius(x = 2.dp.toPx(), y = 2.dp.toPx())
                        )
                    ),
                    style = Stroke(
                        width = strokeWidth
                    ),
                    color = Color.White.copy(alpha = 0.6f)
                )

                // guideline vertical left
                drawLine(
                    color = animatedColor,
                    start = Offset(
                        x = left + width / 3,
                        y = top + strokeWidth / 2
                    ),
                    end = Offset(
                        x = left + width / 3,
                        y = top + height - strokeWidth / 2
                    ),
                    strokeWidth = 2.dp.toPx()
                )

                // guideline vertical right
                drawLine(
                    color = animatedColor,
                    start = Offset(
                        x = left + width - width / 3,
                        y = top + strokeWidth / 2
                    ),
                    end = Offset(
                        x = left + width - width / 3,
                        y = top + height - strokeWidth / 2
                    ),
                    strokeWidth = 2.dp.toPx()
                )

                // guideline horizontal top
                drawLine(
                    color = animatedColor,
                    start = Offset(
                        x = left + strokeWidth / 2,
                        y = top + height / 3
                    ),
                    end = Offset(
                        x = left + width - strokeWidth / 2,
                        y = top + height / 3
                    ),
                    strokeWidth = 2.dp.toPx()
                )

                // guideline horizontal bottom
                drawLine(
                    color = animatedColor,
                    start = Offset(
                        x = left + strokeWidth / 2,
                        y = top + height - height / 3
                    ),
                    end = Offset(
                        x = left + width - strokeWidth / 2,
                        y = top + height - height / 3
                    ),
                    strokeWidth = 2.dp.toPx()
                )

                val topLeftArc = createCropRectBorderArc(left = left, top = top)
                val topLeftBounds = topLeftArc.getBounds()
                val topLeftCenter = Offset(
                    x = topLeftBounds.left + topLeftBounds.width / 2,
                    y = topLeftBounds.top + topLeftBounds.height / 2
                )
                rotate(-90f, topLeftCenter) {
                    drawPath(
                        color = Color.White.copy(alpha = if (selectedArea == SelectedCropArea.TopLeftCorner) 0.8f else 1f),
                        path = topLeftArc,
                        style = Stroke(
                            width = 6.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                }

                val topRightArc = createCropRectBorderArc(left = left + width, top = top)
                val topRightBounds = topRightArc.getBounds()
                val topRightCenter = Offset(
                    x = topRightBounds.left + topRightBounds.width / 2,
                    y = topRightBounds.top + topRightBounds.height / 2
                )
                rotate(0f, topRightCenter) {
                    drawPath(
                        color = Color.White.copy(alpha = if (selectedArea == SelectedCropArea.TopRightCorner) 0.8f else 1f),
                        path = topRightArc,
                        style = Stroke(
                            width = 6.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                }

                val bottomLeftArc = createCropRectBorderArc(left = left, top = top + height)
                val bottomLeftBounds = bottomLeftArc.getBounds()
                val bottomLeftCenter = Offset(
                    x = bottomLeftBounds.left + bottomLeftBounds.width / 2,
                    y = bottomLeftBounds.top + bottomLeftBounds.height / 2
                )
                rotate(180f, bottomLeftCenter) {
                    drawPath(
                        color = Color.White.copy(alpha = if (selectedArea == SelectedCropArea.BottomLeftCorner) 0.8f else 1f),
                        path = bottomLeftArc,
                        style = Stroke(
                            width = 6.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                }

                val bottomRightArc = createCropRectBorderArc(left = left + width, top = top + height)
                val bottomRightBounds = bottomRightArc.getBounds()
                val bottomRightCenter = Offset(
                    x = bottomRightBounds.left + bottomRightBounds.width / 2,
                    y = bottomRightBounds.top + bottomRightBounds.height / 2
                )
                rotate(90f, bottomRightCenter) {
                    drawPath(
                        color = Color.White.copy(alpha = if (selectedArea == SelectedCropArea.BottomRightCorner) 0.8f else 1f),
                        path = bottomRightArc,
                        style = Stroke(
                            width = 6.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                }
            }
            .pointerInput(Unit) {
                val maxTop = (containerHeight - originalHeight) / 2
                val maxLeft = (containerWidth - originalWidth) / 2

                detectDragGestures(
                    onDrag = { event, offset ->
                        val distanceToTop = abs(top - event.position.y)
                        val distanceToLeft = abs(left - event.position.x)
                        val distanceToBottom = abs((top + height) - event.position.y)
                        val distanceToRight = abs((left + width) - event.position.x)
                        val threshold = 40.dp.toPx()

                        Log.d(TAG, "Distance to top: $distanceToTop, left: $distanceToLeft, right: $distanceToRight, bottom: $distanceToBottom")

                        when {
                            // top left
                            distanceToTop <= threshold && distanceToLeft <= threshold || selectedArea == SelectedCropArea.TopLeftCorner -> {
                                val newTop = top + offset.y
                                val newHeight = height - offset.y

                                if (newTop >= maxTop
                                    && newTop < (newTop + newHeight) - threshold
                                ) {
                                    top = newTop
                                    height = newHeight
                                }

                                val newLeft = left + offset.x
                                val newWidth = width - offset.x
                                if (left + offset.x >= maxLeft
                                    && newLeft < (newLeft + newWidth) - threshold
                                ) {
                                    left = newLeft
                                    width = newWidth
                                }

                                selectedArea = SelectedCropArea.TopLeftCorner
                            }

                            // bottom left
                            distanceToBottom <= threshold && distanceToLeft <= threshold || selectedArea == SelectedCropArea.BottomLeftCorner -> {
                                val newHeight = height + offset.y
                                if (top + newHeight <= (maxTop + originalHeight)
                                    && top + newHeight > threshold
                                ) {
                                    height += offset.y
                                }

                                val newLeft = left + offset.x
                                val newWidth = width - offset.x
                                if (left + offset.x >= maxLeft
                                    && newLeft < (newLeft + newWidth) - threshold
                                ) {
                                    left = newLeft
                                    width = newWidth
                                }

                                selectedArea = SelectedCropArea.BottomLeftCorner
                            }

                            // top right
                            distanceToTop <= threshold && distanceToRight <= threshold || selectedArea == SelectedCropArea.TopRightCorner -> {
                                val newTop = top + offset.y
                                val newHeight = height - offset.y

                                if (newTop >= maxTop
                                    && newTop < (newTop + newHeight) - threshold
                                ) {
                                    top = newTop
                                    height = newHeight
                                }

                                val newWidth = width + offset.x
                                if ((left + width) + offset.x <= (maxLeft + originalWidth)
                                    && left + newWidth > left + threshold
                                ) {
                                    width = newWidth
                                }

                                selectedArea = SelectedCropArea.TopRightCorner
                            }

                            // bottom right
                            distanceToBottom <= threshold && distanceToRight <= threshold || selectedArea == SelectedCropArea.BottomRightCorner -> {
                                val newHeight = height + offset.y
                                if (top + newHeight <= (maxTop + originalHeight)
                                    && top + newHeight > threshold
                                ) {
                                    height += offset.y
                                }

                                val newWidth = width + offset.x
                                if ((left + width) + offset.x <= (maxLeft + originalWidth)
                                    && left + newWidth > left + threshold
                                ) {
                                    width = newWidth
                                }

                                selectedArea = SelectedCropArea.BottomRightCorner
                            }

                            // top edge
                            distanceToTop <= threshold || selectedArea == SelectedCropArea.TopEdge -> {
                                val newTop = top + offset.y
                                val newHeight = height - offset.y

                                if (newTop >= maxTop
                                    && newTop < (newTop + newHeight) - threshold
                                ) {
                                    top = newTop
                                    height = newHeight
                                }

                                selectedArea = SelectedCropArea.TopEdge
                            }

                            // left edge
                            distanceToLeft <= threshold || selectedArea == SelectedCropArea.LeftEdge -> {
                                val newLeft = left + offset.x
                                val newWidth = width - offset.x
                                if (left + offset.x >= maxLeft
                                    && newLeft < (newLeft + newWidth) - threshold
                                ) {
                                    left = newLeft
                                    width = newWidth
                                }

                                selectedArea = SelectedCropArea.LeftEdge
                            }

                            // bottom edge
                            distanceToBottom <= threshold || selectedArea == SelectedCropArea.BottomEdge -> {
                                val newHeight = height + offset.y
                                if (top + newHeight <= (maxTop + originalHeight)
                                    && top + newHeight > threshold
                                ) {
                                    height += offset.y
                                }

                                selectedArea = SelectedCropArea.BottomEdge
                            }

                            // right edge
                            distanceToRight <= threshold || selectedArea == SelectedCropArea.RightEdge -> {
                                val newWidth = width + offset.x
                                if ((left + width) + offset.x <= (maxLeft + originalWidth)
                                    && left + newWidth > left + threshold
                                ) {
                                    width = newWidth
                                }

                                selectedArea = SelectedCropArea.RightEdge
                            }
                        }
                    },
                    onDragEnd = {
                        selectedArea = SelectedCropArea.None
                    }
                )
            }
    )
}