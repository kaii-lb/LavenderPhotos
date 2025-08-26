package com.kaii.photos.compose.single_photo.editing_view

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import com.kaii.photos.R
import com.kaii.photos.compose.CroppingRatioBottomSheet
import com.kaii.photos.compose.dialogs.SliderDialog
import com.kaii.photos.compose.single_photo.EditingViewBottomAppBarItem
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.VideoPlayerConstants
import kotlin.math.truncate

// private const val TAG = "VIDEO_EDITOR_TAB_CONTENT"

@Composable
fun VideoEditorTrimContent(
    currentPosition: MutableFloatState,
    basicData: BasicVideoData,
    leftPosition: MutableFloatState,
    rightPosition: MutableFloatState,
    thumbnails: List<Bitmap>,
    onSeek: (Float) -> Unit,
) {
    val actualDuration by remember {
        derivedStateOf {
            rightPosition.floatValue - leftPosition.floatValue
        }
    }

    val duration by rememberUpdatedState(basicData.duration)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize(1f)
            .clip(RoundedCornerShape(16.dp))
    ) {
        val localDensity = LocalDensity.current
        var isDraggingManually by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize(1f)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 22.dp, vertical = 6.dp)
        ) {
            // shows video thumbnails
            Row(
                modifier = Modifier
                    .fillMaxSize(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceBright),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                thumbnails.forEach {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "thumbnail image for video",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxHeight(1f)
                            .width(this@BoxWithConstraints.maxWidth / VideoPlayerConstants.TRIM_THUMBNAIL_COUNT - (22.dp / VideoPlayerConstants.TRIM_THUMBNAIL_COUNT.toFloat()))
                    )
                }
            }

            BoxWithConstraints boxybox@{
                val leftHandlePosition by remember {
                    derivedStateOf {
                        this@boxybox.maxWidth * leftPosition.floatValue / duration
                    }
                }
                val rightHandlePosition by remember {
                    derivedStateOf {
                        this@boxybox.maxWidth * rightPosition.floatValue / duration
                    }
                }
                val seekbarWidth by remember {
                    derivedStateOf {
                        rightHandlePosition - leftHandlePosition - 14.dp
                    }
                }

                val seekHandlePosition by remember {
                    derivedStateOf {
                        leftHandlePosition + seekbarWidth * (currentPosition.floatValue - leftPosition.floatValue) / actualDuration
                    }
                }

                val videoPositionDraggableState = rememberDraggableState { change ->
                    with(localDensity) {
                        val new = currentPosition.floatValue + (change * actualDuration / seekbarWidth.toPx())
                        currentPosition.floatValue = new.coerceIn(leftPosition.floatValue, rightPosition.floatValue)
                        onSeek(currentPosition.floatValue)
                    }
                }

                val animatedSeekOffset by animateDpAsState(
                    targetValue = if (seekHandlePosition.isUnspecified) 0.dp else seekHandlePosition,
                    animationSpec = tween(
                        durationMillis = if (!isDraggingManually) AnimationConstants.DURATION else 0,
                        easing = LinearEasing
                    )
                )
                // seek handle
                Box(
                    modifier = Modifier
                        .offset(x = animatedSeekOffset + 4.dp, y = 4.dp)
                        .width(6.dp)
                        .height(this@boxybox.maxHeight - 8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface)
                        .draggable(
                            state = videoPositionDraggableState,
                            orientation = Orientation.Horizontal,
                            onDragStarted = {
                                isDraggingManually = true
                            },
                            onDragStopped = {
                                isDraggingManually = false
                            }
                        )
                )

                val leftDraggableState = rememberDraggableState { change ->
                    with(localDensity) {
                        val new = leftPosition.floatValue + (change * duration / this@boxybox.maxWidth.toPx())
                        leftPosition.floatValue = new.coerceIn(0f, rightPosition.floatValue - (duration * 0.2f))

                        currentPosition.floatValue = leftPosition.floatValue
                    }
                }
                val animatedLeftHandlePos by animateDpAsState(
                    targetValue = if (leftHandlePosition.isUnspecified) 0.dp else leftHandlePosition,
                    animationSpec = AnimationConstants.expressiveTween(
                        durationMillis = if (isDraggingManually) 0 else AnimationConstants.DURATION
                    )
                )
                LeftHandle(
                    handlePosition = animatedLeftHandlePos,
                    height = this@BoxWithConstraints.maxHeight,
                    draggableState = leftDraggableState,
                    onDragStarted = {
                        isDraggingManually = true
                    },
                    onDragStopped = {
                        isDraggingManually = false
                        currentPosition.floatValue = leftPosition.floatValue
                        onSeek(currentPosition.floatValue)
                    }
                )

                val rightDraggableState = rememberDraggableState { change ->
                    with(localDensity) {
                        val new = rightPosition.floatValue + (change * duration / this@boxybox.maxWidth.toPx())
                        rightPosition.floatValue = new.coerceIn(leftPosition.floatValue + (duration * 0.1f), duration)

                        currentPosition.floatValue = rightPosition.floatValue
                    }
                }
                val animatedRightHandlePos by animateDpAsState(
                    targetValue = if (leftHandlePosition.isUnspecified) 32.dp else rightHandlePosition,
                    animationSpec = AnimationConstants.expressiveTween(
                        durationMillis = if (isDraggingManually) 0 else AnimationConstants.DURATION
                    )
                )
                RightHandle(
                    handlePosition = animatedRightHandlePos,
                    draggableState = rightDraggableState,
                    height = this@BoxWithConstraints.maxHeight,
                    onDragStarted = {
                        isDraggingManually = true
                    },
                    onDragStopped = {
                        isDraggingManually = false
                        currentPosition.floatValue = rightPosition.floatValue
                        onSeek(currentPosition.floatValue)
                    }
                )

                // connector between two handles and gives "inverted rounding" on inside
                Box(
                    modifier = Modifier
                        .offset(x = animatedLeftHandlePos)
                        .width(animatedRightHandlePos - animatedLeftHandlePos)
                        .requiredHeight(this@BoxWithConstraints.maxHeight)
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                        .drawWithContent {
                            drawContent()

                            drawRoundRect(
                                color = Color.White,
                                topLeft = with(localDensity) {
                                    Offset(
                                        x = 0f,
                                        y = 6.dp.toPx()
                                    )
                                },
                                size = with(localDensity) {
                                    Size(
                                        width = (animatedRightHandlePos - animatedLeftHandlePos).toPx(),
                                        height = (this@BoxWithConstraints.maxHeight - 12.dp).toPx()
                                    )
                                },
                                cornerRadius = with(localDensity) {
                                    CornerRadius(8.dp.toPx(), 8.dp.toPx())
                                },
                                blendMode = BlendMode.DstOut,
                            )
                        }
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
private fun LeftHandle(
    handlePosition: Dp,
    height: Dp,
    draggableState: DraggableState,
    modifier: Modifier = Modifier,
    onDragStarted: () -> Unit,
    onDragStopped: () -> Unit
) {
    Box(
        modifier = modifier
            .offset(x = handlePosition - 22.dp)
            .requiredHeight(height)
            .width(22.dp)
            .clip(
                RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 0.dp,
                    bottomStart = 20.dp,
                    bottomEnd = 0.dp
                )
            )
            .background(MaterialTheme.colorScheme.primary)
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal,
                onDragStarted = { onDragStarted() },
                onDragStopped = { onDragStopped() }
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight(0.6f)
                .width(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onPrimary)
        )
    }
}

