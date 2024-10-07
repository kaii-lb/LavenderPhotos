package com.kaii.photos.compose.single_photo

import android.graphics.drawable.Drawable
import android.util.Log
import android.view.Window
import android.view.WindowInsetsController
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavHostController
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.bumptech.glide.integration.compose.rememberGlidePreloadingData
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kaii.photos.R
import com.kaii.photos.compose.CustomMaterialTheme
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.signature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalGlideComposeApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HorizontalImageList(
	currentMediaItem: MediaStoreData,
    groupedMedia: MutableState<List<MediaStoreData>>,
    state: LazyListState,
    scale: MutableState<Float>,
    rotation: MutableState<Float>,
    offset: MutableState<Offset>,
    systemBarsShown: MutableState<Boolean>,
    window: Window,
    appBarsVisible: MutableState<Boolean>,
    isHidden: Boolean = false
) {
    val requestBuilderTransform =
        { item: MediaStoreData, requestBuilder: RequestBuilder<Drawable> ->
            requestBuilder.load(item.uri).signature(item.signature()).centerCrop()
        }

    val preloadingData =
        rememberGlidePreloadingData(
            groupedMedia.value,
            Size(75f, 75f),
            requestBuilderTransform = requestBuilderTransform,
        )

    val context = LocalContext.current
    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).apply {
            setPauseAtEndOfMediaItems(true)
            setAudioAttributes(
                AudioAttributes.Builder().apply {
                    setHandleAudioBecomingNoisy(true)
                }.build(),
                true
            )

            setLoadControl(
                DefaultLoadControl.Builder().apply {
	                setBufferDurationsMs(
	                    1000,
	                    5000,
	                    1000,
	                    1000
	                )

	                setBackBuffer(
	                    1000,
	                    false
	                )

	                setSeekBackIncrementMs(5000)
	                setSeekForwardIncrementMs(5000)

	                setPrioritizeTimeOverSizeThresholds(false)

	                setPriority(C.PRIORITY_MAX)                    
                }.build()
            )

            setSeekForwardIncrementMs(5000)
            setSeekBackIncrementMs(5000)
        }.build()
    }

    val isPlaying = remember { mutableStateOf(true) }
    val isMuted = remember { mutableStateOf(false) }
    val currentVideoPosition = remember { mutableFloatStateOf(0f) }
	var lastVideoPosition by remember { mutableFloatStateOf(0f) }
	// add is seeking param to stop settings currentVideoPosition when slide-seeking
    LaunchedEffect(key1 = isPlaying.value, key2 = isMuted.value, key3 = currentVideoPosition.floatValue) {
        if (isPlaying.value && !exoPlayer.isPlaying()) exoPlayer.play() else if (!isPlaying.value) exoPlayer.pause()
        
        if (isMuted.value) exoPlayer.volume = 0f else exoPlayer.volume = 1f

        if (currentVideoPosition.floatValue != lastVideoPosition) {
	        exoPlayer.seekTo((currentVideoPosition.floatValue * 1000).toLong())
	        lastVideoPosition = currentVideoPosition.floatValue
        }
    }

	// causes blinking issue, plz fix
    // LaunchedEffect(isPlaying.value) {
    // 	while (isPlaying.value) {
   	// 	 	currentVideoPosition.value = (exoPlayer.currentPosition / 1000).toFloat()
   	// 	 	lastVideoPosition = currentVideoPosition.value
    // 		kotlinx.coroutines.delay(1000)
    // 	}
    // }

    LaunchedEffect(key1 = currentMediaItem) {
        scale.value = 1f
        rotation.value = 0f
        offset.value = Offset.Zero

        if (currentMediaItem.type == MediaType.Video) {
            exoPlayer.setMediaItem(MediaItem.fromUri(if (isHidden) currentMediaItem.absolutePath.toUri() else currentMediaItem.uri))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        } else {
            exoPlayer.stop()
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow {
            state.itemIsVisible(currentMediaItem, groupedMedia.value)
        }.collect { visible ->
            if (visible) {
                isPlaying.value = false
            }
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(exoPlayer) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    exoPlayer.pause()
                }

                Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> {
                    exoPlayer.pause()
                }

                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            // exoPlayer.stop()
        }
    }

    LazyRow (
        modifier = Modifier
            .fillMaxHeight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        state = state,
        flingBehavior = rememberSnapFlingBehavior(lazyListState = state)
    ) {
        items(
            count = groupedMedia.value.size,
            key = {
                if (groupedMedia.value.isNotEmpty() && it != groupedMedia.value.size) {
                    val neededItem = groupedMedia.value[it]
                    neededItem.uri.toString()
                } else {
                    System.currentTimeMillis().toString() // this should be unique enough in case of failure right?
                }
            },
        ) { i ->
            val movableContent = movableContentOf {
                val (mediaStoreItem, preloadRequestBuilder) = preloadingData[i]

                val windowInsetsController = window.insetsController ?: return@movableContentOf
                val path = if (isHidden) mediaStoreItem.uri.path else mediaStoreItem.uri

                if (mediaStoreItem.type == MediaType.Video) {
                    Box (
                        modifier = Modifier
                            .fillMaxSize(1f)
                    ) {
                        VideoPlayer(
                        	exoPlayer = exoPlayer,
                         	modifier = Modifier
                         		.fillParentMaxSize(1f)
                         		.align(Alignment.Center)
                                .mediaModifier(
	                                scale,
	                                rotation,
	                                offset,
	                                systemBarsShown,
	                                window,
	                                windowInsetsController,
	                                appBarsVisible
	                            )
                       	)

                        VideoPlayerControls(
                        	exoPlayer = exoPlayer,
                        	showControls = appBarsVisible,
                            isPlaying = isPlaying,
                            isMuted = isMuted,
                            currentVideoPosition = currentVideoPosition,
                        	modifier = Modifier
                                .fillParentMaxSize(1f)
                                .align(Alignment.Center)
                       	)
                    }
                } else {
                    GlideImage(
                        model = path,
                        contentDescription = "selected image",
                        contentScale = ContentScale.Fit,
                        failure = placeholder(R.drawable.broken_image),
                        modifier = Modifier
                            .fillParentMaxSize(1f)
                            .mediaModifier(
                                scale,
                                rotation,
                                offset,
                                systemBarsShown,
                                window,
                                windowInsetsController,
                                appBarsVisible
                            )
                    ) {
                        it.thumbnail(preloadRequestBuilder).signature(mediaStoreItem.signature()).diskCacheStrategy(
                            DiskCacheStrategy.ALL)
                    }
                }
            }
            movableContent()
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Modifier.mediaModifier(
    scale: MutableState<Float>,
    rotation: MutableState<Float>,
    offset: MutableState<Offset>,
    systemBarsShown: MutableState<Boolean>,
    window: Window,
    windowInsetsController: WindowInsetsController,
    appBarAlpha: MutableState<Boolean>
) = this.then(
    Modifier
        .combinedClickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
            onClick = {
                if (systemBarsShown.value) {
                    windowInsetsController.apply {
                        hide(WindowInsetsCompat.Type.systemBars())
                        systemBarsBehavior =
                            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                    window.setDecorFitsSystemWindows(false)
                    systemBarsShown.value = false
                    appBarAlpha.value = false
                } else {
                    windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                    window.setDecorFitsSystemWindows(false)
                    systemBarsShown.value = true
                    appBarAlpha.value = true
                }
            },

            onDoubleClick = {
                if (scale.value == 1f) {
                    scale.value = 2f
                    rotation.value = 0f
                    offset.value = Offset.Zero
                } else {
                    scale.value = 1f
                    rotation.value = 0f
                    offset.value = Offset.Zero
                }
            },
        )
        .graphicsLayer(
            scaleX = scale.value,
            scaleY = scale.value,
            rotationZ = rotation.value,
            translationX = -offset.value.x * scale.value,
            translationY = -offset.value.y * scale.value,
            transformOrigin = TransformOrigin(0.5f, 0.5f)
        )
        .pointerInput(Unit) {
            // loop over each gesture and consume only those we care about
            // so we don't interfere with other gestures
            awaitEachGesture {
                awaitFirstDown()

                do {
                    val event = awaitPointerEvent()

                    if (event.changes.size == 2) {
                        scale.value *= event.calculateZoom()
                        scale.value = scale.value.coerceIn(0.75f, 5f)

                        rotation.value += event.calculateRotation()

                        event.changes.forEach {
                            it.consume()
                        }
                    } else if (event.changes.size == 1 && event.calculatePan() != Offset.Zero) {
                        if (scale.value != 1f) {
                            // this is from android docs, i have no clue what the math here is xD
                            offset.value = (offset.value + Offset(0.5f, 0.5f) / scale.value) -
                                    (Offset(0.5f, 0.5f) / scale.value + event
                                        .calculatePan()
                                        .rotateBy(rotation.value))

                            event.changes.forEach {
                                it.consume()
                            }
                        }
                    }
                } while (event.changes.any { it.pressed })
            }
        })

/** deals with grouped media modifications, in this case removing stuff*/
fun sortOutMediaMods(
    item: MediaStoreData,
    groupedMedia: MutableState<List<MediaStoreData>>,
    coroutineScope: CoroutineScope,
    navController: NavHostController,
    state: LazyListState
) {
    coroutineScope.launch {
        val size = groupedMedia.value.size - 1
        val scrollIndex = groupedMedia.value.indexOf(item) // is this better?
//        val scrollIndex = state.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0 // or this?
        val added = if (scrollIndex == size) -1 else 1

        val newMedia = groupedMedia.value.toList().toMutableList()
        newMedia.removeAt(scrollIndex)

        Log.d("SORT_OUT_MEDIA_MODS", "$scrollIndex $added is the needed index out of $size")

        if (size == 0) {
            navController.popBackStack()
        } else {
            state.scrollToItem((scrollIndex + added).coerceAtLeast(0))
        }

        groupedMedia.value = newMedia
    }
}

fun Offset.rotateBy(angle: Float) : Offset {
    val angleInRadians = angle * (PI / 180)
    val cos = cos(angleInRadians)
    val sin = sin(angleInRadians)

    return Offset(
        (x * cos - y * sin).toFloat(),
        (x * sin + y * cos).toFloat()
    )
}

private fun LazyListState.itemIsVisible(item: MediaStoreData, list: List<MediaStoreData>) : Boolean {
    if (list.isEmpty()) return false

    return layoutInfo.visibleItemsInfo.map {
        list[it.index]
    }.contains(item)
}
