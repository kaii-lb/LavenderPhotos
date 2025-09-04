package com.kaii.photos.compose.single_photo.editing_view.video_editor

import android.app.Activity
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.view.Window
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.compose.app_bars.VideoEditorBottomBar
import com.kaii.photos.compose.app_bars.VideoEditorTopBar
import com.kaii.photos.compose.single_photo.editing_view.BasicVideoData
import com.kaii.photos.compose.single_photo.editing_view.CropBox
import com.kaii.photos.compose.single_photo.editing_view.CroppingAspectRatio
import com.kaii.photos.compose.single_photo.editing_view.FilterPager
import com.kaii.photos.compose.single_photo.editing_view.VideoEditorTabs
import com.kaii.photos.compose.single_photo.rememberExoPlayerWithLifeCycle
import com.kaii.photos.compose.single_photo.rememberPlayerView
import com.kaii.photos.datastore.Video
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.editing.ColorMatrixEffect
import com.kaii.photos.helpers.editing.MediaAdjustments
import com.kaii.photos.helpers.editing.MediaColorFilters
import com.kaii.photos.helpers.editing.VideoModification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "VIDEO_EDITOR"

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditor(
    uri: Uri,
    absolutePath: String,
    window: Window
) {
    val isPlaying = remember { mutableStateOf(false) }

    val mainViewModel = LocalMainViewModel.current
    val startMuted by mainViewModel.settings.Video.getMuteOnStart().collectAsStateWithLifecycle(initialValue = true)
    val isMuted = remember(startMuted) { mutableStateOf(startMuted) }

    /** In Seconds */
    val currentVideoPosition = remember { mutableFloatStateOf(0f) }
    val duration = remember { mutableFloatStateOf(0f) }

    val exoPlayer = rememberExoPlayerWithLifeCycle(
        videoSource = uri,
        absolutePath = absolutePath,
        isPlaying = isPlaying,
        duration = duration,
        currentVideoPosition = currentVideoPosition
    )

    val leftTrimPosition = remember { mutableFloatStateOf(0f) }
    val rightTrimPosition = remember { mutableFloatStateOf(duration.floatValue) }

    LaunchedEffect(duration.floatValue) {
        rightTrimPosition.floatValue = duration.floatValue
    }

    LaunchedEffect(isMuted.value) {
        if (isMuted.value) exoPlayer.volume = 0f
        else exoPlayer.volume = 1f
    }

    LaunchedEffect(leftTrimPosition.floatValue, currentVideoPosition.floatValue) {
        if (currentVideoPosition.floatValue * 1000 < leftTrimPosition.floatValue * 1000) {
            exoPlayer.seekTo((leftTrimPosition.floatValue * 1000).toLong())
            currentVideoPosition.floatValue = leftTrimPosition.floatValue

            if (isPlaying.value) exoPlayer.play()
            else exoPlayer.pause()
        }
    }

    LaunchedEffect(isPlaying.value) {
        if (!isPlaying.value) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            exoPlayer.pause()

            // so when the video is just starting it actually plays?
            if (currentVideoPosition.floatValue > leftTrimPosition.floatValue) exoPlayer.isScrubbingModeEnabled = true
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            exoPlayer.isScrubbingModeEnabled = false
            exoPlayer.play()
        }

        currentVideoPosition.floatValue = exoPlayer.currentPosition / 1000f
        val end = rightTrimPosition.floatValue * 1000
        val threshold = 150f
        val current = currentVideoPosition.floatValue * 1000
        if (current in (end - threshold)..(end + threshold) || current >= end) {
            delay(1000)
            exoPlayer.pause()
            exoPlayer.seekTo((leftTrimPosition.floatValue * 1000).toLong())
            currentVideoPosition.floatValue = leftTrimPosition.floatValue
            Log.d(TAG, "Ending video...")
            isPlaying.value = false
        }

        while (isPlaying.value) {
            currentVideoPosition.floatValue = exoPlayer.currentPosition / 1000f

            // again here since exoplayer doesn't know we might be ending video early
            // because of the way it works (this is easier)
            val end = rightTrimPosition.floatValue * 1000
            val threshold = 150f
            val current = currentVideoPosition.floatValue * 1000
            if (current in (end - threshold)..(end + threshold) || current >= end) {
                delay(1000)
                exoPlayer.pause()
                exoPlayer.seekTo((leftTrimPosition.floatValue * 1000).toLong())
                currentVideoPosition.floatValue = leftTrimPosition.floatValue
                Log.d(TAG, "Ending video...")
                isPlaying.value = false
            }

            delay(100)
        }
    }

    val modifications = remember { mutableStateListOf<VideoModification>() }
    val lastSavedModCount =
        remember { mutableIntStateOf(2) } // starts at 2 cuz of below LaunchedEffect, and then 1 more for setting rightPosition to duration

    LaunchedEffect(leftTrimPosition.floatValue, rightTrimPosition.floatValue) {
        modifications.removeAll { it is VideoModification.Trim } // lots of these, we don't need them, tho this is kinda gross
        modifications.add(
            VideoModification.Trim(
                start = leftTrimPosition.floatValue,
                end = rightTrimPosition.floatValue
            )
        )
    }

    val pagerState = rememberPagerState { 6 }
    var containerDimens by remember { mutableStateOf(Size.Zero) }
    val resetCrop = remember { mutableStateOf(false) }
    var isSeeking by remember { mutableStateOf(false) }

    var rotation by remember { mutableFloatStateOf(0f) }
    val aspectRatio = remember { mutableStateOf(CroppingAspectRatio.FreeForm) }
    var basicVideoData by remember {
        mutableStateOf(
            BasicVideoData(
                duration = exoPlayer.duration / 1000f,
                width = exoPlayer.videoSize.width,
                height = exoPlayer.videoSize.height,
                absolutePath = absolutePath,
                frameRate =
                    if (exoPlayer.videoFormat?.frameRate?.toInt() == -1 || exoPlayer.videoFormat?.frameRate == null) 0f
                    else exoPlayer.videoFormat!!.frameRate
            )
        )
    }

    var tries by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        val videoFormat = exoPlayer.videoFormat

        withContext(Dispatchers.IO) {
            val metadata = MediaMetadataRetriever()
            metadata.setDataSource(absolutePath)

            // this mess is because exoplayer doesn't really know what res the video is all the time
            val frame = metadata.frameAtTime
            val size = if (frame == null) {
                if (exoPlayer.videoSize == VideoSize.UNKNOWN) {
                    VideoSize(
                        exoPlayer.videoFormat?.width ?: 1,
                        exoPlayer.videoFormat?.height ?: 1
                    )
                } else {
                    exoPlayer.videoSize
                }
            } else {
                VideoSize(frame.width, frame.height)
            }

            while ((basicVideoData.width == 0 || basicVideoData.height == 0 || basicVideoData.frameRate == 0f || basicVideoData.duration <= 0f)
                && tries < 10
            ) {
                basicVideoData =
                    BasicVideoData(
                        duration = duration.floatValue,
                        frameRate =
                            if (videoFormat?.frameRate?.toInt() == -1 || videoFormat?.frameRate == null) 0f
                            else videoFormat.frameRate,
                        absolutePath = absolutePath,
                        width = size.width,
                        height = size.height
                    )
                tries += 1

                Log.d(TAG, "Video data $basicVideoData")
                delay(100)
            }
        }
    }

    // prefill for order of application
    val effectsList = remember {
        mutableStateListOf<Effect?>().apply {
            (1..MediaAdjustments.entries.size).forEach {
                add(null)
            }
        }
    }

    val totalModCount = remember { mutableIntStateOf(0) }
    LaunchedEffect(totalModCount.intValue) {
        val last = modifications.lastOrNull()

        if (last is VideoModification.Adjustment) {
            // cuz order of application matters, so just do this and have it be constant
            val effectIndex = MediaAdjustments.entries.indexOf(last.type)
            effectsList.add(effectIndex, last.toEffect())
            effectsList.removeAt(effectIndex + 1) // remove null after shirting right
        }

        exoPlayer.stop()
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .build()

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.setVideoEffects(effectsList.mapNotNull { it })
        exoPlayer.prepare()

        Log.d(TAG, "Effect list $effectsList")
    }

    val coroutineScope = rememberCoroutineScope()
    val filterPagerState = rememberPagerState(
        initialPage = MediaColorFilters.entries.indexOf(
            (modifications.lastOrNull {
                it is VideoModification.Filter
            } as? VideoModification.Filter)?.type ?: MediaColorFilters.None
        )
    ) { MediaColorFilters.entries.size }

    Scaffold(
        topBar = {
            VideoEditorTopBar(
                uri = uri,
                absolutePath = absolutePath,
                modifications = modifications,
                effectsList = effectsList.mapNotNull { it },
                lastSavedModCount = lastSavedModCount,
                containerDimens = containerDimens,
                videoDimens = IntSize(
                    width = basicVideoData.width,
                    height = basicVideoData.height
                )
            )
        },
        bottomBar = {
            VideoEditorBottomBar(
                pagerState = pagerState,
                currentPosition = currentVideoPosition,
                leftPosition = leftTrimPosition,
                rightPosition = rightTrimPosition,
                basicData = basicVideoData,
                croppingAspectRatio = aspectRatio,
                modifications = modifications,
                onCropReset = {
                    resetCrop.value = true
                    rotation = 0f
                    aspectRatio.value = CroppingAspectRatio.FreeForm

                    modifications.add(
                        VideoModification.Rotation(
                            degrees = 0f
                        )
                    )
                },
                onSeek = { pos ->
                    val wasPlaying = isPlaying.value
                    exoPlayer.seekTo(
                        (pos * 1000f).coerceAtMost(rightTrimPosition.floatValue * 1000f).toLong()
                    )
                    isPlaying.value = wasPlaying
                },
                onRotate = {
                    rotation += 90f
                    modifications.add(
                        VideoModification.Rotation(
                            degrees = rotation
                        )
                    )
                },
                increaseModCount = {
                    totalModCount.intValue += 1
                },
                saveEffect = { filter ->
                    effectsList.removeAll {
                        it is ColorMatrixEffect && it.isFilter
                    }

                    effectsList.add(
                        ColorMatrixEffect(
                            matrix = filter.matrix,
                            isFilter = true
                        )
                    )

                    totalModCount.intValue += 1

                    coroutineScope.launch {
                        filterPagerState.animateScrollToPage(
                            MediaColorFilters.entries.indexOf(filter),
                            animationSpec = tween(
                                durationMillis = AnimationConstants.DURATION
                            )
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            val animatedRotation by animateFloatAsState(
                targetValue = rotation
            )

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(1f)
                    .padding(8.dp)
            ) {
                val context = LocalContext.current
                val playerView = rememberPlayerView(
                    exoPlayer = exoPlayer,
                    activity = context as Activity,
                    absolutePath = absolutePath,
                    useTextureView = true
                )

                val width by animateDpAsState(
                    targetValue = if (rotation % 180 == 0f) this@BoxWithConstraints.maxWidth else this@BoxWithConstraints.maxHeight
                )
                val height by animateDpAsState(
                    targetValue = if (rotation % 180f == 0f) this@BoxWithConstraints.maxHeight else this@BoxWithConstraints.maxWidth
                )

                Box(
                    modifier = Modifier
                        .width(width)
                        .height(height)
                        .padding(8.dp)
                        .rotate(animatedRotation)
                        .align(Alignment.Center)
                ) {
                    val isInFilterPage by remember {
                        derivedStateOf {
                            pagerState.currentPage == VideoEditorTabs.entries.indexOf(VideoEditorTabs.Filters)
                        }
                    }

                    AnimatedContent(
                        targetState = isInFilterPage,
                        transitionSpec = {
                            (slideInHorizontally {
                                if (isInFilterPage) it
                                else -it
                            } + fadeIn()
                                    ).togetherWith(
                                    (slideOutHorizontally {
                                        if (isInFilterPage) -it
                                        else it
                                    } + fadeOut())
                                )
                        }
                    ) { state ->
                        if (state) {
                            FilterPager(
                                pagerState = filterPagerState,
                                modifications = modifications,
                                currentVideoPosition = currentVideoPosition,
                                absolutePath = absolutePath,
                                allowedToRefresh = isPlaying.value || isSeeking
                            )
                        } else {
                            AndroidView(
                                factory = {
                                    playerView
                                },
                                modifier = Modifier
                                    .fillMaxSize(1f)
                                    .align(Alignment.Center)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .width(width)
                        .height(height)
                        .rotate(animatedRotation)
                        .align(Alignment.Center),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedVisibility(
                        visible = pagerState.currentPage == 1,
                        enter = fadeIn(
                            animationSpec = AnimationConstants.expressiveTween()
                        ),
                        exit = fadeOut(
                            animationSpec = AnimationConstants.expressiveTween()
                        ),
                        modifier = Modifier
                            .requiredSize(
                                width = width + 16.dp, // as to not clip the CropBox arcs
                                height = height + 16.dp
                            )
                    ) {
                        val localDensity = LocalDensity.current

                        CropBox(
                            containerWidth = with(localDensity) { width.toPx() - 16.dp.toPx() }, // adjust for AnimatedVisibility size
                            containerHeight = with(localDensity) { height.toPx() - 16.dp.toPx() },
                            mediaAspectRatio = basicVideoData.aspectRatio,
                            reset = resetCrop,
                            aspectRatio = aspectRatio,
                            modifier = Modifier
                                .offset(16.dp, 16.dp) // adjust for top-left change from AnimatedVisibility
                        ) { area, original ->
                            modifications.removeAll { it is VideoModification.Crop } // because Crop gets called a million times each movement
                            modifications.add(
                                VideoModification.Crop(
                                    top = area.top,
                                    left = area.left,
                                    width = area.width,
                                    height = area.height
                                )
                            )

                            containerDimens = original
                        }
                    }
                }
            }

            VideoEditorBottomTools(
                pagerState = pagerState,
                modifications = modifications,
                isPlaying = isPlaying,
                isMuted = isMuted,
                currentPosition = currentVideoPosition,
                duration = duration.floatValue,
                totalModCount = totalModCount,
                setPlaybackSpeed = { speed ->
                    exoPlayer.setPlaybackSpeed(speed)
                },
                onSeek = { pos ->
                    isSeeking = true
                    val wasPlaying = isPlaying.value

                    exoPlayer.seekTo(
                        (pos * 1000f).coerceAtMost(duration.floatValue * 1000f).toLong()
                    )

                    isPlaying.value = wasPlaying
                },
                onSeekFinished = {
                    isSeeking = false
                }
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
