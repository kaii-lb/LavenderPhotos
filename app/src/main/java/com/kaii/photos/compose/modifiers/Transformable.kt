package com.kaii.photos.compose.modifiers

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.kaii.photos.helpers.SingleViewConstants
import com.kaii.photos.presentation.ui.TransformableState
import com.kaii.photos.presentation.ui.retainTransformableState
import kotlin.math.abs

@Composable
fun Modifier.transformable(
    state: TransformableState = retainTransformableState(),
): Modifier {
    return this.then(
        Modifier
            .then(
                if (state.applyTransformation) {
                    Modifier.graphicsLayer(
                        scaleX = state.scale,
                        scaleY = state.scale,
                        translationX = -state.offset.x * state.scale,
                        translationY = -state.offset.y * state.scale,
                        transformOrigin = TransformOrigin(0f, 0f)
                    )
                } else Modifier
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    var localZoom = 1f
                    var localOffset = Offset.Zero
                    var pastTouchSlop = false
                    val touchSlop = viewConfiguration.touchSlop
                    val maxZoom = SingleViewConstants.MAX_ZOOM

                    awaitFirstDown()

                    do {
                        val event = awaitPointerEvent()

                        // ignore gesture if it is already consumed or user is not using two fingers
                        val canceled =
                            event.changes.any { it.isConsumed }

                        if (!canceled) {
                            val zoomChange = event.calculateZoom()
                            val offsetChange = event.calculatePan()

                            if (!pastTouchSlop) {
                                localZoom *= zoomChange
                                localOffset += offsetChange

                                val centroidSize = event.calculateCentroidSize()

                                // were basically getting the amount of change here
                                val zoomMotion = abs(1 - localZoom) * centroidSize
                                val offsetMotion = localOffset.getDistance()

                                // calculate the amount of movement/zoom/rotation happening and if it's past a certain point
                                // then go ahead and try to apply the gestures
                                if (zoomMotion > touchSlop || offsetMotion > touchSlop) {
                                    pastTouchSlop = true
                                }
                            }

                            if (pastTouchSlop) {
                                val centroid = event.calculateCentroid()

                                if (zoomChange != 1f || offsetChange != Offset.Zero) {
                                    val oldScale = state.scale

                                    state.scale =
                                        (state.scale * zoomChange).coerceIn(1f, maxZoom)

                                    val offsetDiff =
                                        if (state.applyTransformation) offsetChange
                                        else offsetChange / oldScale

                                    // compensate for change of visual center of image and offset by that
                                    // this makes it "cleaner" to scale since the image isn't bouncing around when the user moves or scales it
                                    state.offset =
                                        if (state.scale == 1f) Offset.Zero
                                        else (state.offset + centroid / oldScale) - (centroid / state.scale + offsetDiff)
                                }

                                if (state.offset != Offset.Zero || event.changes.size == 2 || state.scale != 1f) {
                                    event.changes.forEach {
                                        it.consume()
                                    }
                                }
                            }
                        }
                    } while (!canceled && event.changes.any { it.pressed })
                }
            })
}