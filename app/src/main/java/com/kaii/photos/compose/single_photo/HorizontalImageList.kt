package com.kaii.photos.compose.single_photo

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.setBarVisibility
import com.kaii.photos.compose.transformable
import com.kaii.photos.compose.videoplayer.VideoPlayer
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.SingleViewConstants
import com.kaii.photos.helpers.secureThumbnailImage
import com.kaii.photos.helpers.motion_photo.rememberMotionPhoto
import com.kaii.photos.helpers.paging.PhotoLibraryUIModel
import com.kaii.photos.helpers.scrolling.SinglePhotoScrollState
import com.kaii.photos.helpers.secureThumbnailImage
import com.kaii.photos.screens.video.retainVideoPlayerState
import com.kaii.photos.mediastore.ImmichInfo
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.SecureInfo
import com.kaii.photos.mediastore.getIv
import com.kaii.photos.mediastore.getThumbnailIv
import com.kaii.photos.mediastore.signature
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.ZoomableState
import me.saket.telephoto.zoomable.glide.ZoomableGlideImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState
import java.io.File

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun HorizontalImageList(
    items: LazyPagingItems<PhotoLibraryUIModel>,
    state: PagerState,
    scrollState: SinglePhotoScrollState,
    window: Window,
    appBarsVisible: MutableState<Boolean>,
    blurViews: Boolean,
    useBlackBackground: Boolean,
    useCache: Boolean,
    isSecuredMedia: Boolean = false
) {
    val windowSize = LocalWindowInfo.current.containerSize / 4

    val videoPlayerState = retainVideoPlayerState(
        isOpenWithView = false,
        onPlaybackStateChanged = {},
        onControlsTimeout = {
            appBarsVisible.value = false
        }
    )

    val context = LocalContext.current
    HorizontalPager(
        state = state,
        verticalAlignment = Alignment.CenterVertically,
        pageSpacing = 8.dp,
        // preload one page either side so the next/previous secure image starts decrypting before the
        // swipe settles. kept at 1 (not 5) since each secure neighbour is a full-file decrypt
        beyondViewportPageCount = 1,
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
        if (items[index] == null || items[index] !is PhotoLibraryUIModel.MediaImpl) return@HorizontalPager

        val zoomableState = rememberGlideZoomableState()

        if (state.settledPage != index) {
            LaunchedEffect(Unit) {
                zoomableState.resetZoom(
                    animationSpec = snap()
                )
            }
        }

        val media = items[index] as PhotoLibraryUIModel.MediaImpl

        val motionPhoto = rememberMotionPhoto(uri = media.item.uri.toUri())

        if (media.item.type == MediaType.Video) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (blurViews) {
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
                        model = when {
                            isSecuredMedia -> (media as PhotoLibraryUIModel.SecuredMedia).bytes?.let { bytes ->
                                SecureInfo(
                                    iv = bytes.getThumbnailIv(),
                                    absolutePath = File(media.item.absolutePath).secureThumbnailImage(context).absolutePath,
                                    key = media.signature()
                                )
                            }

                            media.item.isCloud -> ImmichInfo(
                                thumbnail = media.item.immichThumbnail!!,
                                original = media.item.immichUrl!!,
                                hash = media.item.hash!!,
                                auth = media.auth,
                                endpoint = media.endpoint!!,
                                useThumbnail = false
                            )

                            else -> media.item.uri
                        },
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

                VideoPlayer(
                    item = media.item,
                    auth = { media.auth },
                    endpoint = { media.endpoint ?: "" },
                    state = videoPlayerState,
                    appBarsVisible = appBarsVisible,
                    scrollState = scrollState,
                    window = window,
                    blurViews = blurViews,
                    useBlackBackground = useBlackBackground,
                    shouldPlay = {
                        state.currentPage == index
                    },
                    useCache = useCache,
                    modifier = Modifier
                        .fillMaxSize()
                        .transformable(),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize(1f)
            ) {
                val glideModel = remember(media.item.uri) {
                    when {
                        isSecuredMedia -> (media as PhotoLibraryUIModel.SecuredMedia).bytes?.let { bytes ->
                            SecureInfo(
                                iv = bytes.getIv(),
                                absolutePath = media.item.absolutePath,
                                key = media.signature()
                            )
                        }

                        media.item.isCloud -> ImmichInfo(
                            thumbnail = media.item.immichThumbnail!!,
                            original = media.item.immichUrl!!,
                            hash = media.item.hash!!,
                            auth = media.auth,
                            endpoint = media.endpoint!!,
                            useThumbnail = false
                        )

                        else -> media.item.uri
                    }
                }

                // cheap base layer for the secure viewer: the small pre-stored encrypted thumbnail (same
                // model the grid uses, so it's likely already in glide's memory cache). gated on a ready
                // (non-zero) iv and an existing file; null -> the full-file model is used as the base
                val glideThumbnailModel = remember(media) {
                    if (!isSecuredMedia) null
                    else (media as PhotoLibraryUIModel.SecuredMedia).bytes
                        ?.takeIf { it.size >= 32 }
                        ?.getThumbnailIv()
                        ?.takeIf { iv -> iv.any { b -> b.toInt() != 0 } }
                        ?.let { thumbIv ->
                            val thumbFile = File(media.item.absolutePath).secureThumbnailImage(context)
                            if (thumbFile.exists()) {
                                SecureInfo(
                                    iv = thumbIv,
                                    absolutePath = thumbFile.absolutePath,
                                    key = media.signature()
                                )
                            } else {
                                null
                            }
                        }
                }

                if (blurViews) {
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
                        item = media.item,
                        state = videoPlayerState,
                        zoomableState = zoomableState,
                        appBarsVisible = appBarsVisible,
                        window = window,
                        auth = { media.auth },
                        endpoint = { media.endpoint ?: "" },
                        shouldPlay = { state.currentPage == index },
                        blurViews = blurViews,
                        useBlackBackground = useBlackBackground,
                        glideImageView = @Composable { modifier ->
                            GlideView(
                                model = glideModel,
                                thumbnailModel = glideThumbnailModel,
                                item = media.item,
                                zoomableState = zoomableState,
                                window = window,
                                useCache = useCache,
                                appBarsVisible = appBarsVisible,
                                modifier = modifier,
                                disableSetBarVisibility = true
                            )
                        },
                    )
                } else {
                    GlideView(
                        model = glideModel,
                        thumbnailModel = glideThumbnailModel,
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
    isHidden: Boolean = false,
    thumbnailModel: Any? = null
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
        // never disk-cache secure media even if "cache thumbnails" is on: the disk cache would hold the
        // decrypted bytes as plaintext. the grid already hardcodes NONE for secure; mirror that here
        val isSecure = model is SecureInfo
        val request = it
            // stable per-item signature. the old ObjectKey(now()) branch (used when !useCache, the secure
            // default) changed every recomposition, busting the memory cache and forcing a full re-decrypt
            // -> the jitter / low-quality flash. disk caching is still governed by diskCacheStrategy below
            .signature(item.signature())
            .downsample(DownsampleStrategy.FIT_CENTER)
            .diskCacheStrategy(if (useCache && !isSecure) DiskCacheStrategy.ALL else DiskCacheStrategy.NONE)
            .error(
                when {
                    // secure full-image decode failed: fall back to the working thumbnail rather than
                    // the blank empty_image; broken_image only when there's no thumbnail
                    isHidden -> thumbnailModel ?: R.drawable.broken_image

                    model is ImmichInfo -> model.copy(useThumbnail = true)

                    else -> R.drawable.broken_image
                }
            )
            .thumbnail(
                Glide.with(context)
                    .load(
                        // prefer the cheap small thumbnail so the base layer isn't a second full-file
                        // decrypt; fall back to the full model when none was supplied
                        thumbnailModel
                            ?: if (model is ImmichInfo) model.copy(useThumbnail = true)
                            else model
                    )
                    .signature(item.signature())
                    .diskCacheStrategy(if (useCache && !isSecure) DiskCacheStrategy.ALL else DiskCacheStrategy.NONE)
                    .override(windowSize.width, windowSize.height)
            )
            .transition(withCrossFade(if (isHidden) 250 else 100))

        if (item.displayName.endsWith(".gif")) {
            request.decode(GifDrawable::class.java)
        } else {
            request
        }
    }
}