@Composable
private fun RightHandle(
    handlePosition: Dp,
    height: Dp,
    draggableState: DraggableState,
    modifier: Modifier = Modifier,
    onDragStarted: () -> Unit,
    onDragStopped: () -> Unit
) {
    Box(
        modifier = modifier
            .offset(x = handlePosition)
            .requiredHeight(height)
            .width(24.dp)
            .clip(
                RoundedCornerShape(
                    topStart = 0.dp,
                    topEnd = 20.dp,
                    bottomStart = 0.dp,
                    bottomEnd = 20.dp
                )
            )
            .background(MaterialTheme.colorScheme.primary)
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal,
                onDragStarted = { onDragStarted() },
                onDragStopped = { onDragStopped() }
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight(0.6f)
                .width(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onPrimary)
        )
    }
}

@Composable
fun VideoEditorCropContent(
    imageAspectRatio: Float,
    croppingAspectRatio: MutableState<CroppingAspectRatio>,
    onReset: () -> Unit,
    onRotate: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        EditingViewBottomAppBarItem(
            text = stringResource(id = R.string.editing_rotate),
            icon = R.drawable.rotate_ccw,
            onClick = onRotate
        )

        val showSheet = remember { mutableStateOf(false) }

        CroppingRatioBottomSheet(
            show = showSheet,
            ratio = croppingAspectRatio.value,
            originalImageRatio = imageAspectRatio,
            onSetCroppingRatio = {
                croppingAspectRatio.value = it
            }
        )

        EditingViewBottomAppBarItem(
            text = stringResource(id = R.string.editing_ratio),
            icon = R.drawable.resolution,
            onClick = {
                showSheet.value = true
            }
        )

        EditingViewBottomAppBarItem(
            text = stringResource(id = R.string.editing_reset),
            icon = R.drawable.reset,
            onClick = onReset
        )
    }
}

