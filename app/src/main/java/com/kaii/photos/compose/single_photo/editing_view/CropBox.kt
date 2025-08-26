package com.kaii.photos.compose.single_photo.editing_view

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.kaii.photos.helpers.AnimationConstants
import kotlin.math.abs

@Composable
fun CropBox(
    containerWidth: Float,
    containerHeight: Float,
    mediaAspectRatio: Float,
    aspectRatio: MutableState<CroppingAspectRatio>,
    modifier: Modifier = Modifier,
    reset: MutableState<Boolean>,
    onAreaChanged: (area: Rect, original: Size) -> Unit
) {
    val containerAspectRatio = remember {
        containerWidth / containerHeight
    }

    var originalWidth by remember {
        mutableFloatStateOf(
            if (containerAspectRatio > mediaAspectRatio) {
                containerHeight * mediaAspectRatio
            } else {
                containerWidth
            }
        )
    }
    var originalHeight by remember {
        mutableFloatStateOf(
            if (containerAspectRatio > mediaAspectRatio) {
                containerHeight
            } else {
                containerWidth / mediaAspectRatio
            }
        )
    }

    var width by remember {
        mutableFloatStateOf(
            originalWidth
        )
    }
    var height by remember {
        mutableFloatStateOf(
            originalHeight
        )
    }
    var top by remember {
        mutableFloatStateOf(
            (containerHeight - height) / 2
        )
    }
    var left by remember {
        mutableFloatStateOf(
            (containerWidth - width) / 2
        )
    }

    LaunchedEffect(mediaAspectRatio, reset.value, containerWidth, containerHeight) {
        originalWidth =
            if (containerAspectRatio > mediaAspectRatio) {
                containerHeight * mediaAspectRatio
            } else {
                containerWidth
            }
        originalHeight =
            if (containerAspectRatio > mediaAspectRatio) {
                containerHeight
            } else {
                containerWidth / mediaAspectRatio
            }

        width = originalWidth
        height = originalHeight

        top = (containerHeight - height) / 2
        left = (containerWidth - width) / 2

        reset.value = false
    }

    LaunchedEffect(top, left, width, height, originalWidth, originalHeight) {
        onAreaChanged(
            Rect(
                top = top - ((containerHeight - originalHeight) / 2),
                left = left - ((containerWidth - originalWidth) / 2),
                bottom = top + height - ((containerHeight - originalHeight) / 2),
                right = left + width - ((containerWidth - originalWidth) / 2)
            ),
            Size(
                width = originalWidth,
                height = originalHeight
            )
        )
    }

    LaunchedEffect(aspectRatio.value) {
        if (aspectRatio.value == CroppingAspectRatio.FreeForm) return@LaunchedEffect

        if (aspectRatio.value.ratio > mediaAspectRatio) {
            if (top + height * width / aspectRatio.value.ratio <= originalHeight) {
                width = height * aspectRatio.value.ratio
            } else {
                height = width / aspectRatio.value.ratio
            }
        } else {
            if (left + width * height * aspectRatio.value.ratio <= originalHeight) {
                height = width / aspectRatio.value.ratio
            } else {
                width = height * aspectRatio.value.ratio
            }
        }
    }

    var selectedArea by remember { mutableStateOf(SelectedCropArea.None) }

    val animatedColor by animateColorAsState(
        targetValue = Color.White.copy(alpha = if (selectedArea == SelectedCropArea.None) 0f else 0.6f),
        animationSpec = tween(
            durationMillis = AnimationConstants.DURATION
        )
    )

    // shading box (gives cutout effect)
    Box(
        modifier = modifier
            .size(
                width = containerWidth.dp,
                height = containerHeight.dp
            )
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()

                val strokeWidth = 4.dp.toPx()

                drawRect(
                    color = Color.Black.copy(alpha = 0.6f),
                    topLeft = Offset(((containerWidth - originalWidth) / 2), (containerHeight - originalHeight) / 2),
                    size = Size(originalWidth, originalHeight)
                )

                drawRect(
                    color = Color.White,
                    topLeft = Offset(left + strokeWidth / 2, top + strokeWidth / 2),
                    size = Size(width - strokeWidth, height - strokeWidth),
                    blendMode = BlendMode.DstOut
                )
            }
    )

    // CropBox itself
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
                    color = Color.White.copy(alpha = 0.5f)
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

                        when {
                            // top left
                            (distanceToTop <= threshold && selectedArea == SelectedCropArea.None) && distanceToLeft <= threshold || selectedArea == SelectedCropArea.TopLeftCorner -> {
                                if (aspectRatio.value == CroppingAspectRatio.FreeForm) {
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
                                } else {
                                    if (abs(offset.x) >= abs(offset.y)) {
                                        val newLeft = left + offset.x
                                        val newWidth = width - offset.x

                                        val newTop = top + offset.x / aspectRatio.value.ratio
                                        val newHeight = newWidth / aspectRatio.value.ratio

                                        val maxRight = (newLeft + newWidth) - threshold
                                        val maxBottom = (newTop + newHeight) - threshold

                                        if (newLeft >= maxLeft && newLeft <= maxRight && newTop >= maxTop && newTop <= maxBottom) {
                                            left = newLeft
                                            width = newWidth
                                            top = newTop
                                            height = newHeight
                                        }
                                    } else {
                                        val newTop = top + offset.y
                                        val newHeight = height - offset.y

                                        val newLeft = left + offset.y * aspectRatio.value.ratio
                                        val newWidth = newHeight * aspectRatio.value.ratio

                                        val maxRight = (newLeft + newWidth) - threshold
                                        val maxBottom = (newTop + newHeight) - threshold

                                        if (newLeft >= maxLeft && newLeft <= maxRight && newTop >= maxTop && newTop <= maxBottom) {
                                            left = newLeft
                                            width = newWidth
                                            top = newTop
                                            height = newHeight
                                        }
                                    }
                                }

                                selectedArea = SelectedCropArea.TopLeftCorner
                            }

                            // bottom left
                            (distanceToBottom <= threshold && selectedArea == SelectedCropArea.None) && distanceToLeft <= threshold || selectedArea == SelectedCropArea.BottomLeftCorner -> {
                                if (aspectRatio.value == CroppingAspectRatio.FreeForm) {
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
                                } else {
                                    if (abs(offset.x) >= abs(offset.y)) {
                                        val newLeft = left + offset.x
                                        val newWidth = width - offset.x

                                        val newHeight = newWidth / aspectRatio.value.ratio

                                        val maxRight = (newLeft + newWidth) - threshold
                                        val maxBottom = (top + newHeight) - threshold

                                        if (newLeft >= maxLeft && newLeft <= maxRight && top + newHeight <= maxTop + originalHeight && top <= maxBottom) {
                                            left = newLeft
                                            width = newWidth
                                            height = newHeight
                                        }
                                    } else {
                                        val newHeight = height + offset.y

                                        val newWidth = newHeight * aspectRatio.value.ratio
                                        val newLeft = left - offset.y * aspectRatio.value.ratio

                                        val maxRight = (newLeft + newWidth) - threshold
                                        val maxBottom = (top + newHeight) - threshold

                                        if (newLeft >= maxLeft && newLeft <= maxRight && top <= maxBottom && top + newHeight <= maxTop + originalHeight) {
                                            left = newLeft
                                            width = newWidth
                                            height = newHeight
                                        }
                                    }
                                }

                                selectedArea = SelectedCropArea.BottomLeftCorner
                            }

                            // top right
                            (distanceToTop <= threshold && selectedArea == SelectedCropArea.None) && distanceToRight <= threshold || selectedArea == SelectedCropArea.TopRightCorner -> {
                                if (aspectRatio.value == CroppingAspectRatio.FreeForm) {
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
                                } else {
                                    if (abs(offset.x) >= abs(offset.y)) {
                                        val newWidth = width + offset.x

                                        val newHeight = newWidth / aspectRatio.value.ratio
                                        val newTop = (top + height) - newHeight

                                        val maxRight = (left + newWidth) - threshold
                                        val maxBottom = (top + newHeight) - threshold

                                        if (top >= maxTop && top <= maxBottom && left + newWidth <= maxLeft + originalWidth && left <= maxRight) {
                                            top = newTop
                                            width = newWidth
                                            height = newHeight
                                        }
                                    } else {
                                        val newHeight = height - offset.y

                                        val newWidth = newHeight * aspectRatio.value.ratio
                                        val newTop = (top + height) - newHeight

                                        val maxRight = (left + newWidth) - threshold
                                        val maxBottom = (top + newHeight) - threshold

                                        if (left + newWidth <= maxLeft + originalWidth && left <= maxRight && newTop <= maxBottom && newTop >= maxTop) {
                                            top = newTop
                                            width = newWidth
                                            height = newHeight
                                        }
                                    }
                                }

                                selectedArea = SelectedCropArea.TopRightCorner
                            }

                            // bottom right
                            (distanceToBottom <= threshold && selectedArea == SelectedCropArea.None) && distanceToRight <= threshold || selectedArea == SelectedCropArea.BottomRightCorner -> {
                                if (aspectRatio.value == CroppingAspectRatio.FreeForm) {
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
                                } else {
                                    if (abs(offset.x) >= abs(offset.y)) {
                                        val newWidth = width + offset.x
                                        val newHeight = newWidth / aspectRatio.value.ratio

                                        val maxRight = (left + newWidth) - threshold
                                        val maxBottom = (top + newHeight) - threshold

                                        if (top + newHeight <= maxTop + originalHeight && top <= maxBottom && left + newWidth <= maxLeft + originalWidth && left <= maxRight) {
                                            width = newWidth
                                            height = newHeight
                                        }
                                    } else {
                                        val newHeight = height + offset.y
                                        val newWidth = newHeight * aspectRatio.value.ratio

                                        val maxRight = (left + newWidth) - threshold
                                        val maxBottom = (top + newHeight) - threshold

                                        if (top + newHeight <= maxTop + originalHeight && top <= maxBottom && left + newWidth <= maxLeft + originalWidth && left <= maxRight) {
                                            width = newWidth
                                            height = newHeight
                                        }
                                    }
                                }

                                selectedArea = SelectedCropArea.BottomRightCorner
                            }

                            // top edge
                            (distanceToTop <= threshold && selectedArea == SelectedCropArea.None) || selectedArea == SelectedCropArea.TopEdge -> {
                                val newTop = top + offset.y
                                val newHeight = height - offset.y

                                if (aspectRatio.value == CroppingAspectRatio.FreeForm) {
                                    if (newTop >= maxTop
                                        && newTop < (newTop + newHeight) - threshold
                                    ) {
                                        top = newTop
                                        height = newHeight
                                    }
                                } else {
                                    val newWidth = newHeight * aspectRatio.value.ratio

                                    val maxBottom = newTop + newHeight - threshold
                                    val maxRight = left + newWidth - threshold

                                    val newLeft = left - (newWidth - width) / 2
                                    val newTop = top - (newHeight - height) / 2

                                    if (newTop >= maxTop
                                        && newTop <= maxBottom
                                        && newTop + newHeight <= maxTop + originalHeight
                                        && newLeft >= maxLeft
                                        && newLeft <= maxRight
                                        && newLeft + newWidth <= maxLeft + originalWidth
                                    ) {
                                        left = newLeft
                                        top = newTop
                                        height = newHeight
                                        width = newWidth
                                    }
                                }

                                selectedArea = SelectedCropArea.TopEdge
                            }

                            // left edge
                            (distanceToLeft <= threshold && selectedArea == SelectedCropArea.None) || selectedArea == SelectedCropArea.LeftEdge -> {
                                val newLeft = left + offset.x
                                val newWidth = width - offset.x

                                if (aspectRatio.value == CroppingAspectRatio.FreeForm) {
                                    if (left + offset.x >= maxLeft
                                        && newLeft < (newLeft + newWidth) - threshold
                                    ) {
                                        left = newLeft
                                        width = newWidth
                                    }
                                } else {
                                    val newHeight = newWidth / aspectRatio.value.ratio

                                    val maxBottom = top + newHeight - threshold
                                    val maxRight = newLeft + newWidth - threshold

                                    val newLeft = left - (newWidth - width) / 2
                                    val newTop = top - (newHeight - height) / 2

                                    if (newTop >= maxTop
                                        && newTop <= maxBottom
                                        && newTop + newHeight <= maxTop + originalHeight
                                        && newLeft >= maxLeft
                                        && newLeft <= maxRight
                                        && newLeft + newWidth <= maxLeft + originalWidth
                                    ) {
                                        left = newLeft
                                        top = newTop
                                        height = newHeight
                                        width = newWidth
                                    }
                                }

                                selectedArea = SelectedCropArea.LeftEdge
                            }

                            // bottom edge
                            (distanceToBottom <= threshold && selectedArea == SelectedCropArea.None) || selectedArea == SelectedCropArea.BottomEdge -> {
                                val newHeight = height + offset.y

                                if (aspectRatio.value == CroppingAspectRatio.FreeForm) {
                                    if (top + newHeight <= (maxTop + originalHeight)
                                        && top + newHeight > threshold
                                    ) {
                                        height += offset.y
                                    }
                                } else {
                                    val newWidth = newHeight * aspectRatio.value.ratio

                                    val maxBottom = top + newHeight - threshold
                                    val maxRight = left + newWidth - threshold

                                    val newLeft = left - (newWidth - width) / 2
                                    val newTop = top - (newHeight - height) / 2

                                    if (newTop >= maxTop
                                        && newTop <= maxBottom
                                        && newTop + newHeight <= maxTop + originalHeight
                                        && newLeft >= maxLeft
                                        && newLeft <= maxRight
                                        && newLeft + newWidth <= maxLeft + originalWidth
                                    ) {
                                        left = newLeft
                                        top = newTop
                                        height = newHeight
                                        width = newWidth
                                    }
                                }

                                selectedArea = SelectedCropArea.BottomEdge
                            }

                            // right edge
                            (distanceToRight <= threshold && selectedArea == SelectedCropArea.None) || selectedArea == SelectedCropArea.RightEdge -> {
                                val newWidth = width + offset.x

                                if (aspectRatio.value == CroppingAspectRatio.FreeForm) {
                                    if ((left + width) + offset.x <= (maxLeft + originalWidth)
                                        && left + newWidth > left + threshold
                                    ) {
                                        width = newWidth
                                    }
                                } else {
                                    val newHeight = newWidth / aspectRatio.value.ratio

                                    val maxBottom = top + newHeight - threshold
                                    val maxRight = left + newWidth - threshold

                                    val newLeft = left - (newWidth - width) / 2
                                    val newTop = top - (newHeight - height) / 2

                                    if (newTop >= maxTop
                                        && newTop <= maxBottom
                                        && newTop + newHeight <= maxTop + originalHeight
                                        && newLeft >= maxLeft
                                        && newLeft <= maxRight
                                        && newLeft + newWidth <= maxLeft + originalWidth
                                    ) {
                                        left = newLeft
                                        top = newTop
                                        height = newHeight
                                        width = newWidth
                                    }
                                }

                                selectedArea = SelectedCropArea.RightEdge
                            }

                            // center drag (whole crop box)
                            (event.position.y in top..top+height && event.position.x in left..left+width) || selectedArea == SelectedCropArea.Whole -> {
                                if (left + offset.x >= maxLeft
                                    && left + offset.x + width <= maxLeft + originalWidth
                                ) {
                                    left = left + offset.x
                                }

                                if (top + offset.y >= maxTop
                                    && top + offset.y + height <= maxTop + originalHeight
                                ) {
                                    top = top + offset.y
                                }

                                selectedArea = SelectedCropArea.Whole
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