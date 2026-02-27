package com.kaii.photos.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.Spring.StiffnessMediumLow
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import com.kaii.photos.helpers.OffsetSaver
import com.kaii.photos.helpers.SingleViewConstants
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun Modifier.transformable(): Modifier {
    var scale by rememberSaveable { mutableFloatStateOf(1f) }
    var offset by rememberSaveable(stateSaver = OffsetSaver) { mutableStateOf(Offset.Zero) }

    return this.then(
        Modifier
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = -offset.x * scale,
                translationY = -offset.y * scale,
                transformOrigin = TransformOrigin(0f, 0f)
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

                                // calculate the amount of movement/zoom/rotation happening and if its past a certain point
                                // then go ahead and try to apply the gestures
                                if (zoomMotion > touchSlop || offsetMotion > touchSlop) {
                                    pastTouchSlop = true
                                }
                            }

                            if (pastTouchSlop) {
                                val centroid = event.calculateCentroid()

                                if (zoomChange != 1f || offsetChange != Offset.Zero) {
                                    val oldScale = scale

                                    scale =
                                        (scale * zoomChange).coerceIn(1f, maxZoom)

                                    // compensate for change of visual center of image and offset by that
                                    // this makes it "cleaner" to scale since the image isn't bouncing around when the user moves or scales it
                                    offset =
                                        if (scale == 1f) Offset.Zero else
                                            (offset + centroid / oldScale) - (centroid / scale + offsetChange)
                                }

                                if (offset != Offset.Zero || event.changes.size == 2 || scale != 1f) {
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

// from https://developer.android.com/reference/kotlin/androidx/compose/ui/layout/package-summary#(androidx.compose.ui.Modifier).onPlaced(kotlin.Function1)
@Composable
fun Modifier.animatePlacement(): Modifier = composed {
    val scope = rememberCoroutineScope()
    var targetOffset by remember { mutableStateOf(IntOffset.Zero) }
    var animatable by remember {
        mutableStateOf<Animatable<IntOffset, AnimationVector2D>?>(null)
    }
    this
        .onPlaced {
            // Calculate the position in the parent layout
            targetOffset = it.positionInParent().round()
        }
        .offset {
            // Animate to the new target offset when alignment changes.
            val anim =
                animatable
                    ?: Animatable(targetOffset, IntOffset.VectorConverter).also {
                        animatable = it
                    }
            if (anim.targetValue != targetOffset) {
                scope.launch {
                    anim.animateTo(targetOffset, spring(stiffness = StiffnessMediumLow))
                }
            }
            // Offset the child in the opposite direction to the targetOffset, and slowly catch
            // up to zero offset via an animation to achieve an overall animated movement.
            animatable?.let { it.value - targetOffset } ?: IntOffset.Zero
        }
}