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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
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

    LaunchedEffect(key1 = currentMediaItem) {
		scale.value = 1f
		rotation.value = 0f
		offset.value = Offset.Zero
	}

    val isPlaying = remember { mutableStateOf(false) }
    val isMuted = remember { mutableStateOf(false) }
    val currentVideoPosition = remember { mutableFloatStateOf(0f) }
    
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
                        GlideImage(
                            model = path,
                            contentDescription = "selected video",
                            contentScale = ContentScale.Fit,
                            failure = placeholder(R.drawable.broken_image),
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
                        ) {
                            it.thumbnail(preloadRequestBuilder).signature(mediaStoreItem.signature()).diskCacheStrategy(
                                DiskCacheStrategy.ALL)
                        }

                        VideoPlayerControls(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerControls(
    showControls: MutableState<Boolean>,
    isPlaying: MutableState<Boolean>,
    isMuted: MutableState<Boolean>,
    currentVideoPosition: MutableState<Float>,
    modifier: Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    AnimatedVisibility(
        visible = showControls.value,
        enter = expandIn(
            animationSpec = tween(
                durationMillis = 500
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = 500
            )
        ),
        exit = shrinkOut(
            animationSpec = tween(
                durationMillis = 500
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = 500
            )
        ),
        modifier = modifier
    ) {
        Box (
            modifier = Modifier
                .then(modifier)
                .background(CustomMaterialTheme.colorScheme.background.copy(0.1f))
        ) {
        	Row (
        		modifier = Modifier
                    .height(172.dp)
                    .align(Alignment.BottomCenter),
        		verticalAlignment = Alignment.Top
        	) {
	            Row (
	                modifier = Modifier
                        .fillMaxWidth(1f)
                        .wrapContentHeight()
                        .padding(8.dp, 0.dp),
	                verticalAlignment = Alignment.CenterVertically,
	                horizontalArrangement = Arrangement.Center
	            ) {
					Row (
	                    modifier = Modifier
                            .height(32.dp)
                            .wrapContentWidth()
                            .clip(RoundedCornerShape(1000.dp))
                            .background(CustomMaterialTheme.colorScheme.secondaryContainer)
                            .padding(4.dp, 0.dp),
	                    verticalAlignment = Alignment.CenterVertically,
	                    horizontalArrangement = Arrangement.Center	                        
					) {
		                Text (
		                    text = "00:00",
		                    style = TextStyle(
		                        fontSize = TextUnit(12f, TextUnitType.Sp),
		                        color = CustomMaterialTheme.colorScheme.onBackground,
		                        textAlign = TextAlign.Center,
		                    ),
		                )
					}

					Spacer (modifier = Modifier.width(8.dp))

	                Slider (
	                    value = currentVideoPosition.value,
				        valueRange = 0f..120f,
				        onValueChange = { pos -> 
							currentVideoPosition.value = pos
				        },
				        steps = 120,	                    
	                    thumb = {
	                        SliderDefaults.Thumb(
	                            interactionSource = interactionSource,
	                            thumbSize = DpSize(6.dp, 16.dp),
	                        )
	                    },
                        track = { sliderState ->
                            val colors = SliderDefaults.colors()

                            SliderDefaults.Track(
                                sliderState = sliderState,
                                trackInsideCornerSize = 8.dp,
                                colors = colors.copy(
                                    activeTickColor = colors.activeTrackColor,
                                    inactiveTickColor = colors.inactiveTrackColor,
                                    disabledActiveTickColor = colors.disabledActiveTrackColor,
                                    disabledInactiveTickColor = colors.disabledInactiveTrackColor,

                                    activeTrackColor = colors.activeTrackColor,
                                    inactiveTrackColor = colors.inactiveTrackColor,

                                    disabledThumbColor = colors.activeTrackColor,
                                    thumbColor = colors.activeTrackColor
                                ),
                                thumbTrackGapSize = 4.dp,
                                drawTick = { _, _ -> },
                                modifier = Modifier
                                    .height(16.dp)
                            )
                        },
	                    // interactionSource = interactionSource,
	                    modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
	                )

	                Spacer (modifier = Modifier.width(8.dp))

					Row (
	                    modifier = Modifier
                            .height(32.dp)
                            .wrapContentWidth()
                            .clip(RoundedCornerShape(1000.dp))
                            .background(CustomMaterialTheme.colorScheme.secondaryContainer)
                            .padding(4.dp, 0.dp),
	                    verticalAlignment = Alignment.CenterVertically,
	                    horizontalArrangement = Arrangement.Center
					) {
		                Text (
		                    text = "02:34",
		                    style = TextStyle(
		                        fontSize = TextUnit(12f, TextUnitType.Sp),
		                        color = CustomMaterialTheme.colorScheme.onBackground,
		                        textAlign = TextAlign.Center,
		                    ),
		                )
					}

					Spacer (modifier = Modifier.width(8.dp))

	                FilledTonalIconButton(
	                    onClick = {
	                        isMuted.value = !isMuted.value
	                    },
	                    modifier = Modifier
	                    	.size(32.dp)
	                ) {
	                    Icon(
	                        painter = painterResource(id = if(isMuted.value) R.drawable.volume_mute else R.drawable.volume_max),
	                        contentDescription = "Video player mute or un-mute",
		                    modifier = Modifier
		                    	.size(24.dp)	                        
	                    )
	                }					
	            }
        	}

            Row (
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .wrapContentHeight()
                    .align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                FilledTonalIconButton(
                    onClick = {
                        // TODO: seek exoplayer backward 5 seconds
                    },
                    modifier = Modifier
                    	.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.fast_rewind),
                        contentDescription = "Video player skip back 5 seconds",
                        modifier = Modifier
							.padding(0.dp, 0.dp, 2.dp, 0.dp)                                                
                    )
                }

                Spacer (modifier = Modifier.width(48.dp))

                FilledTonalIconButton(
                    onClick = {
                        // TODO: play or pause exoplayer
                        isPlaying.value = !isPlaying.value
                    },
                    modifier = Modifier
                    	.size(48.dp)                    
                ) {
                    Icon(
                        painter = painterResource(id = if (!isPlaying.value) R.drawable.play_arrow else R.drawable.pause),
                        contentDescription = "Video player play or pause"
                    )
                }

                Spacer (modifier = Modifier.width(48.dp))

                FilledTonalIconButton(
                    onClick = {
                        // TODO: seek exoplayer forward 5 seconds
                    },
                    modifier = Modifier
                    	.size(48.dp)   
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.fast_forward),
                        contentDescription = "Video player skip forward 5 seconds",
                        modifier = Modifier
							.padding(2.dp, 0.dp, 0.dp, 0.dp)                        
                    )
                }
            }
        }
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
