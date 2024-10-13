package com.kaii.photos.compose.single_photo

import android.graphics.drawable.Drawable
import android.view.Window
import android.view.WindowInsetsController
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.bumptech.glide.integration.compose.rememberGlidePreloadingData
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kaii.photos.R
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.signature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun HorizontalImageList(
	currentMediaItem: MediaStoreData,
    groupedMedia: MutableState<List<MediaStoreData>>,
    state: PagerState,
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

    HorizontalPager(
        state = state,
        verticalAlignment = Alignment.CenterVertically,
        pageSpacing = 8.dp,
        key = {
            if (groupedMedia.value.isNotEmpty() && it != groupedMedia.value.size) {
                val neededItem = groupedMedia.value[it]
                neededItem.uri.toString()
            } else {
                System.currentTimeMillis().toString() // this should be unique enough in case of failure right?
            }
        },
        snapPosition = SnapPosition.Center,
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
        
        val (mediaStoreItem, preloadRequestBuilder) = preloadingData[index]

        val windowInsetsController = window.insetsController ?: return@HorizontalPager
        val path = if (isHidden) mediaStoreItem.uri.path else mediaStoreItem.uri

        if (mediaStoreItem.type == MediaType.Video) {
	        VideoPlayer(
	            item = mediaStoreItem,
	            visible = appBarsVisible.value,
	            shouldPlay = shouldPlay,
	            modifier = Modifier
	            	.fillMaxSize(1f)
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
        } else {
        	Box (
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
    state: PagerState
) {
    coroutineScope.launch {
        val size = groupedMedia.value.size - 1
        val scrollIndex = groupedMedia.value.indexOf(item) // is this better?
//        val scrollIndex = state.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0 // or this?

        val newMedia = groupedMedia.value.toList().toMutableList()
        newMedia.removeAt(scrollIndex)

        if (size == 0) {
            navController.popBackStack()
        } else {
            state.scrollToPage((scrollIndex).coerceIn(0, size))
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

//private fun LazyListState.itemIsVisible(item: MediaStoreData, list: List<MediaStoreData>) : Boolean {
//    if (list.isEmpty()) return false
//
//    return layoutInfo.visibleItemsInfo.map {
//        list[it.index]
//    }.contains(item)
//}
