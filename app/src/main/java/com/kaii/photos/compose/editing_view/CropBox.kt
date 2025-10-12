package com.kaii.photos.compose.editing_view

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.editing.CroppingAspectRatio
import com.kaii.photos.helpers.editing.MediaEditingState
import com.kaii.photos.helpers.editing.SelectedCropArea
import kotlin.math.abs

@Composable
fun CropBox(
    containerWidth: Float,
    containerHeight: Float,
    mediaAspectRatio: Float,
    editingState: MediaEditingState,
    scale: Float,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onAreaChanged: (area: Rect, original: Size) -> Unit,
    onCropDone: () -> Unit
) {
    val containerAspectRatio = rememberSaveable(containerWidth, containerHeight) {
        containerWidth / containerHeight
    }

    var originalWidth by rememberSaveable {
        mutableFloatStateOf(
            if (containerAspectRatio > mediaAspectRatio) {
                containerHeight * mediaAspectRatio
            } else {
                containerWidth
            }
        )
    }
    var originalHeight by rememberSaveable {
        mutableFloatStateOf(
            if (containerAspectRatio > mediaAspectRatio) {
                containerHeight
            } else {
                containerWidth / mediaAspectRatio
            }
        )
    }

    var width by rememberSaveable {
        mutableFloatStateOf(
            originalWidth
        )
    }
    var height by rememberSaveable {
        mutableFloatStateOf(
            originalHeight
        )
    }
    var top by rememberSaveable {
        mutableFloatStateOf(
            (containerHeight - height) / 2
        )
    }
    var left by rememberSaveable {
        mutableFloatStateOf(
            (containerWidth - width) / 2
        )
    }

    LaunchedEffect(mediaAspectRatio, editingState.resetCrop, containerWidth, containerHeight) {
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

        editingState.resetCrop(false)
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

    LaunchedEffect(editingState.croppingAspectRatio) {
        if (editingState.croppingAspectRatio == CroppingAspectRatio.FreeForm) return@LaunchedEffect

        if (editingState.croppingAspectRatio.ratio > mediaAspectRatio) {
            if (top + height * width / editingState.croppingAspectRatio.ratio <= originalHeight) {
                width = height * editingState.croppingAspectRatio.ratio
            } else {
                height = width / editingState.croppingAspectRatio.ratio
            }
        } else {
            if (left + width * height * editingState.croppingAspectRatio.ratio <= originalHeight) {
                height = width / editingState.croppingAspectRatio.ratio
            } else {
                width = height * editingState.croppingAspectRatio.ratio
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

    val localDensity = LocalDensity.current
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0f,
        animationSpec = AnimationConstants.expressiveTween(
            durationMillis = AnimationConstants.DURATION
        )
    )

    Box(
        modifier = Modifier
            .alpha(alpha)
            .background(Color.Transparent)
            .requiredSize(
                width = with(localDensity) { containerWidth.toDp() + 32.dp },
                height = with(localDensity) { containerHeight.toDp() + 32.dp }
            )
    ) {
        // shading box (gives cutout effect)
        Box(
            modifier = modifier
                .offset(x = 16.dp, y = 16.dp)
                .size(
                    width = containerWidth.dp,
                    height = containerHeight.dp
                )
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()

                    val strokeWidth = 4.dp.toPx() * (1f / scale)

                    drawRect(
                        color = Color.Black.copy(alpha = 0.75f),
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
                .offset(x = 16.dp, y = 16.dp)
                .size(
                    width = containerWidth.dp,
                    height = containerHeight.dp
                )
                .drawWithContent {
                    drawContent()

                    val strokeWidth = 4.dp.toPx() * (1f / scale)
                    val cornerRadius = 2.dp.toPx() * (1f / scale)

                    drawOutline(
                        outline = Outline.Rounded(
                            roundRect = RoundRect(
                                left = left,
                                top = top,
                                right = left + width,
                                bottom = top + height,
                                cornerRadius = CornerRadius(x = cornerRadius, y = cornerRadius)
                            )
                        ),
                        style = Stroke(
                            width = strokeWidth
                        ),
                        color = Color.White.copy(alpha = 0.5f)
                    )

                    val guidelineStrokeWidth = 2.dp.toPx() * (1f / scale)
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
                        strokeWidth = guidelineStrokeWidth
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
                        strokeWidth = guidelineStrokeWidth
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
                        strokeWidth = guidelineStrokeWidth
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
                        strokeWidth = guidelineStrokeWidth
                    )

                    val arcStrokeWidth = 6.dp.toPx() * (1f / scale)
                    val topLeftArc = createCropRectBorderArc(left = left, top = top, scale = scale)
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
                                width = arcStrokeWidth,
                                cap = StrokeCap.Round
                            )
                        )
                    }

                    val topRightArc = createCropRectBorderArc(left = left + width, top = top, scale = scale)
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
                                width = arcStrokeWidth,
                                cap = StrokeCap.Round
                            )
                        )
                    }

                    val bottomLeftArc = createCropRectBorderArc(left = left, top = top + height, scale = scale)
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
                                width = arcStrokeWidth,
                                cap = StrokeCap.Round
                            )
                        )
                    }

                    val bottomRightArc = createCropRectBorderArc(left = left + width, top = top + height, scale = scale)
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
                                width = arcStrokeWidth,
                                cap = StrokeCap.Round
                            )
                        )
                    }
                }
                .then(
                    if (enabled) {
                        Modifier.pointerInput(scale) {
                            val maxTop = (containerHeight - originalHeight) / 2
                            val maxLeft = (containerWidth - originalWidth) / 2

                            detectDragGestures(
                                onDrag = { event, offset ->
                                    val distanceToTop = abs(top - event.position.y)
                                    val distanceToLeft = abs(left - event.position.x)
                                    val distanceToBottom = abs((top + height) - event.position.y)
                                    val distanceToRight = abs((left + width) - event.position.x)
                                    val threshold = 56.dp.toPx()

                                    when {
                                        // center drag (whole crop box)
                                        (event.position.y in top + height / 3..top + height * 2 / 3 && event.position.x in left + width / 3..left + width * 2 / 3) || selectedArea == SelectedCropArea.Whole -> {
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

                                        // top left
                                        ((distanceToTop <= threshold && distanceToTop <= distanceToBottom)
                                                && (distanceToLeft <= threshold && distanceToLeft <= distanceToRight)
                                                && selectedArea == SelectedCropArea.None)
                                                || selectedArea == SelectedCropArea.TopLeftCorner -> {
                                            if (editingState.croppingAspectRatio == CroppingAspectRatio.FreeForm) {
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

                                                    val newTop = top + offset.x / editingState.croppingAspectRatio.ratio
                                                    val newHeight = newWidth / editingState.croppingAspectRatio.ratio

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

                                                    val newLeft = left + offset.y * editingState.croppingAspectRatio.ratio
                                                    val newWidth = newHeight * editingState.croppingAspectRatio.ratio

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
                                        ((distanceToBottom <= threshold && distanceToBottom <= distanceToTop)
                                                && (distanceToLeft <= threshold && distanceToLeft <= distanceToRight)
                                                && selectedArea == SelectedCropArea.None)
                                                || selectedArea == SelectedCropArea.BottomLeftCorner -> {
                                            if (editingState.croppingAspectRatio == CroppingAspectRatio.FreeForm) {
                                                val newHeight = height + offset.y
                                                if (top + newHeight <= (maxTop + originalHeight)
                                                    && newHeight > threshold
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

                                                    val newHeight = newWidth / editingState.croppingAspectRatio.ratio

                                                    val maxRight = (newLeft + newWidth) - threshold
                                                    val maxBottom = (top + newHeight) - threshold

                                                    if (newLeft >= maxLeft && newLeft <= maxRight && top + newHeight <= maxTop + originalHeight && top <= maxBottom) {
                                                        left = newLeft
                                                        width = newWidth
                                                        height = newHeight
                                                    }
                                                } else {
                                                    val newHeight = height + offset.y

                                                    val newWidth = newHeight * editingState.croppingAspectRatio.ratio
                                                    val newLeft = left - offset.y * editingState.croppingAspectRatio.ratio

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
                                        ((distanceToTop <= threshold && distanceToTop <= distanceToBottom)
                                                && (distanceToRight <= threshold && distanceToRight <= distanceToLeft)
                                                && selectedArea == SelectedCropArea.None)
                                                || selectedArea == SelectedCropArea.TopRightCorner -> {
                                            if (editingState.croppingAspectRatio == CroppingAspectRatio.FreeForm) {
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

                                                    val newHeight = newWidth / editingState.croppingAspectRatio.ratio
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

                                                    val newWidth = newHeight * editingState.croppingAspectRatio.ratio
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
                                        ((distanceToBottom <= threshold && distanceToBottom <= distanceToTop)
                                                && (distanceToRight <= threshold && distanceToRight <= distanceToLeft)
                                                && selectedArea == SelectedCropArea.None)
                                                || selectedArea == SelectedCropArea.BottomRightCorner -> {
                                            if (editingState.croppingAspectRatio == CroppingAspectRatio.FreeForm) {
                                                val newHeight = height + offset.y
                                                if (top + newHeight <= (maxTop + originalHeight)
                                                    && newHeight > threshold
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
                                                    val newHeight = newWidth / editingState.croppingAspectRatio.ratio

                                                    val maxRight = (left + newWidth) - threshold
                                                    val maxBottom = (top + newHeight) - threshold

                                                    if (top + newHeight <= maxTop + originalHeight && top <= maxBottom && left + newWidth <= maxLeft + originalWidth && left <= maxRight) {
                                                        width = newWidth
                                                        height = newHeight
                                                    }
                                                } else {
                                                    val newHeight = height + offset.y
                                                    val newWidth = newHeight * editingState.croppingAspectRatio.ratio

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
                                        ((distanceToTop <= threshold && distanceToTop <= distanceToBottom)
                                                && selectedArea == SelectedCropArea.None)
                                                || selectedArea == SelectedCropArea.TopEdge -> {
                                            val newTop = top + offset.y
                                            val newHeight = height - offset.y

                                            if (editingState.croppingAspectRatio == CroppingAspectRatio.FreeForm) {
                                                if (newTop >= maxTop
                                                    && newTop < (newTop + newHeight) - threshold
                                                ) {
                                                    top = newTop
                                                    height = newHeight
                                                }
                                            } else {
                                                val newWidth = newHeight * editingState.croppingAspectRatio.ratio

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
                                        ((distanceToLeft <= threshold && distanceToLeft <= distanceToRight)
                                                && selectedArea == SelectedCropArea.None)
                                                || selectedArea == SelectedCropArea.LeftEdge -> {
                                            val newLeft = left + offset.x
                                            val newWidth = width - offset.x

                                            if (editingState.croppingAspectRatio == CroppingAspectRatio.FreeForm) {
                                                if (left + offset.x >= maxLeft
                                                    && newLeft < (newLeft + newWidth) - threshold
                                                ) {
                                                    left = newLeft
                                                    width = newWidth
                                                }
                                            } else {
                                                val newHeight = newWidth / editingState.croppingAspectRatio.ratio

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
                                        ((distanceToBottom <= threshold && distanceToBottom <= distanceToTop)
                                                && selectedArea == SelectedCropArea.None)
                                                || selectedArea == SelectedCropArea.BottomEdge -> {
                                            val newHeight = height + offset.y

                                            if (editingState.croppingAspectRatio == CroppingAspectRatio.FreeForm) {
                                                if (top + newHeight <= (maxTop + originalHeight)
                                                    && top + newHeight > threshold
                                                ) {
                                                    height += offset.y
                                                }
                                            } else {
                                                val newWidth = newHeight * editingState.croppingAspectRatio.ratio

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
                                        ((distanceToRight <= threshold && distanceToRight <= distanceToLeft)
                                                && selectedArea == SelectedCropArea.None)
                                                || selectedArea == SelectedCropArea.RightEdge -> {
                                            val newWidth = width + offset.x

                                            if (editingState.croppingAspectRatio == CroppingAspectRatio.FreeForm) {
                                                if ((left + width) + offset.x <= (maxLeft + originalWidth)
                                                    && left + newWidth > left + threshold
                                                ) {
                                                    width = newWidth
                                                }
                                            } else {
                                                val newHeight = newWidth / editingState.croppingAspectRatio.ratio

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
                                    }
                                },
                                onDragEnd = {
                                    selectedArea = SelectedCropArea.None
                                    onCropDone()
                                }
                            )
                        }
                    } else Modifier
                )
        )
    }

}

fun DrawScope.createCropRectBorderArc(
    left: Float,
    top: Float,
    scale: Float
) = Path().apply {
    val radius = 16.dp.toPx() * (1f / scale)
    moveTo(x = left - radius / 2, y = top - radius / 2)

    lineTo(x = left, y = top - radius / 2)

    arcTo(
        rect = Rect(
            left = left - radius / 2,
            top = top - radius / 2,
            right = left + radius / 2,
            bottom = top + radius / 2
        ),
        startAngleDegrees = 270f,
        sweepAngleDegrees = 90f,
        forceMoveTo = false
    )

    lineTo(x = left + radius / 2, y = top + radius / 2)
}

