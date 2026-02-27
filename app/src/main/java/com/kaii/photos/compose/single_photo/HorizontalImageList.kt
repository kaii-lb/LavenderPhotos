package com.kaii.photos.compose.single_photo

import android.util.Log
import android.view.Window
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.setBarVisibility
import com.kaii.photos.compose.transformable
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.EncryptionManager
import com.kaii.photos.helpers.SingleViewConstants
import com.kaii.photos.helpers.getSecuredCacheImageForFile
import com.kaii.photos.helpers.motion_photo.rememberMotionPhoto
import com.kaii.photos.helpers.motion_photo.rememberMotionPhotoState
import com.kaii.photos.helpers.paging.PhotoLibraryUIModel
import com.kaii.photos.helpers.scrolling.SinglePhotoScrollState
import com.kaii.photos.mediastore.ImmichInfo
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.getIv
import com.kaii.photos.mediastore.getThumbnailIv
import com.kaii.photos.mediastore.signature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.ZoomableState
import me.saket.telephoto.zoomable.glide.ZoomableGlideImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState
import java.io.File
import kotlin.math.abs

private const val TAG = "com.kaii.photos.compose.single_photo.HorizontalImageList"

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun HorizontalImageList(
    items: LazyPagingItems<PhotoLibraryUIModel>,
    state: PagerState,
    scrollState: SinglePhotoScrollState,
    window: Window,
    appBarsVisible: MutableState<Boolean>,
    isSecuredMedia: Boolean = false
) {
    val mainViewModel = LocalMainViewModel.current
    val windowSize = LocalWindowInfo.current.containerSize / 4

    val videoAutoplay by scrollState.videoAutoplay.collectAsStateWithLifecycle()
    val useCache by mainViewModel.settings.storage.getCacheThumbnails().collectAsStateWithLifecycle(initialValue = true)
    val blurBackground by mainViewModel.blurViews.collectAsStateWithLifecycle()

    HorizontalPager(
        state = state,
        verticalAlignment = Alignment.CenterVertically,
        pageSpacing = 8.dp,
        // beyondViewportPageCount = 5, // TODO: check this
        key = items.itemKey { it.itemKey() },
        snapPosition = SnapPosition.Center,
        userScrollEnabled = !scrollState.privacyMode && !scrollState.videoLock,
        modifier = Modifier
            .fillMaxHeight(1f)
            .semantics {
                testTagsAsResourceId = true
            }
            .testTag("single_photo_horizontal_pager")
    ) { index ->
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

        val media = items[index] as PhotoLibraryUIModel.MediaImpl

        val motionPhoto = rememberMotionPhoto(uri = media.item.uri.toUri())

        if (media.item.type == MediaType.Video) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (blurBackground) {
                    var targetAlpha by remember { mutableFloatStateOf(0f) }
                    val animatedAlpha by animateFloatAsState(
                        targetValue = targetAlpha,
                        animationSpec = tween(
                            durationMillis = AnimationConstants.DURATION
                        )
                    )

                    LaunchedEffect(Unit) {
                        targetAlpha = 0.5f
                    }

                    GlideImage(
                        model = media.item.uri.toUri(),
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                        loading = placeholder(R.drawable.broken_image),
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(48.dp)
                            .alpha(animatedAlpha)
                    ) {
                        it.signature(media.signature())
                            .diskCacheStrategy(if (useCache) DiskCacheStrategy.ALL else DiskCacheStrategy.NONE)
                            .override(windowSize.width, windowSize.height)
                    }
                }

                // TODO: handle immich
                VideoPlayer(
                    item = media.item,
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
                // TODO: possibly move to a less messy and horrible decrypting implementation
                val context = LocalContext.current
                var model by remember { mutableStateOf<Any?>(null) }

                LaunchedEffect(isSecuredMedia) {
                    if (!isSecuredMedia || model != null) return@LaunchedEffect

                    withContext(Dispatchers.IO) {
                        try {
                            media as PhotoLibraryUIModel.SecuredMedia

                            val iv = media.bytes!!.getIv()
                            val thumbnailIv = media.bytes.getThumbnailIv()
                            val thumbnailFile = getSecuredCacheImageForFile(
                                fileName = media.item.displayName,
                                context = context
                            )

                            if (thumbnailFile.length() > 1024 * 1024 * 10) { // don't decrypt thumbnail if file will load instantly anyway
                                model = EncryptionManager.decryptBytes(
                                    bytes = thumbnailFile.readBytes(),
                                    iv = thumbnailIv
                                )
                            }

                            model = EncryptionManager.decryptBytes(
                                bytes = File(media.item.absolutePath).readBytes(),
                                iv = iv
                            )
                        } catch (e: Throwable) {
                            Log.d(TAG, e.toString())
                            e.printStackTrace()

                            media.item.uri.toUri().path
                        }
                    }
                }

                val glideModel =
                    when {
                        isSecuredMedia -> model

                        media.item.immichUrl != null -> ImmichInfo(
                            thumbnail = media.item.immichThumbnail!!,
                            original = media.item.immichUrl!!,
                            hash = media.item.hash!!,
                            accessToken = media.accessToken!!,
                            useThumbnail = false
                        )

                        else -> media.item.uri
                    }

                if (blurBackground) {
                    var targetAlpha by remember { mutableFloatStateOf(0f) }
                    val animatedAlpha by animateFloatAsState(
                        targetValue = targetAlpha,
                        animationSpec = tween(
                            durationMillis = AnimationConstants.DURATION
                        )
                    )

                    LaunchedEffect(Unit) {
                        targetAlpha = 0.5f
                    }

                    GlideImage(
                        model = glideModel,
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(48.dp)
                            .alpha(animatedAlpha)
                    ) {
                        it.signature(media.signature())
                            .diskCacheStrategy(if (useCache) DiskCacheStrategy.ALL else DiskCacheStrategy.NONE)
                            .override(windowSize.width, windowSize.height)
                    }
                }

                if (motionPhoto.isMotionPhoto.value) {
                    MotionPhotoView(
                        state = rememberMotionPhotoState(uri = motionPhoto.uri),
                        zoomableState = zoomableState,
                        appBarsVisible = appBarsVisible,
                        window = window,
                        glideImageView = @Composable { modifier ->
                            GlideView(
                                model = glideModel,
                                item = media.item,
                                zoomableState = zoomableState,
                                window = window,
                                useCache = useCache,
                                appBarsVisible = appBarsVisible,
                                modifier = modifier,
                                disableSetBarVisibility = true
                            )
                        }
                    )
                } else {
                    GlideView(
                        model = glideModel,
                        item = media.item,
                        zoomableState = zoomableState,
                        window = window,
                        useCache = useCache,
                        appBarsVisible = appBarsVisible,
                        isHidden = isSecuredMedia,
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
    useCache: Boolean,
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