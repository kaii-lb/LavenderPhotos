package com.kaii.photos.compose.single_photo

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import android.view.Window
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.setBarVisibility
import com.kaii.photos.datastore.Behaviour
import com.kaii.photos.datastore.Video
import com.kaii.photos.helpers.EncryptionManager
import com.kaii.photos.helpers.OffsetSaver
import com.kaii.photos.helpers.getSecuredCacheImageForFile
import com.kaii.photos.helpers.motion_photo.MotionPhoto
import com.kaii.photos.helpers.rememberVibratorManager
import com.kaii.photos.helpers.vibrateShort
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.getIv
import com.kaii.photos.mediastore.getThumbnailIv
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

private const val TAG = "com.kaii.photos.compose.single_photo.HorizontalImageList"

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun HorizontalImageList(
    currentMediaItem: MediaStoreData,
    groupedMedia: List<MediaStoreData>,
    state: PagerState,
    window: Window,
    appBarsVisible: MutableState<Boolean>,
    isHidden: Boolean = false,
    isOpenWithView: Boolean = false
) {
    val scale = rememberSaveable { mutableFloatStateOf(1f) }
    val rotation = rememberSaveable { mutableFloatStateOf(0f) }
    val offset = rememberSaveable(stateSaver = OffsetSaver) { mutableStateOf(Offset.Zero) }

    LaunchedEffect(key1 = currentMediaItem) {
        scale.floatValue = 1f
        rotation.floatValue = 0f
        offset.value = Offset.Zero
    }

    val localConfig = LocalConfiguration.current
    var isLandscape by remember { mutableStateOf(localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) }

    val isTouchLocked = remember { mutableStateOf(false) }

    LaunchedEffect(localConfig) {
        isLandscape = localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (!isLandscape) isTouchLocked.value = false
    }

    val mainViewModel = LocalMainViewModel.current
    val shouldAutoPlay by mainViewModel.settings.Video.getShouldAutoPlay()
        .collectAsStateWithLifecycle(initialValue = true)

    val muteVideoOnStart by mainViewModel.settings.Video.getMuteOnStart()
        .collectAsStateWithLifecycle(initialValue = true)

    val lastVideoWasMuted = rememberSaveable { mutableStateOf(muteVideoOnStart) }
    LaunchedEffect(muteVideoOnStart) {
        lastVideoWasMuted.value = muteVideoOnStart && !isOpenWithView
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
        userScrollEnabled = (scale.floatValue == 1f && rotation.floatValue == 0f && offset.value == Offset.Zero) && !isTouchLocked.value,
        modifier = Modifier
            .fillMaxHeight(1f)
    ) { index ->
        if (groupedMedia.isEmpty()) return@HorizontalPager

        val shouldPlay = remember(state) {
            derivedStateOf {
                (abs(state.currentPageOffsetFraction) < 0.5f && state.currentPage == index)
                        || (abs(state.currentPageOffsetFraction) > 0.5f && state.currentPage == index)
            }
        }

        val mediaStoreItem = groupedMedia[index]

        if (mediaStoreItem.type == MediaType.Video) {
            Box(
                modifier = Modifier
                    .fillMaxSize(1f)
            ) {
                VideoPlayer(
                    item = mediaStoreItem,
                    appBarsVisible = appBarsVisible,
                    shouldAutoPlay = shouldAutoPlay || isOpenWithView,
                    lastWasMuted = lastVideoWasMuted,
                    isTouchLocked = isTouchLocked,
                    window = window,
                    shouldPlay = shouldPlay,
                    modifier = Modifier
                        .fillMaxSize(1f)
                        .mediaModifier(
                            scale = scale,
                            rotation = rotation,
                            offset = offset,
                            window = window,
                            appBarsVisible = appBarsVisible,
                            item = mediaStoreItem
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

                LaunchedEffect(isHidden) {
                    if (!isHidden || model != null) return@LaunchedEffect

                    withContext(Dispatchers.IO) {
                        try {
                            val iv = mediaStoreItem.bytes!!.getIv()
                            val thumbnailIv = mediaStoreItem.bytes.getThumbnailIv()

                            model = EncryptionManager.decryptBytes(
                                bytes = getSecuredCacheImageForFile(
                                    fileName = mediaStoreItem.displayName,
                                    context = context
                                ).readBytes(),
                                iv = thumbnailIv
                            )

                            model = EncryptionManager.decryptBytes(
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

                val motionPhoto = remember {
                    MotionPhoto(mediaStoreItem.uri, context)
                }

                if (motionPhoto.isMotionPhoto) {
                    MotionPhotoView(
                        motionPhoto = motionPhoto,
                        releaseExoPlayer = null,
                        glideImageView = @Composable { modifier ->
                            GlideView(
                                isHidden = isHidden,
                                model = model,
                                mediaStoreItem = mediaStoreItem,
                                scale = scale,
                                rotation = rotation,
                                offset = offset,
                                window = window,
                                appBarsVisible = appBarsVisible,
                                modifier = modifier
                            )
                        }
                    )
                } else {
                    GlideView(
                        isHidden = isHidden,
                        model = model,
                        mediaStoreItem = mediaStoreItem,
                        scale = scale,
                        rotation = rotation,
                        offset = offset,
                        window = window,
                        appBarsVisible = appBarsVisible
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun GlideView(
    isHidden: Boolean,
    model: Any?,
    mediaStoreItem: MediaStoreData,
    scale: MutableFloatState,
    rotation: MutableFloatState,
    offset: MutableState<Offset>,
    window: Window,
    appBarsVisible: MutableState<Boolean>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    GlideImage(
        model = if (isHidden) model else mediaStoreItem.uri,
        contentDescription = "selected image",
        contentScale = ContentScale.Fit,
        failure = placeholder(R.drawable.broken_image),
        modifier = modifier
            .fillMaxSize(1f)
            .mediaModifier(
                scale = scale,
                rotation = rotation,
                offset = offset,
                window = window,
                appBarsVisible = appBarsVisible
            )
    ) {
        it.signature(mediaStoreItem.signature())
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .override(Target.SIZE_ORIGINAL)
            .addListener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable?>?,
                    isFirstResource: Boolean
                ): Boolean {
                    window.colorMode = ActivityInfo.COLOR_MODE_DEFAULT
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable?>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    val activity = (context as Activity)

                    if (resource == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || !activity.display.isHdr) return false

                    if (resource is BitmapDrawable && resource.bitmap.hasGainmap()) {
                        window.colorMode = ActivityInfo.COLOR_MODE_HDR
                    }

                    return false
                }
            })
    }
}

@Composable
fun Modifier.mediaModifier(
    scale: MutableFloatState,
    rotation: MutableFloatState,
    offset: MutableState<Offset>,
    window: Window,
    appBarsVisible: MutableState<Boolean>,
    item: MediaStoreData? = null,
): Modifier {
    val vibratorManager = rememberVibratorManager()
    var isDoubleTapToScaling by remember { mutableStateOf(false) }

    val mainViewModel = LocalMainViewModel.current
    val rotateInViews by mainViewModel.settings.Behaviour.getRotateInViews()
        .collectAsStateWithLifecycle(initialValue = true)

    val animatedScale by animateFloatAsState(
        targetValue = scale.floatValue,
        animationSpec = tween(
            durationMillis = if (isDoubleTapToScaling) 350 else 0
        )
    )
    val animatedRotation by animateFloatAsState(
        targetValue = rotation.floatValue,
        animationSpec = tween(
            durationMillis = if (isDoubleTapToScaling) 350 else 0
        )
    )
    val animatedOffset by animateOffsetAsState(
        targetValue = offset.value,
        animationSpec = tween(
            durationMillis = if (isDoubleTapToScaling) 350 else 0
        )
    )

    LaunchedEffect(isDoubleTapToScaling) {
        if (isDoubleTapToScaling) {
            delay(350)
            isDoubleTapToScaling = false
        }
    }

    return this.then(
        Modifier
            .graphicsLayer(
                scaleX = animatedScale,
                scaleY = animatedScale,
                rotationZ = animatedRotation,
                translationX = -animatedOffset.x * animatedScale,
                translationY = -animatedOffset.y * animatedScale,
                transformOrigin = TransformOrigin(0f, 0f)
            )
            .pointerInput(Unit) {
                if (item?.type != MediaType.Video) {
                    detectTapGestures(
                        onTap = {
                            setBarVisibility(
                                visible = !appBarsVisible.value,
                                window = window
                            ) {
                                appBarsVisible.value = it
                            }
                        },

                        onDoubleTap = { clickOffset ->
                            isDoubleTapToScaling = true
                            if (scale.floatValue == 1f && offset.value == Offset.Zero) {
                                scale.floatValue = 2f
                                rotation.floatValue = 0f
                                offset.value = clickOffset / scale.floatValue
                            } else {
                                scale.floatValue = 1f
                                rotation.floatValue = 0f
                                offset.value = Offset.Zero
                            }
                        }
                    )
                }
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
                                val actualRotation = if (panZoomLock || !rotateInViews) 0f else rotationChange

                                if (actualRotation != 0f || zoomChange != 1f || offsetChange != Offset.Zero) {
                                    val oldScale = scale.floatValue

                                    if (panZoomLock) {
                                        scale.floatValue =
                                            (scale.floatValue * zoomChange).coerceIn(1f, 5f)
                                    }

                                    val nextRotation = rotation.floatValue + actualRotation

                                    val closestPoint = (nextRotation / 360f).roundToInt() * 360f
                                    val delta = abs(closestPoint - nextRotation)
                                    if (
                                        delta < 2.5f &&
                                        scale.floatValue == 1f &&
                                        rotation.floatValue != closestPoint
                                    ) {
                                        vibratorManager.vibrateShort()
                                        rotation.floatValue = closestPoint
                                    } else if (delta > 2.5f || scale.floatValue != 1f) {
                                        rotation.floatValue = nextRotation
                                    }


                                    val isRotating = actualRotation != 0f
                                    val counterOffset =
                                        if (isRotating) offsetChange else Offset.Zero
                                    // compensate for change of visual center of image and offset by that
                                    // this makes it "cleaner" to scale since the image isn't bouncing around when the user moves or scales it
                                    offset.value =
                                        if (scale.floatValue == 1f && rotation.floatValue == 0f) Offset.Zero else
                                            (offset.value + centroid / oldScale).rotateBy(
                                                actualRotation
                                            ) - (centroid / scale.floatValue + (offsetChange - counterOffset).rotateBy(
                                                rotation.floatValue + actualRotation
                                            ))
                                }

                                if (offset.value != Offset.Zero || event.changes.size == 2 || scale.floatValue != 1f) {
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

        val newMedia = groupedMedia.value.toMutableList()
        newMedia.removeAt(scrollIndex)

        groupedMedia.value = newMedia

        if (size == 0) {
            popBackStackAction()
        } else {
            state.animateScrollToPage((scrollIndex).coerceIn(0, size))
        }
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