@Composable
fun VideoEditorAdjustContent(
    basicData: BasicVideoData,
    modifications: SnapshotStateList<VideoModification>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        var showVolumeDialog by remember { mutableStateOf(false) }
        val resources = LocalResources.current

        if (showVolumeDialog) {
            SliderDialog(
                steps = 200,
                range = 0f..200f,
                startsAt = 100f,
                title = {
                    resources.getString(R.string.editing_volume, "${it.toInt()}%")
                },
                onSetValue = {
                    modifications.add(
                        VideoModification.Volume(it / 100f)
                    )
                },
                onDismiss = {
                    showVolumeDialog = false
                }
            )
        }

        EditingViewBottomAppBarItem(
            text = stringResource(id = R.string.volume),
            icon = R.drawable.volume_max,
            onClick = {
                showVolumeDialog = true
            }
        )

        var showSpeedDialog by remember { mutableStateOf(false) }
        fun getSpeed(index: Int) =
            when (index) {
                0 -> 0.5f
                1 -> 1f
                2 -> 1.5f
                3 -> 2f
                4 -> 4f
                5 -> 8f

                else -> 1f
            }

        if (showSpeedDialog) {
            SliderDialog(
                steps = 4,
                range = 0f..5f,
                startsAt = 1f,
                title = {
                    resources.getString(R.string.editing_speed, "${getSpeed(it.toInt())}X")
                },
                onSetValue = {
                    modifications.add(
                        VideoModification.Speed(getSpeed(it.toInt()))
                    )
                },
                onDismiss = {
                    showSpeedDialog = false
                }
            )
        }

        EditingViewBottomAppBarItem(
            text = stringResource(id = R.string.wilson),
            icon = R.drawable.speed,
            onClick = {
                showSpeedDialog = true
            }
        )

        var showFrameDropDialog by remember { mutableStateOf(false) }
        if (showFrameDropDialog) {
            SliderDialog(
                steps = basicData.frameRate.toInt() - 1,
                range = 1f..truncate(basicData.frameRate),
                startsAt = truncate(basicData.frameRate),
                title = {
                    resources.getString(R.string.editing_framerate, "${it.toInt()}")
                },
                onSetValue = {
                    modifications.add(
                        VideoModification.FrameDrop(it.toInt())
                    )
                },
                onDismiss = {
                    showFrameDropDialog = false
                }
            )
        }

        EditingViewBottomAppBarItem(
            text = stringResource(id = R.string.fps),
            icon = R.drawable.fps_select_60,
            onClick = {
                showFrameDropDialog = true
            }
        )
    }
}