package com.kaii.photos.compose.editing_view.video_editor

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
import androidx.compose.animation.core.animateOffsetAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.FrameDropEffect
import androidx.media3.exoplayer.ExoPlayer
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.VideoEditorBottomBar
import com.kaii.photos.compose.app_bars.VideoEditorTopBar
import com.kaii.photos.compose.dialogs.TextEntryDialog
import com.kaii.photos.compose.editing_view.CropBox
import com.kaii.photos.compose.editing_view.VideoFilterPage
import com.kaii.photos.compose.editing_view.makeVideoDrawCanvas
import com.kaii.photos.compose.single_photo.rememberExoPlayerWithLifeCycle
import com.kaii.photos.compose.single_photo.rememberPlayerView
import com.kaii.photos.compose.widgets.shimmerEffect
import com.kaii.photos.datastore.Video
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.editing.BasicVideoData
import com.kaii.photos.helpers.editing.ColorMatrixEffect
import com.kaii.photos.helpers.editing.DrawableText
import com.kaii.photos.helpers.editing.MediaAdjustments
import com.kaii.photos.helpers.editing.MediaColorFilters
import com.kaii.photos.helpers.editing.VideoEditorTabs
import com.kaii.photos.helpers.editing.VideoModification
import com.kaii.photos.helpers.editing.applyEffects
import com.kaii.photos.helpers.editing.rememberDrawingPaintState
import com.kaii.photos.helpers.editing.rememberVideoEditingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

