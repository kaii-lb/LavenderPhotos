package com.kaii.photos.compose.single_photo.editing_view

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.VideoPlayerConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

// private const val TAG = "VIDEO_EDITOR_TAB_CONTENT"

@Composable
fun VideoEditorTrimContent(
    absolutePath: String,
    currentPosition: MutableFloatState,
    duration: MutableFloatState,
    leftPosition: MutableFloatState,
    rightPosition: MutableFloatState,
    onSeek: (newPosition: Float) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val metadata = MediaMetadataRetriever()
    val thumbnails = remember { mutableStateListOf<Bitmap>() }

    LaunchedEffect(duration.floatValue) {
        if (duration.floatValue == 0f) return@LaunchedEffect

        coroutineScope.launch(Dispatchers.IO) {
            metadata.setDataSource(absolutePath)

            val stepSize = duration.floatValue.roundToInt().seconds.inWholeMicroseconds / 6

            for (i in 0..(VideoPlayerConstants.TRIM_THUMBNAIL_COUNT - 1)) {
                val new = metadata.getScaledFrameAtTime(
                    stepSize * i,
                    MediaMetadataRetriever.OPTION_PREVIOUS_SYNC,
                    480,
                    480
                )

                new?.let { thumbnails.add(it) }
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize(1f)
            .clip(RoundedCornerShape(16.dp))
    ) {
        val leftHandlePosition by remember {
            derivedStateOf {
                this.maxWidth * leftPosition.floatValue / duration.floatValue
            }
        }
        val rightHandlePosition by remember {
            derivedStateOf {
                this.maxWidth * rightPosition.floatValue / duration.floatValue - 24.dp
            }
        }
        val seekHandlePosition by remember {
            derivedStateOf {
                leftHandlePosition + (rightHandlePosition - leftHandlePosition - 28.dp - 10.dp) * (currentPosition.floatValue - leftPosition.floatValue) / (rightPosition.floatValue - leftPosition.floatValue)
            }
        }

        // shows video thumbnails
        Box(
            modifier = Modifier
                .fillMaxSize(1f)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 22.dp, vertical = 6.dp)
        ) {
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
        }

        val localDensity = LocalDensity.current

        val leftDraggableState = rememberDraggableState { change ->
            with(localDensity) {
                val new = leftPosition.floatValue + (change * duration.floatValue / this@BoxWithConstraints.maxWidth.toPx())
                leftPosition.floatValue = new.coerceIn(0f, rightPosition.floatValue - (duration.floatValue * 0.2f))

                currentPosition.floatValue = leftPosition.floatValue
                onSeek(currentPosition.floatValue)
            }
        }
        Box(
            modifier = Modifier
                .offset(x = leftHandlePosition)
                .fillMaxHeight(1f)
                .width(24.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 0.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 0.dp
                    )
                )
                .background(MaterialTheme.colorScheme.primary)
                .draggable(
                    state = leftDraggableState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        currentPosition.floatValue = leftPosition.floatValue
                        onSeek(currentPosition.floatValue)
                    }
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

        val rightDraggableState = rememberDraggableState { change ->
            with(localDensity) {
                val new = rightPosition.floatValue + (change * duration.floatValue / this@BoxWithConstraints.maxWidth.toPx())
                rightPosition.floatValue = new.coerceIn(leftPosition.floatValue + (duration.floatValue * 0.2f), duration.floatValue)

                currentPosition.floatValue = rightPosition.floatValue
                onSeek(currentPosition.floatValue)
            }
        }

        Box(
            modifier = Modifier
                .offset(x = rightHandlePosition)
                .fillMaxHeight(1f)
                .width(24.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 0.dp,
                        topEnd = 16.dp,
                        bottomStart = 0.dp,
                        bottomEnd = 16.dp
                    )
                )
                .background(MaterialTheme.colorScheme.primary)
                .draggable(
                    state = rightDraggableState,
                    orientation = Orientation.Horizontal
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

        Box(
            modifier = Modifier
                .offset(x = leftHandlePosition + 24.dp)
                .width(rightHandlePosition - leftHandlePosition - 24.dp)
                .height(this.maxHeight)
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()

                    drawRoundRect(
                        color = Color.Blue,
                        topLeft = with(localDensity) {
                            Offset(
                                x = 0f,
                                y = 6.dp.toPx()
                            )
                        },
                        size = with(localDensity) {
                            Size(
                                width = (rightHandlePosition - leftHandlePosition - 24.dp).toPx(),
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

        val videoPositionDraggableState = rememberDraggableState { change ->
            with(localDensity) {
                val new = currentPosition.floatValue + (change * duration.floatValue / this@BoxWithConstraints.maxWidth.toPx())
                currentPosition.floatValue = new.coerceIn(leftPosition.floatValue, rightPosition.floatValue)
                onSeek(currentPosition.floatValue)
            }
        }

        var isDraggingSeekHandle by remember { mutableStateOf(false) }
        val animatedSeekOffset by animateDpAsState(
            targetValue = if (seekHandlePosition.isUnspecified) 0.dp else seekHandlePosition,
            animationSpec = tween(
                durationMillis = if (!isDraggingSeekHandle) AnimationConstants.DURATION else 0
            )
        )
        Box(
            modifier = Modifier
                .offset(x = animatedSeekOffset + 28.dp, y = 10.dp)
                .width(6.dp)
                .height(this.maxHeight - 20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface)
                .draggable(
                    state = videoPositionDraggableState,
                    orientation = Orientation.Horizontal,
                    onDragStarted = {
                        isDraggingSeekHandle = true
                    },
                    onDragStopped = {
                        isDraggingSeekHandle = false
                    }
                )
        )
    }
}