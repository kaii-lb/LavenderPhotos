package com.kaii.photos.compose.editing_view.video_editor

import android.media.MediaMetadataRetriever
import android.util.Log
import android.view.Window
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
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
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.FrameDropEffect
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavController
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.video_editor.VideoEditorBottomBar
import com.kaii.photos.compose.app_bars.video_editor.VideoEditorTopBar
import com.kaii.photos.compose.dialogs.user_action.TextEntryDialog
import com.kaii.photos.compose.editing_view.CropBox
import com.kaii.photos.compose.editing_view.PreviewCanvas
import com.kaii.photos.compose.editing_view.VideoFilterPage
import com.kaii.photos.compose.editing_view.makeDrawCanvas
import com.kaii.photos.compose.videoplayer.rememberPlayerView
import com.kaii.photos.compose.widgets.shimmerEffect
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.file_management.editing.GenericFileEditor
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.editing.BasicVideoData
import com.kaii.photos.helpers.editing.ColorMatrixEffect
import com.kaii.photos.helpers.editing.DrawableText
import com.kaii.photos.helpers.editing.MediaAdjustments
import com.kaii.photos.helpers.editing.MediaColorFilters
import com.kaii.photos.helpers.editing.VideoEditorTabs
import com.kaii.photos.helpers.editing.VideoModification
import com.kaii.photos.helpers.editing.rememberDrawingPaintState
import com.kaii.photos.helpers.editing.rememberVideoEditingState
import com.kaii.photos.models.editor.EditorViewModel
import com.kaii.photos.models.editor.EditorViewModelFactory
import com.kaii.photos.screens.video.retainVideoPlayerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

private const val TAG = "com.kaii.photos.compose.editing_view.VideoEditor"