private const val TAG = "com.kaii.photos.compose.editing_view.VideoEditor"

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
    var exoPlayerLoading by remember { mutableStateOf(true) }

    val exoPlayer = rememberExoPlayerWithLifeCycle(
        videoSource = uri,
        absolutePath = absolutePath,
        isPlaying = isPlaying,
        duration = duration,
        currentVideoPosition = currentVideoPosition
    ) { playbackState ->
        if (exoPlayerLoading) {
            exoPlayerLoading = playbackState == ExoPlayer.STATE_BUFFERING
        }
    }

    val videoEditingState = rememberVideoEditingState(
        duration = duration.floatValue
    )
    val drawingPaintState = rememberDrawingPaintState(
        isVideo = true
    )

    LaunchedEffect(isMuted.value) {
        if (isMuted.value) exoPlayer.volume = 0f
        else exoPlayer.volume = 1f
    }

    LaunchedEffect(videoEditingState.startTrimPosition, currentVideoPosition.floatValue) {
        if (currentVideoPosition.floatValue * 1000 < videoEditingState.startTrimPosition * 1000) {
            exoPlayer.seekTo((videoEditingState.startTrimPosition * 1000).toLong())
            currentVideoPosition.floatValue = videoEditingState.startTrimPosition

            if (isPlaying.value) exoPlayer.play()
            else exoPlayer.pause()
        }
    }

    LaunchedEffect(isPlaying.value) {
        if (!isPlaying.value) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            exoPlayer.pause()

            // so when the video is just starting it actually plays?
            if (currentVideoPosition.floatValue > videoEditingState.startTrimPosition) exoPlayer.isScrubbingModeEnabled = true
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            exoPlayer.isScrubbingModeEnabled = false
            exoPlayer.play()
        }

        currentVideoPosition.floatValue = exoPlayer.currentPosition / 1000f
        val end = videoEditingState.endTrimPosition * 1000
        val threshold = 150f
        val current = currentVideoPosition.floatValue * 1000
        if (current in (end - threshold)..(end + threshold) || current >= end) {
            delay(1000)
            exoPlayer.pause()
            exoPlayer.seekTo((videoEditingState.startTrimPosition * 1000).toLong())
            currentVideoPosition.floatValue = videoEditingState.startTrimPosition
            Log.d(TAG, "Ending video...")
            isPlaying.value = false
        }

        while (isPlaying.value) {
            currentVideoPosition.floatValue = exoPlayer.currentPosition / 1000f

            // again here since exoplayer doesn't know we might be ending video early
            // because of the way it works (this is easier)
            val end = videoEditingState.endTrimPosition * 1000
            val threshold = 150f
            val current = currentVideoPosition.floatValue * 1000
            if (current in (end - threshold)..(end + threshold) || current >= end) {
                delay(1000)
                exoPlayer.pause()
                exoPlayer.seekTo((videoEditingState.startTrimPosition * 1000).toLong())
                currentVideoPosition.floatValue = videoEditingState.startTrimPosition
                Log.d(TAG, "Ending video...")
                isPlaying.value = false
            }

            delay(100)
        }
    }

    val modifications = remember { mutableStateListOf<VideoModification>() }
    val lastSavedModCount =
        remember { mutableIntStateOf(2) } // starts at 2 cuz of below LaunchedEffect, and then 1 more for setting rightPosition to duration

    LaunchedEffect(videoEditingState.startTrimPosition, videoEditingState.endTrimPosition) {
        modifications.removeAll { it is VideoModification.Trim } // lots of these, we don't need them, tho this is kinda gross
        modifications.add(
            VideoModification.Trim(
                start = videoEditingState.startTrimPosition,
                end = videoEditingState.endTrimPosition
            )
        )
    }

    LaunchedEffect(videoEditingState.speed) {
        exoPlayer.setPlaybackSpeed(videoEditingState.speed)
    }

    val pagerState = rememberPagerState { VideoEditorTabs.entries.size }
    var containerDimens by remember { mutableStateOf(Size.Zero) }
    var isSeeking by remember { mutableStateOf(false) }

    var basicVideoData by remember {
        mutableStateOf(
            BasicVideoData(
                duration = exoPlayer.duration / 1000f,
                width = exoPlayer.videoSize.width,
                height = exoPlayer.videoSize.height,
                absolutePath = absolutePath,
                bitrate = exoPlayer.videoFormat?.bitrate ?: 0,
                frameRate =
                    if (exoPlayer.videoFormat?.frameRate?.toInt() == -1 || exoPlayer.videoFormat?.frameRate == null) 0f
                    else exoPlayer.videoFormat!!.frameRate
            )
        )
    }

    Log.d(TAG, "basic video data $basicVideoData")

    LaunchedEffect(duration.floatValue) {
        val videoFormat = exoPlayer.videoFormat
        var tries = 0

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

            while ((basicVideoData.duration <= 0f || basicVideoData.frameRate == 0f) && tries < 10) {
                val frameRate = if (videoFormat?.frameRate == null || videoFormat.frameRate.toInt() == -1) {
                    val possible = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloat()

                    if (possible == null) {
                        val frameCount = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)?.toFloat() ?: 0f

                        frameCount / duration.floatValue
                    } else {
                        possible
                    }
                } else {
                    videoFormat.frameRate
                }

                videoEditingState.setFrameRate(frameRate) // important!!!

                val bitrate = if (videoFormat?.bitrate == null || videoFormat.bitrate == -1) {
                    val possible = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toInt()

                    possible ?: -1 // -1 basically give up and go for highest
                } else {
                    videoFormat.bitrate
                }

                videoEditingState.setBitrate(bitrate * 2) // * 2 since with markup and effects and stuff the normal simply isn't enough

                basicVideoData =
                    BasicVideoData(
                        duration = duration.floatValue,
                        frameRate = frameRate,
                        absolutePath = absolutePath,
                        bitrate = bitrate,
                        width = size.width,
                        height = size.height
                    )
                tries += 1

                Log.d(TAG, "Video data $basicVideoData")
                delay(100)
            }
        }
    }

    LaunchedEffect(videoEditingState.volume) {
        exoPlayer.volume = videoEditingState.volume
    }

    LaunchedEffect(videoEditingState.frameRate) {
        if (videoEditingState.frameRate == basicVideoData.frameRate || videoEditingState.frameRate <= 0f) {
            videoEditingState.removeAllEffects {
                it is FrameDropEffect
            }
        } else {
            videoEditingState.addEffect(
                FrameDropEffect.createSimpleFrameDropEffect(
                    basicVideoData.frameRate,
                    if (videoEditingState.frameRate >= basicVideoData.frameRate) basicVideoData.frameRate
                    else videoEditingState.frameRate
                )
            )
        }

        exoPlayer.applyEffects(
            uri = uri,
            effectList = videoEditingState.effectList
        )
    }

    val totalModCount = remember { mutableIntStateOf(0) }
    LaunchedEffect(totalModCount.intValue) {
        val last = modifications.lastOrNull()

        if (last is VideoModification.Adjustment) {
            videoEditingState.addEffect(
                effect = last.toEffect(),
                effectIndex = MediaAdjustments.entries.indexOf(last.type)
            )
        }

        exoPlayer.applyEffects(
            uri = uri,
            effectList = videoEditingState.effectList
        )
    }

    val coroutineScope = rememberCoroutineScope()
    val filterPagerState = rememberPagerState(
        initialPage = MediaColorFilters.entries.indexOf(
            (modifications.lastOrNull {
                it is VideoModification.Filter
            } as? VideoModification.Filter)?.type ?: MediaColorFilters.None
        )
    ) { MediaColorFilters.entries.size }


    var canvasSize by remember { mutableStateOf(Size.Zero) }
    Scaffold(
        topBar = {
            VideoEditorTopBar(
                uri = uri,
                absolutePath = absolutePath,
                modifications = modifications,
                videoEditingState = videoEditingState,
                drawingPaintState = drawingPaintState,
                basicVideoData = basicVideoData,
                lastSavedModCount = lastSavedModCount,
                containerDimens = containerDimens,
                canvasSize = canvasSize
            )
        },
        bottomBar = {
            VideoEditorBottomBar(
                pagerState = pagerState,
                currentPosition = currentVideoPosition,
                basicData = basicVideoData,
                videoEditingState = videoEditingState,
                drawingPaintState = drawingPaintState,
                modifications = modifications,
                onSeek = { pos ->
                    val wasPlaying = isPlaying.value
                    exoPlayer.seekTo(
                        (pos * 1000f).coerceAtMost(videoEditingState.endTrimPosition * 1000f).toLong()
                    )
                    isPlaying.value = wasPlaying
                },
                increaseModCount = {
                    totalModCount.intValue += 1
                },
                saveEffect = { filter ->
                    videoEditingState.removeAllEffects {
                        it is ColorMatrixEffect && it.isFilter
                    }

                    videoEditingState.addEffect(filter.toEffect())

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
                targetValue = videoEditingState.rotation
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
                    targetValue = if (videoEditingState.rotation % 180 == 0f) this@BoxWithConstraints.maxWidth else this@BoxWithConstraints.maxHeight
                )
                val height by animateDpAsState(
                    targetValue = if (videoEditingState.rotation % 180f == 0f) this@BoxWithConstraints.maxHeight else this@BoxWithConstraints.maxWidth
                )

                val localDensity = LocalDensity.current
                canvasSize = with(localDensity) {
                    Size(width.toPx(), height.toPx())
                }

                val latestCrop by remember {
                    derivedStateOf {
                        modifications.lastOrNull {
                            it is VideoModification.Crop
                        } as? VideoModification.Crop ?: VideoModification.Crop(0f, 0f, 0f, 0f)
                    }
                }

                // find the top left of the actual video area
                var originalCrop by remember { mutableStateOf(VideoModification.Crop(0f, 0f, 0f, 0f)) }
                LaunchedEffect(modifications.lastOrNull()) {
                    if (originalCrop.width == 0f && originalCrop.height == 0f) {
                        originalCrop = modifications.lastOrNull {
                            it is VideoModification.Crop
                        } as? VideoModification.Crop ?: VideoModification.Crop(0f, 0f, 0f, 0f)
                    }
                }

                val actualTop by remember {
                    derivedStateOf {
                        with(localDensity) {
                            (height.toPx() - originalCrop.height) / 2
                        }
                    }
                }
                val actualLeft by remember {
                    derivedStateOf {
                        with(localDensity) {
                            (width.toPx() - originalCrop.width) / 2
                        }
                    }
                }

                var scale by remember { mutableFloatStateOf(1f) }
                var offset by remember { mutableStateOf(Offset.Zero) }
                LaunchedEffect(videoEditingState.resetCrop) {
                    if (videoEditingState.resetCrop) {
                        scale = 1f
                        offset = Offset.Zero
                    }
                }

                val animatedScale by animateFloatAsState(
                    targetValue = scale,
                    animationSpec = tween(
                        durationMillis = AnimationConstants.DURATION_SHORT
                    )
                )
                val animatedOffset by animateOffsetAsState(
                    targetValue = offset,
                    animationSpec = tween(
                        durationMillis = AnimationConstants.DURATION_SHORT
                    )
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
                            VideoFilterPage(
                                pagerState = filterPagerState,
                                drawingPaintState = drawingPaintState,
                                currentVideoPosition = currentVideoPosition,
                                absolutePath = absolutePath,
                                allowedToRefresh = isPlaying.value || isSeeking
                            )
                        } else {
                            var showTextDialog by remember { mutableStateOf(false) }
                            val textMeasurer = rememberTextMeasurer()
                            var tapPosition by remember { mutableStateOf(Offset.Zero) }

                            if (showTextDialog) {
                                TextEntryDialog(
                                    title = stringResource(id = R.string.editing_text),
                                    placeholder = stringResource(id = R.string.bottom_sheets_enter_text),
                                    onValueChange = { input ->
                                        input.isNotBlank()
                                    },
                                    onConfirm = { input ->
                                        if (input.isNotBlank()) {
                                            val size = textMeasurer.measure(
                                                text = input,
                                                style = DrawableText.Styles.Default.copy(
                                                    color = drawingPaintState.paint.color,
                                                    fontSize = TextUnit(drawingPaintState.paint.strokeWidth, TextUnitType.Sp)
                                                )
                                            ).size

                                            val newText = VideoModification.DrawingText(
                                                text = DrawableText(
                                                    text = input,
                                                    position = Offset(tapPosition.x, tapPosition.y),
                                                    paint = drawingPaintState.paint,
                                                    rotation = 0f,
                                                    size = size
                                                )
                                            )

                                            drawingPaintState.modifications.add(newText)

                                            showTextDialog = false
                                            true
                                        } else {
                                            false
                                        }
                                    },
                                    onDismiss = {
                                        showTextDialog = false
                                    }
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        translationX = animatedOffset.x
                                        translationY = animatedOffset.y
                                        scaleX = animatedScale
                                        scaleY = animatedScale
                                    }
                                    .makeVideoDrawCanvas(
                                        drawingPaintState = drawingPaintState,
                                        textMeasurer = textMeasurer,
                                        currentVideoPosition = currentVideoPosition,
                                        enabled = pagerState.currentPage == VideoEditorTabs.entries.indexOf(VideoEditorTabs.Draw)
                                    ) { position ->
                                        tapPosition = position
                                        showTextDialog = true
                                    }
                            ) {
                                AndroidView(
                                    factory = {
                                        playerView
                                    },
                                    modifier = Modifier
                                        .fillMaxSize(1f)
                                        .background(MaterialTheme.colorScheme.background)
                                )

                                PreviewCanvas(
                                    drawingPaintState = drawingPaintState,
                                    actualLeft = actualLeft,
                                    actualTop = actualTop,
                                    latestCrop = latestCrop,
                                    originalCrop = originalCrop,
                                    pagerState = pagerState,
                                    width = width,
                                    height = height
                                )

                                AnimatedContent(
                                    targetState = exoPlayerLoading || basicVideoData.aspectRatio == -1f,
                                    transitionSpec = {
                                        fadeIn(
                                            animationSpec = tween(
                                                durationMillis = AnimationConstants.DURATION_LONG
                                            )
                                        ).togetherWith(
                                            fadeOut(
                                                animationSpec = tween(
                                                    durationMillis = AnimationConstants.DURATION_LONG
                                                )
                                            )
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxSize(1f)
                                        .align(Alignment.Center)
                                ) { state ->
                                    if (state) {
                                        Box(
                                            modifier = Modifier
                                                .requiredSize(
                                                    width = width,
                                                    height = height
                                                )
                                                .clip(RoundedCornerShape(16.dp))
                                                .align(Alignment.Center)
                                                .shimmerEffect(
                                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                                    highlightColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                                )
                                        )
                                    } else {
                                        // just fill the size so it doesn't scale down
                                        Box(
                                            modifier = Modifier
                                                .requiredSize(
                                                    width = width,
                                                    height = height
                                                )
                                        )
                                    }
                                }
                            }
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
                        visible = pagerState.currentPage == VideoEditorTabs.entries.indexOf(VideoEditorTabs.Crop) && basicVideoData.aspectRatio != -1f,
                        enter = fadeIn(
                            animationSpec = AnimationConstants.expressiveTween(
                                durationMillis = AnimationConstants.DURATION_LONG
                            )
                        ),
                        exit = fadeOut(
                            animationSpec = AnimationConstants.expressiveTween(
                                durationMillis = AnimationConstants.DURATION_LONG
                            )
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
                            editingState = videoEditingState,
                            scale = animatedScale,
                            modifier = Modifier
                                .graphicsLayer {
                                    translationX = animatedScale * 16.dp.toPx() + animatedOffset.x
                                    translationY = animatedScale * 16.dp.toPx() + animatedOffset.y
                                    scaleX = animatedScale
                                    scaleY = animatedScale
                                },
                            onAreaChanged = { area, original ->
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
                            },
                            onCropDone = {
                                val actualWidth = with(localDensity) { (containerDimens.width - 56.dp.toPx()) } // subtract spacing of handles
                                val actualHeight = with(localDensity) { (containerDimens.height - 56.dp.toPx()) } // to not clip them
                                val targetX = actualWidth / latestCrop.width
                                val targetY = actualHeight / latestCrop.height

                                scale = max(1f, min(targetX, targetY))

                                offset = Offset(
                                    x = with(localDensity) {
                                        scale * (-latestCrop.left + (containerDimens.width - latestCrop.width) / 2)
                                    },
                                    y = with(localDensity) {
                                        scale * (-latestCrop.top + (containerDimens.height - latestCrop.height) / 2)
                                    }
                                )
                            }
                        )
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
                videoEditingState = videoEditingState,
                drawingPaintState = drawingPaintState,
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
