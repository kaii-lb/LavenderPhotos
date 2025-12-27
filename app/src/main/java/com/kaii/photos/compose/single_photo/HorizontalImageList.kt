package com.kaii.photos.compose.single_photo

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import android.view.Window
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.github.panpf.zoomimage.CoilZoomAsyncImage
import com.github.panpf.zoomimage.CoilZoomState
import com.github.panpf.zoomimage.compose.coil.CoilComposeSubsamplingImageGenerator
import com.github.panpf.zoomimage.compose.rememberZoomImageLogger
import com.github.panpf.zoomimage.compose.subsampling.rememberSubsamplingState
import com.github.panpf.zoomimage.compose.zoom.ScrollBarSpec
import com.github.panpf.zoomimage.compose.zoom.ZoomableState
import com.github.panpf.zoomimage.compose.zoom.rememberZoomableState
import com.github.panpf.zoomimage.util.Logger
import com.github.panpf.zoomimage.zoom.ScalesCalculator
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.setBarVisibility
import com.kaii.photos.compose.transformable
import com.kaii.photos.datastore.Video
import com.kaii.photos.helpers.EncryptionManager
import com.kaii.photos.helpers.SingleViewConstants
import com.kaii.photos.helpers.getSecuredCacheImageForFile
import com.kaii.photos.helpers.motion_photo.rememberMotionPhoto
import com.kaii.photos.helpers.motion_photo.rememberMotionPhotoState
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.getIv
import com.kaii.photos.mediastore.getThumbnailIv
import com.kaii.photos.mediastore.stringSignature
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

private const val TAG = "com.kaii.photos.compose.single_photo.HorizontalImageList"

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun HorizontalImageList(
    groupedMedia: List<MediaStoreData>,
    state: PagerState,
    window: Window,
    appBarsVisible: MutableState<Boolean>,
    isHidden: Boolean = false,
    isOpenWithView: Boolean = false
) {
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

    // Log.d(TAG, "CURRENT OFFSET ${zoomableState.contentTransformation.centroid} ${zoomableState.contentTransformation.offset}")

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
        userScrollEnabled = !isTouchLocked.value,
        modifier = Modifier
            .fillMaxHeight(1f)
    ) { index ->
        if (groupedMedia.isEmpty()) return@HorizontalPager

        val zoomableState = rememberZoomableState().apply {
            setScalesCalculator(
                ScalesCalculator.dynamic(
                    multiple = SingleViewConstants.MAX_ZOOM
                )
            )
        }

        if (state.settledPage != index) {
            LaunchedEffect(Unit) {
                zoomableState.reset()
            }
        }

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
                        .transformable()
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

                val motionPhoto = rememberMotionPhoto(uri = mediaStoreItem.uri)

                if (motionPhoto.isMotionPhoto.value) {
                    MotionPhotoView(
                        state = rememberMotionPhotoState(uri = motionPhoto.uri),
                        zoomableState = zoomableState,
                        glideImageView = @Composable { modifier ->
                            GlideView(
                                model = if (isHidden) model else mediaStoreItem.uri,
                                mediaStoreItem = mediaStoreItem,
                                zoomableState = zoomableState,
                                window = window,
                                appBarsVisible = appBarsVisible,
                                modifier = modifier
                            )
                        }
                    )
                } else {
                    GlideView(
                        model = if (isHidden) model else mediaStoreItem.uri,
                        mediaStoreItem = mediaStoreItem,
                        zoomableState = zoomableState,
                        window = window,
                        appBarsVisible = appBarsVisible
                    )
                }
            }
        }
    }
}

@Composable
fun rememberCoilZoomState(
    zoomableState: ZoomableState,
    subsamplingImageGenerators: ImmutableList<CoilComposeSubsamplingImageGenerator>? = null,
    logLevel: Logger.Level? = null,
): CoilZoomState {
    val logger: Logger = rememberZoomImageLogger(tag = "CoilZoomAsyncImage", level = logLevel)
    val subsamplingState = rememberSubsamplingState(zoomableState)
    return remember(logger, zoomableState, subsamplingState, subsamplingImageGenerators) {
        CoilZoomState(logger, zoomableState, subsamplingState, subsamplingImageGenerators)
    }
}

@Composable
fun GlideView(
    model: Any?,
    mediaStoreItem: MediaStoreData,
    zoomableState: ZoomableState,
    window: Window,
    appBarsVisible: MutableState<Boolean>,
    modifier: Modifier = Modifier,
    useCache: Boolean = true
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val zoomState = rememberCoilZoomState(zoomableState)

    CoilZoomAsyncImage(
        model =
            ImageRequest.Builder(context)
                .data(model)
                .crossfade(true)
                .allowHardware(true)
                .diskCachePolicy(if (useCache) CachePolicy.ENABLED else CachePolicy.DISABLED)
                .diskCacheKey(mediaStoreItem.stringSignature())
                .memoryCacheKey(mediaStoreItem.stringSignature())
                .listener(
                    onSuccess = { _, image ->
                        val activity = (context as Activity)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && activity.display.isHdr) {
                            val isHdr = image.image.toBitmap(width = 1, height = 1).hasGainmap()
                            coroutineScope.launch(Dispatchers.Main) {
                                if (isHdr) {
                                    window.colorMode = ActivityInfo.COLOR_MODE_HDR
                                } else {
                                    window.colorMode = ActivityInfo.COLOR_MODE_DEFAULT
                                }
                            }
                        }
                    },
                    onError = { _, _ ->
                        coroutineScope.launch(Dispatchers.Main) {
                            window.colorMode = ActivityInfo.COLOR_MODE_DEFAULT
                        }
                    }
                )
                .build(),
        contentDescription = mediaStoreItem.displayName,
        contentScale = ContentScale.Fit,
        alignment = Alignment.Center,
        fallback = painterResource(R.drawable.broken_image),
        zoomState = zoomState,
        scrollBar = ScrollBarSpec(Color.Transparent, 0.dp, 0.dp),
        modifier = modifier
            .fillMaxSize(),
        onTap = {
            setBarVisibility(
                visible = !appBarsVisible.value,
                window = window
            ) {
                appBarsVisible.value = it
            }
        }
    )
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
