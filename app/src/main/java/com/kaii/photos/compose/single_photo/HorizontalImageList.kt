package com.kaii.photos.compose.single_photo

import android.content.res.Configuration
import android.view.Window
import android.view.WindowInsetsController
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kaii.photos.R
import com.kaii.photos.helpers.rememberVibratorManager
import com.kaii.photos.helpers.vibrateShort
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.signature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun HorizontalImageList(
    navController: NavHostController,
    currentMediaItem: MediaStoreData,
    groupedMedia: List<MediaStoreData>,
    state: PagerState,
    scale: MutableState<Float>,
    rotation: MutableState<Float>,
    offset: MutableState<Offset>,
    systemBarsShown: MutableState<Boolean>,
    window: Window,
    appBarsVisible: MutableState<Boolean>,
    isHidden: Boolean = false
) {
    LaunchedEffect(key1 = currentMediaItem) {
        scale.value = 1f
        rotation.value = 0f
        offset.value = Offset.Zero
    }

    HorizontalPager(
        state = state,
        verticalAlignment = Alignment.CenterVertically,
        pageSpacing = 8.dp,
        key = {
            if (groupedMedia.isNotEmpty() && it <= groupedMedia.size - 1) {
                val neededItem = groupedMedia[it]
                neededItem.uri.toString()
            } else {
                System.currentTimeMillis()
                    .toString() // this should be unique enough in case of failure right?
            }
        },
        snapPosition = SnapPosition.Center,
        userScrollEnabled = scale.value == 1f && rotation.value == 0f && offset.value == Offset.Zero,
        modifier = Modifier
            .fillMaxHeight(1f)
    ) { index ->
        val shouldPlay by remember(state) {
            derivedStateOf {
                (abs(state.currentPageOffsetFraction) < .5 && state.currentPage == index) || (abs(
                    state.currentPageOffsetFraction
                ) > .5 && state.targetPage == index)
            }
        }

        val mediaStoreItem = groupedMedia[index]

        val windowInsetsController = window.insetsController ?: return@HorizontalPager
        val path = if (isHidden) mediaStoreItem.uri.path else mediaStoreItem.uri

        if (mediaStoreItem.type == MediaType.Video) {
            val showVideoPlayerControls = remember { mutableStateOf(true) }
            val canFadeControls = remember { mutableStateOf(true) }

            LaunchedEffect(
                key1 = showVideoPlayerControls.value,
                key2 = canFadeControls.value,
                key3 = appBarsVisible.value
            ) {
                if (canFadeControls.value) {
                    delay(5000)
                    showVideoPlayerControls.value = false
                    appBarsVisible.value = false
                    systemBarsShown.value = false

                    windowInsetsController.apply {
                        hide(WindowInsetsCompat.Type.systemBars())
                        systemBarsBehavior =
                            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                    window.setDecorFitsSystemWindows(false)


                    canFadeControls.value = false
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize(1f)
            ) {
                VideoPlayer(
                    item = mediaStoreItem,
                    visible = showVideoPlayerControls,
                    appBarsVisible = appBarsVisible,
                    shouldPlay = shouldPlay,
                    navController = navController,
                    canFadeControls = canFadeControls,
                    windowInsetsController = windowInsetsController,
                    window = window,
                    modifier = Modifier
                        .fillMaxSize(1f)
                        .mediaModifier(
                            scale = scale,
                            rotation = rotation,
                            offset = offset,
                            systemBarsShown = systemBarsShown,
                            window = window,
                            windowInsetsController = windowInsetsController,
                            appBarsVisible = appBarsVisible,
                            item = mediaStoreItem,
                            showVideoPlayerController = showVideoPlayerControls,
                        )
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize(1f)
            ) {
                GlideImage(
                    model = path,
                    contentDescription = "selected image",
                    contentScale = ContentScale.Fit,
                    failure = placeholder(R.drawable.broken_image),
                    modifier = Modifier
                        .fillMaxSize(1f)
                        .mediaModifier(
                            scale = scale,
                            rotation = rotation,
                            offset = offset,
                            systemBarsShown = systemBarsShown,
                            window = window,
                            windowInsetsController = windowInsetsController,
                            appBarsVisible = appBarsVisible
                        )
                ) {
                    it.signature(mediaStoreItem.signature())
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                }
            }
        }
    }
}

@Composable
private fun Modifier.mediaModifier(
    scale: MutableState<Float>,
    rotation: MutableState<Float>,
    offset: MutableState<Offset>,
    systemBarsShown: MutableState<Boolean>,
    window: Window,
    windowInsetsController: WindowInsetsController,
    appBarsVisible: MutableState<Boolean>,
    item: MediaStoreData? = null,
    showVideoPlayerController: MutableState<Boolean>? = null,
): Modifier {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val vibratorManager = rememberVibratorManager()

    return this.then(
        Modifier
            .graphicsLayer(
                scaleX = scale.value,
                scaleY = scale.value,
                rotationZ = rotation.value,
                translationX = -offset.value.x * scale.value,
                translationY = -offset.value.y * scale.value,
                transformOrigin = TransformOrigin(0f, 0f)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (systemBarsShown.value) {
                            windowInsetsController.apply {
                                hide(WindowInsetsCompat.Type.systemBars())
                                systemBarsBehavior =
                                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                            }
                            window.setDecorFitsSystemWindows(false)
                            systemBarsShown.value = false
                            appBarsVisible.value = false

                            if (!isLandscape) showVideoPlayerController?.value = false
                        } else {
                            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                            window.setDecorFitsSystemWindows(false)
                            systemBarsShown.value = true
                            appBarsVisible.value = true

                            if (!isLandscape) showVideoPlayerController?.value = true
                        }
                    },

                    onDoubleTap = { clickOffset ->
                        if (item?.type == MediaType.Video && showVideoPlayerController != null) {
                            if (isLandscape) showVideoPlayerController.value =
                                !showVideoPlayerController.value
                        } else {
                            if (scale.value == 1f && offset.value == Offset.Zero) {
                                scale.value = 2f
                                rotation.value = 0f
                                offset.value = clickOffset / scale.value
                            } else {
                                scale.value = 1f
                                rotation.value = 0f
                                offset.value = Offset.Zero
                            }
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    var localRotation = 0f
                    var localZoom = 1f
                    var localOffset = Offset.Zero
                    var pastTouchSlop = false
                    var panZoomLock = false
                    val touchSlop = viewConfiguration.touchSlop

                    awaitFirstDown()

                    do {
                        val event = awaitPointerEvent()

                        // ignore gesture if it is already consumed or user is not using two fingers
                        val canceled =
                            event.changes.any { it.isConsumed }

                        if (!canceled) {
                            val zoomChange = event.calculateZoom()
                            val rotationChange = event.calculateRotation()
                            val offsetChange = event.calculatePan()

                            if (!pastTouchSlop) {
                                localZoom *= zoomChange
                                localRotation += rotationChange
                                localOffset += offsetChange

                                val centroidSize = event.calculateCentroidSize()

                                // were basically getting the amount of change here
                                val zoomMotion = abs(1 - localZoom) * centroidSize
                                val rotationMotion =
                                    abs(localRotation * PI.toFloat() * centroidSize / 180f)
                                val offsetMotion = localOffset.getDistance()

                                // calculate the amount of movement/zoom/rotation happening and if its past a certain point
                                // then go ahead and try to apply the gestures
                                if (zoomMotion > touchSlop || rotationMotion > touchSlop || offsetMotion > touchSlop) {
                                    pastTouchSlop = true
                                    panZoomLock = rotationMotion < touchSlop
                                }
                            }

                            if (pastTouchSlop) {
                                val centroid = event.calculateCentroid()

                                // ignore rotation if user is moving or zooming, QOL thing
                                val actualRotation = if (panZoomLock) 0f else rotationChange

                                if (actualRotation != 0f || zoomChange != 1f || offsetChange != Offset.Zero) {
                                    val oldScale = scale.value

                                    if (panZoomLock) {
                                        scale.value = (scale.value * zoomChange).coerceIn(1f, 5f)
                                    }

                                    val nextRotation = rotation.value + actualRotation

                                    val closestPoint = (nextRotation / 360f).roundToInt() * 360f
                                    val delta = abs(closestPoint - nextRotation)
                                    if (
                                        delta < 2.5f &&
                                        scale.value == 1f &&
                                        rotation.value != closestPoint
                                    ) {
                                        vibratorManager.vibrateShort()
                                        rotation.value = closestPoint
                                    } else if (delta > 2.5f || scale.value != 1f) {
                                        rotation.value = nextRotation
                                    }


                                    val isRotating = actualRotation != 0f
                                    val counterOffset =
                                        if (isRotating) offsetChange else Offset.Zero
                                    // compensate for change of visual center of image and offset by that
                                    // this makes it "cleaner" to scale since the image isn't bouncing around when the user moves or scales it
                                    offset.value =
                                        if (scale.value == 1f && rotation.value == 0f) Offset.Zero else
                                            (offset.value + centroid / oldScale).rotateBy(
                                                actualRotation
                                            ) - (centroid / scale.value + (offsetChange - counterOffset).rotateBy(
                                                rotation.value + actualRotation
                                            ))
                                }

                                if (offset.value != Offset.Zero || event.changes.size == 2 || scale.value != 1f) {
                                    event.changes.forEach {
                                        it.consume()
                                    }
                                }
                            }
                        }
                    } while (!canceled && event.changes.any { it.pressed } && showVideoPlayerController == null)
                }
            })
}

/** deals with grouped media modifications, in this case removing stuff*/
fun sortOutMediaMods(
    item: MediaStoreData,
    groupedMedia: MutableState<List<MediaStoreData>>,
    coroutineScope: CoroutineScope,
    state: PagerState,
    popBackStackAction: () -> Unit
) {
    coroutineScope.launch {
        val size = groupedMedia.value.size - 1
        val scrollIndex = groupedMedia.value.indexOf(item)

        val newMedia = groupedMedia.value.toList().toMutableList()
        newMedia.removeAt(scrollIndex)

        if (size == 0) {
            popBackStackAction()
        } else {
            state.animateScrollToPage((scrollIndex).coerceIn(0, size))
//            state.scrollToPage((scrollIndex).coerceIn(0, size))
        }

        groupedMedia.value = newMedia
    }
}

fun Offset.rotateBy(angle: Float): Offset {
    val angleInRadians = angle * PI / 180
    val cos = cos(angleInRadians)
    val sin = sin(angleInRadians)

    return Offset(
        (x * cos - y * sin).toFloat(),
        (x * sin + y * cos).toFloat()
    )
}