@Composable
fun VideoEditor(
    uri: String,
    album: AlbumType?,
    window: Window,
    isFromOpenWithView: Boolean
) {
    val viewModel = viewModel<EditorViewModel>(
        factory = EditorViewModelFactory(
            context = LocalContext.current,
            album = album ?: AlbumType.PlaceHolder
        )
    )

    val blurViews by viewModel.blurViews.collectAsStateWithLifecycle()
    val useBlackBackground by viewModel.useBlackBackground.collectAsStateWithLifecycle()
    val overwriteByDefault by viewModel.overwriteByDefault.collectAsStateWithLifecycle()
    val info by viewModel.immichInfo.collectAsStateWithLifecycle()

    VideoEditorImpl(
        uri = uri,
        info = { info },
        window = window,
        isFromOpenWithView = isFromOpenWithView,
        blurViews = blurViews,
        useBlackBackground = useBlackBackground,
        overwriteByDefault = { overwriteByDefault },
        editVideo = viewModel::editVideo,
        setNavProps = viewModel::setNavProps
    )
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditorImpl(
    uri: String,
    info: () -> ImmichBasicInfo,
    window: Window,
    isFromOpenWithView: Boolean,
    blurViews: Boolean,
    useBlackBackground: Boolean,
    overwriteByDefault: () -> Boolean,
    editVideo: (NavController, GenericFileEditor.EditParameters.Video) -> Unit,
    setNavProps: (NavController) -> Unit
) {
    var exoPlayerLoading by remember { mutableStateOf(true) }
    val videoPlayerState = retainVideoPlayerState(
        isOpenWithView = isFromOpenWithView,
        onControlsTimeout = {},
        onPlaybackStateChanged = { playbackState ->
            if (exoPlayerLoading) {
                exoPlayerLoading = playbackState == ExoPlayer.STATE_BUFFERING
            }
        }
    )

    val context = LocalContext.current
    LaunchedEffect(uri, info()) {
        if (uri.startsWith("/api") && (info().auth.asString().isBlank() || info().endpoint.isBlank())) return@LaunchedEffect

        videoPlayerState.setSource(
            context = context,
            item = MediaStoreData.dummyItem.copy(
                uri = uri,
                immichUrl = uri.takeIf { it.startsWith("/api") }
            ),
            auth = info().auth,
            endpoint = info().endpoint,
            shouldPlay = { true }
        )
    }

    val videoEditingState = rememberVideoEditingState(
        duration = videoPlayerState.duration
    )
    val drawingPaintState = rememberDrawingPaintState(
        isVideo = true
    )

    LaunchedEffect(videoEditingState.startTrimPosition, videoPlayerState.currentPosition) {
        if (videoPlayerState.currentPosition * 1000 < videoEditingState.startTrimPosition * 1000) {
            videoPlayerState.seekTo((videoEditingState.startTrimPosition * 1000).toLong())

            if (videoPlayerState.isPlaying) {
                videoPlayerState.play()
            } else {
                videoPlayerState.pause()
            }
        }
    }

    LaunchedEffect(videoPlayerState.isPlaying) {
        if (!videoPlayerState.isPlaying) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            videoPlayerState.pause()
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            videoPlayerState.play()
        }

        val end = videoEditingState.endTrimPosition * 1000
        val threshold = 150f
        val current = videoPlayerState.currentPosition * 1000
        if (current in (end - threshold)..(end + threshold) || current >= end) {
            delay(1000)
            videoPlayerState.pause()
            videoPlayerState.seekTo((videoEditingState.startTrimPosition * 1000).toLong())
            Log.d(TAG, "Ending video...")
        }

        while (videoPlayerState.isPlaying) {
            // again here since exoplayer doesn't know we might be ending video early
            // because of the way it works (this is easier)
            val end = videoEditingState.endTrimPosition * 1000
            val threshold = 150f
            val current = videoPlayerState.currentPosition * 1000
            if (current in (end - threshold)..(end + threshold) || current >= end) {
                delay(1000)
                videoPlayerState.pause()
                videoPlayerState.seekTo((videoEditingState.startTrimPosition * 1000).toLong())
                Log.d(TAG, "Ending video...")
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
        videoPlayerState.setPlaybackSpeed(videoEditingState.speed)
    }

    val pagerState = rememberPagerState { VideoEditorTabs.entries.size }
    var containerDimens by remember { mutableStateOf(Size.Zero) }
    var isSeeking by remember { mutableStateOf(false) }

    var basicVideoData by remember {
        mutableStateOf(
            BasicVideoData(
                duration = videoPlayerState.duration / 1000f,
                width = videoPlayerState.videoSize.width,
                height = videoPlayerState.videoSize.height,
                uri = if (uri.startsWith("/api")) {
                    info().endpoint + uri.replace("original", "video/playback")
                } else {
                    uri
                },
                bitrate = videoPlayerState.videoFormat?.bitrate ?: 0,
                frameRate =
                    if (videoPlayerState.videoFormat?.frameRate?.toInt() == -1 || videoPlayerState.videoFormat?.frameRate == null) 0f
                    else videoPlayerState.videoFormat!!.frameRate,
                audioChannelCount = videoPlayerState.audioFormat?.channelCount ?: 2
            )
        )
    }

    Log.d(TAG, "basic video data $basicVideoData")

    LaunchedEffect(videoPlayerState.duration, videoPlayerState.audioTracks.lastOrNull(), info()) {
        if (uri.startsWith("/api") && info().auth.asString().isBlank()) return@LaunchedEffect

        val videoFormat = videoPlayerState.videoFormat
        var tries = 0
        val audioChannelCount = videoPlayerState.audioFormat?.channelCount ?: 2
        val frameRate = videoPlayerState.getFrameRate()

        withContext(Dispatchers.IO) {
            val mediaUri = if (uri.startsWith("/api")) {
                info().endpoint + uri.replace("original", "video/playback")
            } else {
                uri
            }

            val metadata = MediaMetadataRetriever()
            if (uri.startsWith("/api")) {
                metadata.setDataSource(
                    mediaUri,
                    info().auth.headers
                )
            } else {
                metadata.setDataSource(
                    context,
                    mediaUri.toUri()
                )
            }

            // this mess is because exoplayer doesn't really know what res the video is all the time
            val frame = metadata.frameAtTime
            val size = if (frame == null) {
                if (videoPlayerState.videoSize == VideoSize.UNKNOWN) {
                    VideoSize(
                        videoPlayerState.videoFormat?.width ?: 1,
                        videoPlayerState.videoFormat?.height ?: 1
                    )
                } else {
                    videoPlayerState.videoSize
                }
            } else {
                VideoSize(frame.width, frame.height)
            }

            if (frameRate != null && frameRate.toInt() != -1 && !frameRate.isNaN()) videoEditingState.setFrameRate(frameRate) // important!!!

            val bitrate = if (videoFormat?.bitrate == null || videoFormat.bitrate == -1) {
                val possible = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toInt()

                possible ?: -1 // -1 basically give up and go for highest
            } else {
                videoFormat.bitrate
            }

            videoEditingState.setBitrate(bitrate * 2) // * 2 since with markup and effects and stuff the normal simply isn't enough

            basicVideoData =
                BasicVideoData(
                    duration = videoPlayerState.duration,
                    frameRate = frameRate,
                    uri = mediaUri,
                    bitrate = bitrate,
                    width = size.width,
                    height = size.height,
                    audioChannelCount = audioChannelCount
                )
            tries += 1

            Log.d(TAG, "Video data $basicVideoData")
            delay(100)
        }
    }

    LaunchedEffect(videoEditingState.volume) {
        videoPlayerState.setVolume(videoEditingState.volume)
    }

    LaunchedEffect(videoEditingState.frameRate) {
        Log.d(TAG, "Video frame-rates ${videoEditingState.frameRate} ${basicVideoData.frameRate}")

        if (videoEditingState.frameRate == basicVideoData.frameRate || videoEditingState.frameRate <= 0f || basicVideoData.frameRate == null) {
            videoEditingState.removeAllEffects {
                it is FrameDropEffect
            }
        } else {
            videoEditingState.addEffect(
                FrameDropEffect.createSimpleFrameDropEffect(
                    basicVideoData.frameRate!!,
                    if (videoEditingState.frameRate >= basicVideoData.frameRate!!) basicVideoData.frameRate!!
                    else videoEditingState.frameRate
                )
            )
        }

        videoPlayerState.applyEffects(
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

        videoPlayerState.applyEffects(
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
                modifications = modifications,
                videoEditingState = videoEditingState,
                drawingPaintState = drawingPaintState,
                basicVideoData = basicVideoData,
                lastSavedModCount = lastSavedModCount,
                containerDimens = containerDimens,
                canvasSize = canvasSize,
                isFromOpenWithView = isFromOpenWithView,
                overwriteByDefault = overwriteByDefault,
                info = info,
                editVideo = editVideo,
                setNavProps = setNavProps
            )
        },
        bottomBar = {
            VideoEditorBottomBar(
                pagerState = pagerState,
                currentPosition = { videoPlayerState.currentPosition },
                basicData = { basicVideoData },
                auth = { info().auth },
                videoEditingState = videoEditingState,
                drawingPaintState = drawingPaintState,
                modifications = modifications,
                onSeek = { pos ->
                    videoPlayerState.seekTo(
                        (pos * 1000f).coerceAtMost(videoEditingState.endTrimPosition * 1000f).toLong()
                    )
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
        BoxWithConstraints(
            modifier = Modifier
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            val animatedRotation by animateFloatAsState(
                targetValue = videoEditingState.rotation
            )

            val width = this@BoxWithConstraints.maxWidth - 48.dp
            val height = this@BoxWithConstraints.maxHeight - 48.dp

            val localDensity = LocalDensity.current
            val rotationScale by animateFloatAsState(
                targetValue =
                    with(localDensity) {
                        if (videoEditingState.rotation % 180f == 0f) {
                            min(
                                width.toPx() / containerDimens.width,
                                height.toPx() / containerDimens.height
                            )
                        } else {
                            min(
                                width.toPx() / containerDimens.height,
                                height.toPx() / containerDimens.width
                            )
                        } * 0.9f
                    }
            )

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

            val videoSize by remember {
                derivedStateOf {
                    with(localDensity) {
                        val xRatio = width.toPx() / basicVideoData.width
                        val yRatio = height.toPx() / basicVideoData.height
                        val ratio = min(xRatio, yRatio)

                        Size(
                            width = basicVideoData.width * ratio,
                            height = basicVideoData.height * ratio
                        )
                    }
                }
            }

            val actualTop by remember {
                derivedStateOf {
                    with(localDensity) {
                        (height.toPx() - videoSize.height) / 2
                    }
                }
            }

            val actualLeft by remember {
                derivedStateOf {
                    with(localDensity) {
                        (width.toPx() - videoSize.width) / 2
                    }
                }
            }

            val animatedScale by animateFloatAsState(
                targetValue = videoEditingState.scale,
                animationSpec = tween(
                    durationMillis = AnimationConstants.DURATION_SHORT
                )
            )
            val animatedOffset by animateOffsetAsState(
                targetValue = videoEditingState.offset,
                animationSpec = tween(
                    durationMillis = AnimationConstants.DURATION_SHORT
                )
            )

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

            val isInFilterPage by remember {
                derivedStateOf {
                    pagerState.currentPage == VideoEditorTabs.entries.indexOf(VideoEditorTabs.Filters)
                }
            }

            val animatedAlpha by animateFloatAsState(
                targetValue = if (isInFilterPage) 0f else 1f,
                animationSpec = AnimationConstants.expressiveTween(
                    durationMillis = AnimationConstants.DURATION
                )
            )

            Box(
                modifier = Modifier
                    .alpha(animatedAlpha)
                    .rotate(animatedRotation)
                    .scale(rotationScale)
                    .graphicsLayer {
                        translationX = animatedOffset.x
                        translationY = animatedOffset.y
                        scaleX = animatedScale
                        scaleY = animatedScale
                    }
                    .makeDrawCanvas(
                        drawingPaintState = drawingPaintState,
                        textMeasurer = textMeasurer,
                        currentVideoPosition = videoPlayerState.currentPosition,
                        enabled = pagerState.currentPage == VideoEditorTabs.entries.indexOf(VideoEditorTabs.Draw),
                        addText = { position ->
                            tapPosition = position
                            showTextDialog = true
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                val playerView = rememberPlayerView(
                    useTextureView = true,
                    blurViews = blurViews,
                    useBlackBackground = useBlackBackground
                )

                AndroidView(
                    factory = {
                        playerView
                    },
                    update = {
                        videoPlayerState.linkPlayerView(it)
                    },
                    modifier = Modifier
                        .width(width)
                        .height(height)
                        .background(MaterialTheme.colorScheme.background)
                )

                PreviewCanvas(
                    drawingPaintState = drawingPaintState,
                    actualLeft = actualLeft,
                    actualTop = actualTop,
                    latestCrop = latestCrop,
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

            AnimatedVisibility(
                visible = isInFilterPage,
                enter = slideInHorizontally {
                    if (isInFilterPage) it
                    else -it
                } + fadeIn(),
                exit = (slideOutHorizontally {
                    if (isInFilterPage) -it
                    else it
                } + fadeOut()),
                modifier = Modifier
                    .fillMaxSize(1f)
                    .padding(12.dp)
            ) {
                VideoFilterPage(
                    pagerState = filterPagerState,
                    drawingPaintState = drawingPaintState,
                    currentVideoPosition = videoPlayerState.currentPosition,
                    uri = basicVideoData.uri,
                    endpoint = { info().endpoint },
                    auth = { info().auth },
                    allowedToRefresh = videoPlayerState.isPlaying || isSeeking
                )
            }

            CropBox(
                containerWidth = with(localDensity) { width.toPx() },
                containerHeight = with(localDensity) { height.toPx() },
                mediaAspectRatio = basicVideoData.aspectRatio,
                editingState = videoEditingState,
                scale = animatedScale * rotationScale,
                enabled = pagerState.currentPage == VideoEditorTabs.entries.indexOf(VideoEditorTabs.Crop) && basicVideoData.aspectRatio != -1f,
                modifier = Modifier
                    .rotate(animatedRotation)
                    .scale(rotationScale)
                    .graphicsLayer {
                        translationX = animatedOffset.x
                        translationY = animatedOffset.y
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

                    videoEditingState.setScale(max(1f, min(targetX, targetY)))

                    videoEditingState.setOffset(
                        Offset(
                            x = videoEditingState.scale * (-latestCrop.left + (containerDimens.width - latestCrop.width) / 2),
                            y = videoEditingState.scale * (-latestCrop.top + (containerDimens.height - latestCrop.height) / 2)
                        )
                    )
                }
            )

            VideoEditorBottomTools(
                pagerState = pagerState,
                modifications = modifications,
                currentPosition = videoPlayerState.currentPosition,
                duration = videoPlayerState.duration,
                isPlaying = videoPlayerState.isPlaying,
                isMuted = videoPlayerState.isMuted,
                totalModCount = totalModCount,
                videoEditingState = videoEditingState,
                drawingPaintState = drawingPaintState,
                modifier = Modifier
                    .align(Alignment.BottomCenter),
                onSeek = { pos ->
                    isSeeking = true

                    videoPlayerState.seekTo(
                        (pos * 1000f).coerceAtMost(videoPlayerState.duration * 1000f).toLong()
                    )
                },
                onSeekFinished = {
                    isSeeking = false
                },
                togglePlayback = {
                    if (videoPlayerState.isPlaying) videoPlayerState.pause()
                    else videoPlayerState.play()
                },
                toggleMute = videoPlayerState::toggleMute
            )
        }
    }
}
