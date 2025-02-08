package com.kaii.photos.compose.single_photo

import android.util.Log
import android.content.res.Configuration
import android.view.Window
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kaii.photos.R
import com.kaii.photos.MainActivity.Companion.mainViewModel
import com.kaii.photos.compose.setBarVisibility
import com.kaii.photos.datastore.Video
import com.kaii.photos.helpers.EncryptionManager
import com.kaii.photos.helpers.rememberVibratorManager
import com.kaii.photos.helpers.vibrateShort
import com.kaii.photos.helpers.appSecureVideoCacheDir
import com.kaii.photos.helpers.getSecuredCacheImageForFile
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.signature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val TAG = "HORIZONTAL_IMAGE_LIST"

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
    window: Window,
    appBarsVisible: MutableState<Boolean>,
    isHidden: Boolean = false,
    isOpenWithView: Boolean = false
) {
    LaunchedEffect(key1 = currentMediaItem) {
        scale.value = 1f
        rotation.value = 0f
        offset.value = Offset.Zero
    }

    val localConfig = LocalConfiguration.current
    var isLandscape by remember { mutableStateOf(localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) }

    val isTouchLocked = remember { mutableStateOf(false) }

    LaunchedEffect(localConfig) {
        isLandscape = localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (!isLandscape) isTouchLocked.value = false
    }

	val shouldAutoPlay =
        if (isOpenWithView) true
        else mainViewModel.settings.Video.getShouldAutoPlay().collectAsStateWithLifecycle(initialValue = true).value

	val muteVideoOnStart =
        if (isOpenWithView) false
        else mainViewModel.settings.Video.getMuteOnStart().collectAsStateWithLifecycle(initialValue = true).value

	val lastVideoWasMuted = remember { mutableStateOf(muteVideoOnStart) }

	LaunchedEffect(muteVideoOnStart) {
		lastVideoWasMuted.value = muteVideoOnStart
	}

    val encryptionManager = remember {
        EncryptionManager()
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
        userScrollEnabled = (scale.value == 1f && rotation.value == 0f && offset.value == Offset.Zero) && !isTouchLocked.value,
        modifier = Modifier
            .fillMaxHeight(1f)
    ) { index ->
        val shouldPlay by remember(state) {
            derivedStateOf {
                (abs(state.currentPageOffsetFraction) < 0.5f && state.currentPage == index)
                        || (abs(state.currentPageOffsetFraction) > 0.5f && state.targetPage == index)
            }
        }

        val mediaStoreItem = groupedMedia[index]

        if (mediaStoreItem.type == MediaType.Video && shouldPlay) {
            val showVideoPlayerControls = remember { mutableStateOf(true) }
            val canFadeControls = remember { mutableStateOf(true) }

            LaunchedEffect(
                key1 = showVideoPlayerControls.value,
                key2 = canFadeControls.value,
                key3 = appBarsVisible.value
            ) {
                if (canFadeControls.value && showVideoPlayerControls.value) {
                    delay(5000)
                    setBarVisibility(
                        visible = false,
                        window = window
                    ) {
                        appBarsVisible.value = it

                        showVideoPlayerControls.value = it
                    }

                    canFadeControls.value = false
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize(1f)
            ) {
                VideoPlayer(
                    item = mediaStoreItem,
                    controlsVisible = showVideoPlayerControls,
                    appBarsVisible = appBarsVisible,
                    shouldPlay = shouldPlay,
                    shouldAutoPlay = shouldAutoPlay,
                    lastWasMuted = lastVideoWasMuted,
                    navController = navController,
                    canFadeControls = canFadeControls,
                    isTouchLocked = isTouchLocked,
                    window = window,
                    modifier = Modifier
                        .fillMaxSize(1f)
                        .mediaModifier(
                            scale = scale,
                            rotation = rotation,
                            offset = offset,
                            window = window,
                            appBarsVisible = appBarsVisible,
                            item = mediaStoreItem,
                            showVideoPlayerController = showVideoPlayerControls,
                            isTouchLocked = isTouchLocked
                        )
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize(1f)
            ) {
                var model by remember { mutableStateOf<Any?>(null) }
                val context = LocalContext.current

                LaunchedEffect(Unit) {
                   	if (isHidden) {
	                    withContext(Dispatchers.IO) {
	                    	try {
								val iv = mediaStoreItem.bytes!!.copyOfRange(0, 16)
								val thumbnailIv = mediaStoreItem.bytes.copyOfRange(16, 32)

								model = encryptionManager.decryptBytes(
									bytes = getSecuredCacheImageForFile(fileName = mediaStoreItem.displayName!!, context = context).readBytes(),
									iv = thumbnailIv
								)

								model = encryptionManager.decryptBytes(
									bytes = File(mediaStoreItem.absolutePath).readBytes(),
									iv = iv
								)
	                    	} catch (e: Throwable) {
	                    		Log.d(TAG, e.toString())
	                    		e.printStackTrace()

	                    		mediaStoreItem.uri.path
	                    	}
	                    }
                    }
                }

                GlideImage(
                    model = if (isHidden) model else mediaStoreItem.uri,
                    contentDescription = "selected image",
                    contentScale = ContentScale.Fit,
                    failure = placeholder(R.drawable.broken_image),
                    modifier = Modifier
                        .fillMaxSize(1f)
                        .mediaModifier(
                            scale = scale,
                            rotation = rotation,
                            offset = offset,
                            window = window,
                            appBarsVisible = appBarsVisible,
                            isTouchLocked = isTouchLocked
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
fun Modifier.mediaModifier(
    scale: MutableState<Float>,
    rotation: MutableState<Float>,
    offset: MutableState<Offset>,
    window: Window,
    appBarsVisible: MutableState<Boolean>,
    isTouchLocked: MutableState<Boolean>,
    item: MediaStoreData? = null,
    showVideoPlayerController: MutableState<Boolean>? = null,
): Modifier {
    val localConfig = LocalConfiguration.current
    var isLandscape by remember { mutableStateOf(localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) }
    val vibratorManager = rememberVibratorManager()

    LaunchedEffect(localConfig) {
        isLandscape = localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

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
                        if (!isTouchLocked.value) {
                            if (item?.type == MediaType.Video && showVideoPlayerController != null && isLandscape) {
                            	if (appBarsVisible.value) {
	                                setBarVisibility(
	                                    visible = false,
	                                    window = window
	                                ) {
	                                    appBarsVisible.value = it
	                                }
                            	} else {
                            		showVideoPlayerController.value = !showVideoPlayerController.value
                            	}
                            } else {
                                setBarVisibility(
                                    visible = !appBarsVisible.value,
                                    window = window
                                ) {
                                    appBarsVisible.value = it

                                    if (!isLandscape) showVideoPlayerController?.value = it
                                }
                            }
                        }
                    },

                    onDoubleTap = { clickOffset ->
                        if (!isTouchLocked.value) {
                        	if (item?.type != MediaType.Video && showVideoPlayerController == null) {
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
