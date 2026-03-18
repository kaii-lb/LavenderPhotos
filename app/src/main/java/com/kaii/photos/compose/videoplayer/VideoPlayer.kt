package com.kaii.photos.compose.videoplayer

import android.app.Activity
import android.util.Log
import android.view.Window
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.setBarVisibility
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.EncryptionManager
import com.kaii.photos.helpers.VideoPlayerConstants
import com.kaii.photos.helpers.appSecureFolderDir
import com.kaii.photos.helpers.scrolling.SinglePhotoScrollState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.ceil

// special thanks to @bedirhansaricayir on GitHub, helped with a LOT of performance stuff
// https://github.com/bedirhansaricayir/Instagram-Reels-Jetpack-Compose/blob/master/app/src/main/java/com/reels/example/presentation/components/ExploreVideoPlayer.kt

private const val TAG = "com.kaii.photos.compose.videoplayer.VideoPlayer"

// TODO: rework to use a class or viewmodel instead of this mess
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    item: MediaStoreData,
    accessToken: String,
    appBarsVisible: MutableState<Boolean>,
    shouldAutoPlay: Boolean,
    scrollState: SinglePhotoScrollState,
    window: Window,
    shouldPlay: State<Boolean>,
    blurViews: Boolean,
    useBlackBackground: Boolean,
    modifier: Modifier = Modifier,
    isOpenWithView: Boolean = false
) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val isSecuredMedia = item.absolutePath.startsWith(context.appSecureFolderDir)
    var videoSource by remember { mutableStateOf(
        item.immichUrl?.replace("original", "video/playback")?.toUri() ?: item.uri.toUri()
    )}

    if (isSecuredMedia) {
        var securedMediaProgress by remember { mutableFloatStateOf(0f) }
        var continueToVideo by remember { mutableStateOf(!isSecuredMedia) }

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                val iv = MediaDatabase.getInstance(context).securedItemEntityDao()
                    .getIvFromSecuredPath(item.absolutePath)

                if (iv == null) {
                    Log.e(TAG, "IV for ${item.displayName} was null, aborting")
                    return@withContext
                }

                val output =
                    EncryptionManager.decryptVideo(
                        absolutePath = item.absolutePath,
                        iv = iv,
                        context = context
                    ) {
                        securedMediaProgress = it
                    }

                videoSource = output.toUri()
                continueToVideo = true
            }
        }

        if (!continueToVideo) {
            Column(
                modifier = modifier
                    .fillMaxSize(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.video_decrypting),
                    fontSize = TextUnit(16f, TextUnitType.Sp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(id = R.string.media_progress),
                    fontSize = TextUnit(16f, TextUnitType.Sp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = {
                        securedMediaProgress
                    },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainer,
                    gapSize = 0.dp,
                    strokeCap = StrokeCap.Round,
                    drawStopIndicator = {},
                    modifier = Modifier
                        .height(14.dp)
                        .fillMaxWidth(0.6f),
                )
            }

            return
        }
    }

    val isPlaying = retain(shouldAutoPlay, shouldPlay.value) {
        mutableStateOf(shouldPlay.value && shouldAutoPlay)
    }
    val lastIsPlaying = retain { mutableStateOf(isPlaying.value) }

    /** In Seconds */
    val currentVideoPosition = retain { mutableFloatStateOf(0f) }
    val duration = retain { mutableFloatStateOf(0f) }

    val exoPlayer = rememberExoPlayerWithLifeCycle(
        videoSource = videoSource,
        item = item,
        accessToken = accessToken,
        isPlaying = isPlaying,
        duration = duration,
        currentVideoPosition = currentVideoPosition,
        onPlaybackStateChanged = {}
    )
    val playerView = rememberPlayerView(
        exoPlayer = exoPlayer,
        activity = context as Activity,
        absolutePath = item.absolutePath,
        blurViews = blurViews,
        useBlackBackground = useBlackBackground
    )

    val isMuted = remember(scrollState.videoWasMuted) { mutableStateOf(scrollState.videoWasMuted) }
    val controlsVisible = remember { mutableStateOf(true) }
    var showVideoPlayerControlsTimeout by remember { mutableIntStateOf(0) }

    LaunchedEffect(showVideoPlayerControlsTimeout) {
        delay(VideoPlayerConstants.CONTROLS_HIDE_TIMEOUT)
        setBarVisibility(
            visible = false,
            window = window
        ) {
            appBarsVisible.value = it

            controlsVisible.value = it
        }

        showVideoPlayerControlsTimeout = 0
    }

    BackHandler {
        isPlaying.value = false
        currentVideoPosition.floatValue = 0f
        duration.floatValue = 0f
        scrollState.resetMute()

        exoPlayer.stop()
        exoPlayer.release()

        if (isOpenWithView) context.finish()
        else navController.popBackStack()
    }


    val isLandscape by rememberDeviceOrientation()
    LaunchedEffect(isPlaying.value, isLandscape, shouldPlay.value) {
        if (!isPlaying.value || !shouldPlay.value) {
            controlsVisible.value = true

            if (!isLandscape) {
                setBarVisibility(
                    visible = true,
                    window = window
                ) {
                    appBarsVisible.value = true
                }
            }

            exoPlayer.pause()

            if (currentVideoPosition.floatValue > 0f) exoPlayer.isScrubbingModeEnabled = true
        } else {
            exoPlayer.isScrubbingModeEnabled = false
            exoPlayer.play()
        }

        lastIsPlaying.value = isPlaying.value

        currentVideoPosition.floatValue = exoPlayer.currentPosition / 1000f

        if (shouldPlay.value && isPlaying.value && shouldAutoPlay && !exoPlayer.isPlaying) {
            exoPlayer.play()
        }

        while (isPlaying.value && shouldPlay.value) {
            currentVideoPosition.floatValue = exoPlayer.currentPosition / 1000f

            delay(250)

            if (ceil(currentVideoPosition.floatValue) >= ceil(duration.floatValue) && duration.floatValue != 0f && isPlaying.value) {
                launch {
                    controlsVisible.value = true
                    delay(1000)
                    exoPlayer.pause()
                    exoPlayer.seekTo(0)
                    currentVideoPosition.floatValue = 0f
                    isPlaying.value = false
                }
            }
        }
    }


    LaunchedEffect(controlsVisible.value) {
        if (controlsVisible.value) showVideoPlayerControlsTimeout += 1
    }

    LaunchedEffect(isMuted.value) {
        exoPlayer.volume = if (isMuted.value) 0f else 1f

        exoPlayer.setAudioAttributes(
            AudioAttributes.Builder().apply {
                setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                AudioAttributes.DEFAULT
                setAllowedCapturePolicy(C.ALLOW_CAPTURE_BY_ALL)
            }.build(),
            !isMuted.value
        )
    }

    LaunchedEffect(shouldAutoPlay, shouldPlay.value) {
        exoPlayer.playWhenReady = shouldAutoPlay && shouldPlay.value
    }

    Box(
        modifier = Modifier
            .fillMaxSize(1f)
            .then(
                if (isPlaying.value && shouldPlay.value) {
                    Modifier.keepScreenOn()
                } else Modifier.Companion
            )
    ) {
        AndroidView(
            factory = {
                playerView
            },
            modifier = modifier
                .align(Alignment.Center)
        )

        var doubleTapDisplayTimeMillis by remember { mutableIntStateOf(0) }
        val isLandscape by rememberDeviceOrientation()
        val seekBackBackgroundColor by animateColorAsState(
            targetValue = if (doubleTapDisplayTimeMillis < 0) MaterialTheme.colorScheme.primary.copy(
                alpha = 0.4f
            ) else Color.Transparent,
            animationSpec = tween(
                durationMillis = 350
            ),
            label = "Animate double tap to skip background color"
        )
        val seekForwardBackgroundColor by animateColorAsState(
            targetValue = if (doubleTapDisplayTimeMillis > 0) MaterialTheme.colorScheme.primary.copy(
                alpha = 0.4f
            ) else Color.Transparent,
            animationSpec = tween(
                durationMillis = 350
            ),
            label = "Animate double tap to skip background color"
        )

        LaunchedEffect(doubleTapDisplayTimeMillis) {
            delay(1000)
            doubleTapDisplayTimeMillis = 0
        }
        LaunchedEffect(isLandscape) {
            setBarVisibility(
                visible = !isLandscape,
                window = window
            ) {
                appBarsVisible.value = it
                if (!isLandscape) controlsVisible.value = it
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize(1f)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { position ->
                            if (!scrollState.videoLock && doubleTapDisplayTimeMillis == 0) {
                                setBarVisibility(
                                    visible = if (isLandscape) false else !controlsVisible.value,
                                    window = window
                                ) {
                                    appBarsVisible.value = it
                                }

                                controlsVisible.value = !controlsVisible.value
                            } else if (!scrollState.videoLock && doubleTapDisplayTimeMillis != 0) {
                                if (position.x < size.width / 2) {
                                    if (doubleTapDisplayTimeMillis > 0) doubleTapDisplayTimeMillis =
                                        0
                                    doubleTapDisplayTimeMillis -= 1000

                                    val prev = isPlaying.value
                                    exoPlayer.seekBack()
                                    isPlaying.value = prev
                                } else if (position.x >= size.width / 2) {
                                    if (doubleTapDisplayTimeMillis < 0) doubleTapDisplayTimeMillis =
                                        0
                                    doubleTapDisplayTimeMillis += 1000

                                    val prev = isPlaying.value
                                    exoPlayer.seekForward()
                                    isPlaying.value = prev
                                }

                                showVideoPlayerControlsTimeout += 1
                            }
                        },

                        onDoubleTap = { position ->
                            if (!scrollState.videoLock && position.x < size.width / 2) {
                                if (doubleTapDisplayTimeMillis > 0) doubleTapDisplayTimeMillis = 0
                                doubleTapDisplayTimeMillis -= 1000

                                val prev = isPlaying.value
                                exoPlayer.seekBack()
                                isPlaying.value = prev
                            } else if (!scrollState.videoLock && position.x >= size.width / 2) {
                                if (doubleTapDisplayTimeMillis < 0) doubleTapDisplayTimeMillis = 0
                                doubleTapDisplayTimeMillis += 1000

                                val prev = isPlaying.value
                                exoPlayer.seekForward()
                                isPlaying.value = prev
                            }

                            showVideoPlayerControlsTimeout += 1
                        }
                    )
                }
                .then(modifier)
        )

        // seperate boxes to avoid touch blocking due to zindex ordering
        Box(
            modifier = Modifier
                .fillMaxSize(1f)
                .zIndex(2f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight(1f)
                    .fillMaxWidth(0.45f)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(0, 100, 100, 0))
                    .background(seekBackBackgroundColor)
            ) {
                AnimatedVisibility(
                    visible = doubleTapDisplayTimeMillis < 0,
                    enter =
                        fadeIn(
                            animationSpec = tween(
                                durationMillis = AnimationConstants.DURATION
                            )
                        ) + scaleIn(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            )
                        ),
                    exit =
                        fadeOut(
                            animationSpec = tween(
                                durationMillis = AnimationConstants.DURATION
                            )
                        ) + scaleOut(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            )
                        ),
                    modifier = Modifier
                        .align(Alignment.Center)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.fast_rewind),
                        contentDescription = stringResource(id = R.string.video_seek_icon_desc),
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight(1f)
                    .fillMaxWidth(0.45f)
                    .align(Alignment.CenterEnd)
                    .clip(RoundedCornerShape(100, 0, 0, 100))
                    .background(seekForwardBackgroundColor)
            ) {
                AnimatedVisibility(
                    visible = doubleTapDisplayTimeMillis > 0,
                    enter =
                        fadeIn(
                            animationSpec = tween(
                                durationMillis = AnimationConstants.DURATION
                            )
                        ) + scaleIn(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            )
                        ),
                    exit =
                        fadeOut(
                            animationSpec = tween(
                                durationMillis = AnimationConstants.DURATION
                            )
                        ) + scaleOut(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            )
                        ),
                    modifier = Modifier
                        .align(Alignment.Center)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.fast_forward),
                        contentDescription = stringResource(id = R.string.video_seek_icon_desc),
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center)
                    )
                }
            }
        }

        val title = remember { File(item.absolutePath).nameWithoutExtension }
        AnimatedVisibility(
            visible = controlsVisible.value,
            enter = fadeIn(
                animationSpec = AnimationConstants.expressiveTween()
            ),
            exit = fadeOut(
                animationSpec = AnimationConstants.expressiveTween()
            ),
            modifier = Modifier
                .fillMaxSize(1f)
                .align(Alignment.Center)
        ) {
            VideoPlayerControls(
                exoPlayer = exoPlayer,
                isPlaying = isPlaying,
                isMuted = isMuted,
                currentVideoPosition = currentVideoPosition,
                duration = duration,
                title = title,
                modifier = Modifier
                    .fillMaxSize(1f),
                onAnyTap = {
                    showVideoPlayerControlsTimeout += 1
                },
                setLastWasMuted = { new ->
                    scrollState.setWasMuted(new)
                }
            )
        }

        if ((scrollState.videoLock || controlsVisible.value) && isLandscape) {
            Row(
                modifier = Modifier
                    .wrapContentSize()
                    .animateContentSize()
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    space = 4.dp,
                    alignment = Alignment.CenterHorizontally
                )
            ) {
                FilledTonalIconToggleButton(
                    checked = scrollState.videoLock,
                    onCheckedChange = {
                        scrollState.setVideoLock(it)
                        showVideoPlayerControlsTimeout += 1
                    },
                    colors = IconButtonDefaults.filledTonalIconToggleButtonColors().copy(
                        checkedContainerColor = MaterialTheme.colorScheme.primary,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.Top)
                ) {
                    Icon(
                        painter = painterResource(id = if (scrollState.videoLock) R.drawable.secure_folder else R.drawable.unlock),
                        contentDescription = stringResource(id = R.string.video_lock_screen),
                        modifier = Modifier
                            .size(20.dp)
                    )
                }

                if (controlsVisible.value) {
                    Column(
                        modifier = Modifier
                            .wrapContentSize(),
                        verticalArrangement = Arrangement.spacedBy(
                            space = 4.dp,
                            alignment = Alignment.Top
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                setBarVisibility(
                                    visible = !appBarsVisible.value,
                                    window = window
                                ) {
                                    appBarsVisible.value = it
                                }

                                showVideoPlayerControlsTimeout += 1
                                scrollState.setVideoLock(false)
                            },
                            colors = IconButtonDefaults.filledTonalIconButtonColors().copy(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier
                                .size(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.more_options),
                                contentDescription = stringResource(id = R.string.show_options),
                                modifier = Modifier
                                    .size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}