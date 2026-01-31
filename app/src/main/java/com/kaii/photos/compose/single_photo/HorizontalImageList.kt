package com.kaii.photos.compose.single_photo

import android.view.Window
import androidx.compose.animation.core.snap
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.setBarVisibility
import com.kaii.photos.compose.transformable
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.helpers.SingleViewConstants
import com.kaii.photos.helpers.motion_photo.rememberMotionPhoto
import com.kaii.photos.helpers.motion_photo.rememberMotionPhotoState
import com.kaii.photos.helpers.scrolling.SinglePhotoScrollState
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.PhotoLibraryUIModel
import com.kaii.photos.mediastore.signature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.ZoomableState
import me.saket.telephoto.zoomable.glide.ZoomableGlideImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState
import kotlin.math.abs

// private const val TAG = "com.kaii.photos.compose.single_photo.HorizontalImageList"

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun HorizontalImageList(
    groupedMedia: List<PhotoLibraryUIModel.Media>,
    state: PagerState,
    scrollState: SinglePhotoScrollState,
    window: Window,
    appBarsVisible: MutableState<Boolean>,
    isHidden: Boolean = false
) {
    val videoAutoplay by scrollState.videoAutoplay.collectAsStateWithLifecycle()

    HorizontalPager(
        state = state,
        verticalAlignment = Alignment.CenterVertically,
        pageSpacing = 8.dp,
        // beyondViewportPageCount = 5, // TODO: check this
        key = {
            if (groupedMedia.isNotEmpty() && it <= groupedMedia.size - 1) {
                val neededItem = groupedMedia[it].item
                neededItem.uri
            } else {
                System.currentTimeMillis()
                    .toString() // this should be unique enough in case of failure right?
            }
        },
        snapPosition = SnapPosition.Center,
        userScrollEnabled = !scrollState.privacyMode && !scrollState.videoLock,
        modifier = Modifier
            .fillMaxHeight(1f)
    ) { index ->
        if (groupedMedia.isEmpty()) return@HorizontalPager

        val zoomableState = rememberGlideZoomableState()

        if (state.settledPage != index) {
            LaunchedEffect(Unit) {
                zoomableState.resetZoom(
                    animationSpec = snap()
                )
            }
        }

        val shouldPlay = remember(state) {
            derivedStateOf {
                (abs(state.currentPageOffsetFraction) < 0.5f && state.currentPage == index)
                        || (abs(state.currentPageOffsetFraction) > 0.5f && state.currentPage == index)
            }
        }

        val mediaStoreItem = groupedMedia[index].item

        val motionPhoto = rememberMotionPhoto(uri = mediaStoreItem.uri.toUri())

        if (mediaStoreItem.type == MediaType.Video) {
            Box(
                modifier = Modifier
                    .fillMaxSize(1f)
            ) {
                VideoPlayer(
                    item = mediaStoreItem,
                    appBarsVisible = appBarsVisible,
                    shouldAutoPlay = videoAutoplay,
                    scrollState = scrollState,
                    window = window,
                    shouldPlay = shouldPlay,
                    modifier = Modifier
                        .fillMaxSize(1f)
                        .transformable()
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize(1f)
            ) {
                // TODO
                // val context = LocalContext.current
                // var model by remember { mutableStateOf<Any?>(null) }

                // LaunchedEffect(isHidden) {
                //     if (!isHidden || model != null) return@LaunchedEffect
                //
                //     withContext(Dispatchers.IO) {
                //         try {
                //             val iv = mediaStoreItem.bytes!!.getIv()
                //             val thumbnailIv = mediaStoreItem.bytes.getThumbnailIv()
                //             val thumbnailFile = getSecuredCacheImageForFile(
                //                 fileName = mediaStoreItem.displayName,
                //                 context = context
                //             )
                //
                //             if (thumbnailFile.length() > 1024*1024*10) { // don't decrypt thumbnail if file will load instantly anyway
                //                 model = EncryptionManager.decryptBytes(
                //                     bytes = thumbnailFile.readBytes(),
                //                     iv = thumbnailIv
                //                 )
                //             }
                //
                //             model = EncryptionManager.decryptBytes(
                //                 bytes = File(mediaStoreItem.absolutePath).readBytes(),
                //                 iv = iv
                //             )
                //         } catch (e: Throwable) {
                //             Log.d(TAG, e.toString())
                //             e.printStackTrace()
                //
                //             mediaStoreItem.uri.path
                //         }
                //     }
                // }

                if (motionPhoto.isMotionPhoto.value) {
                    MotionPhotoView(
                        state = rememberMotionPhotoState(uri = motionPhoto.uri),
                        zoomableState = zoomableState,
                        appBarsVisible = appBarsVisible,
                        window = window,
                        glideImageView = @Composable { modifier ->
                            GlideView(
                                model = mediaStoreItem.uri, // TODO if (isHidden) model else mediaStoreItem.uri,
                                item = mediaStoreItem,
                                zoomableState = zoomableState,
                                window = window,
                                appBarsVisible = appBarsVisible,
                                modifier = modifier,
                                disableSetBarVisibility = true
                            )
                        }
                    )
                } else {
                    GlideView(
                        model = mediaStoreItem.uri, // TODO if (isHidden) model else mediaStoreItem.uri,
                        item = mediaStoreItem,
                        zoomableState = zoomableState,
                        window = window,
                        appBarsVisible = appBarsVisible,
                        isHidden = isHidden,
                        modifier = Modifier
                            .align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
fun rememberGlideZoomableState(): ZoomableState {
    return rememberZoomableState(
        zoomSpec = ZoomSpec(
            maxZoomFactor = SingleViewConstants.MAX_ZOOM
        )
    )
}

@Composable
fun GlideView(
    model: Any?,
    item: MediaStoreData,
    zoomableState: ZoomableState,
    window: Window,
    appBarsVisible: MutableState<Boolean>,
    modifier: Modifier = Modifier,
    useCache: Boolean = true,
    disableSetBarVisibility: Boolean = false,
    isHidden: Boolean = false
) {
    val context = LocalContext.current
    val state = rememberZoomableImageState(zoomableState = zoomableState)
    val windowSize = LocalWindowInfo.current.containerSize / 4

    ZoomableGlideImage(
        model = model,
        contentDescription = item.displayName,
        state = state,
        onClick = {
            if (!disableSetBarVisibility) {
                setBarVisibility(
                    visible = !appBarsVisible.value,
                    window = window
                ) {
                    appBarsVisible.value = it
                }
            }
        },
        onDoubleClick = { zoomState, offset ->
            val zoom = zoomState.zoomFraction ?: 0f

            if (zoom == 0f) {
                zoomState.zoomBy(
                    zoomFactor = SingleViewConstants.HALF_ZOOM,
                    centroid = offset
                )
            } else {
                zoomState.resetZoom()
            }
        },
        modifier = modifier
            .fillMaxSize()
    ) {
        it.signature(item.signature())
            .diskCacheStrategy(if (useCache) DiskCacheStrategy.ALL else DiskCacheStrategy.NONE)
            .downsample(DownsampleStrategy.FIT_CENTER)
            .error(if (isHidden) R.drawable.empty_image else R.drawable.broken_image)
            .thumbnail(
                Glide.with(context)
                    .load(model)
                    .signature(item.signature())
                    .diskCacheStrategy(if (useCache) DiskCacheStrategy.ALL else DiskCacheStrategy.NONE)
                    .override(windowSize.width, windowSize.height)
            )
            .transition(withCrossFade(if (isHidden) 250 else 100))
    }
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
