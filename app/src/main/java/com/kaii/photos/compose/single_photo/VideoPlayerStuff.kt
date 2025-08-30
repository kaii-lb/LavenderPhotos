package com.kaii.photos.compose.single_photo

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.util.Log
import android.util.Xml
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.C.VIDEO_SCALING_MODE_SCALE_TO_FIT
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.kaii.photos.LocalAppDatabase
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.setBarVisibility
import com.kaii.photos.compose.rememberDeviceOrientation
import com.kaii.photos.datastore.Video
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.EncryptionManager
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.helpers.VideoPlayerConstants
import com.kaii.photos.helpers.appSecureFolderDir
import com.kaii.photos.helpers.getSecureDecryptedVideoFile
import com.kaii.photos.mediastore.MediaStoreData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// special thanks to @bedirhansaricayir on github, helped with a LOT of performance stuff
// https://github.com/bedirhansaricayir/Instagram-Reels-Jetpack-Compose/blob/master/app/src/main/java/com/reels/example/presentation/components/ExploreVideoPlayer.kt

private const val TAG = "VIDEO_PLAYER_STUFF"

@Composable
fun VideoPlayerControls(
    exoPlayer: ExoPlayer,
    isPlaying: MutableState<Boolean>,
    isMuted: MutableState<Boolean>,
    currentVideoPosition: MutableFloatState,
    duration: MutableFloatState,
    title: String,
    modifier: Modifier,
    onAnyTap: () -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .background(Color.Transparent)
    ) {
        val isLandscape by rememberDeviceOrientation()

        if (isLandscape) {
            // title box
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = if (title == "") stringResource(id = R.string.media) else title,
                    fontSize = TextUnit(12f, TextUnitType.Sp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .widthIn(max = this@BoxWithConstraints.maxWidth / 2)
                        .wrapContentSize()
                        .clip(RoundedCornerShape(1000.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(8.dp, 4.dp)
                        .align(Alignment.CenterStart)
                )
            }
        }

        Row(
            modifier = Modifier
                .height(if (isLandscape) 48.dp else 172.dp)
                .align(Alignment.BottomCenter),
            verticalAlignment = Alignment.Top
        ) {
            VideoPlayerControllerBottomControls(
                currentVideoPosition = currentVideoPosition,
                duration = duration,
                isPlaying = isPlaying,
                isMuted = isMuted,
                exoPlayer = exoPlayer,
                onAnyTap = onAnyTap
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth(1f)
                .wrapContentHeight()
                .align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            FilledTonalIconButton(
                onClick = {
                    val prev = isPlaying.value
                    exoPlayer.seekBack()
                    isPlaying.value = prev

                    onAnyTap()
                },
                modifier = Modifier
                    .size(48.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.fast_rewind),
                    contentDescription = stringResource(id = R.string.video_seek_back),
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 2.dp, 0.dp)
                )
            }

            Spacer(modifier = Modifier.width(48.dp))

            FilledTonalIconButton(
                onClick = {
                    isPlaying.value = !isPlaying.value

                    onAnyTap()
                },
                modifier = Modifier
                    .size(48.dp)
            ) {
                Icon(
                    painter = painterResource(id = if (!isPlaying.value) R.drawable.play_arrow else R.drawable.pause),
                    contentDescription = stringResource(id = R.string.video_play_toggle)
                )
            }

            Spacer(modifier = Modifier.width(48.dp))

            FilledTonalIconButton(
                onClick = {
                    val prev = isPlaying.value
                    exoPlayer.seekForward()
                    isPlaying.value = prev

                    onAnyTap()
                },
                modifier = Modifier
                    .size(48.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.fast_forward),
                    contentDescription = stringResource(id = R.string.video_seek_front),
                    modifier = Modifier
                        .padding(2.dp, 0.dp, 0.dp, 0.dp)
                )
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun VideoPlayerControllerBottomControls(
    currentVideoPosition: MutableFloatState,
    duration: MutableFloatState,
    isPlaying: MutableState<Boolean>,
    exoPlayer: ExoPlayer,
    isMuted: MutableState<Boolean>,
    onAnyTap: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(1f)
            .wrapContentHeight()
            .padding(16.dp, 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val currentDurationFormatted =
            currentVideoPosition.floatValue.roundToInt().seconds.formatLikeANormalPerson()

        // video progress
        Row(
            modifier = Modifier
                .height(32.dp)
                .width(if (currentDurationFormatted.second) 72.dp else 48.dp)
                .clip(RoundedCornerShape(1000.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(4.dp, 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = currentDurationFormatted.first,
                style = TextStyle(
                    fontSize = TextUnit(12f, TextUnitType.Sp),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                ),
            )
        }

        duration.floatValue = duration.floatValue.coerceAtLeast(0f)

        VideoPlayerSeekbar(
            currentPosition = currentVideoPosition.floatValue,
            duration = duration.floatValue,
            modifier = Modifier
                .weight(1f)
        ) { pos ->
            onAnyTap()

            val prev = isPlaying.value
            exoPlayer.seekTo(
                (pos * 1000f).coerceAtMost(duration.floatValue * 1000f).toLong()
            )
            isPlaying.value = prev
        }

        val formattedDuration =
            duration.floatValue.roundToInt().seconds.formatLikeANormalPerson()

        // total duration
        Row(
            modifier = Modifier
                .height(32.dp)
                .width(if (formattedDuration.second) 72.dp else 48.dp)
                .clip(RoundedCornerShape(1000.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(4.dp, 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = formattedDuration.first,
                style = TextStyle(
                    fontSize = TextUnit(12f, TextUnitType.Sp),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                ),
            )
        }

        // mute button
        FilledTonalIconButton(
            onClick = {
                isMuted.value = !isMuted.value

                onAnyTap()
            },
            modifier = Modifier
                .size(32.dp)
        ) {
            Icon(
                painter = painterResource(id = if (isMuted.value) R.drawable.volume_mute else R.drawable.volume_max),
                contentDescription = stringResource(id = R.string.video_mute_toggle),
                modifier = Modifier
                    .size(24.dp)
            )
        }

        val isLandscape by rememberDeviceOrientation()
        if (isLandscape) {
            var currentPlaybackSpeed by remember { mutableFloatStateOf(exoPlayer.playbackParameters.speed) }

            FilledTonalButton(
                onClick = {
                    val new =
                        when (currentPlaybackSpeed) {
                            1f -> 1.5f
                            1.5f -> 2f
                            2f -> 4f
                            4f -> 0.5f
                            else -> 1f
                        }

                    exoPlayer.setPlaybackSpeed(new)
                    currentPlaybackSpeed = new
                },
                contentPadding = PaddingValues(horizontal = 4.dp),
                modifier = Modifier
                    .height(32.dp)
                    .width(40.dp)
            ) {
                Text(
                    text = "${currentPlaybackSpeed}X",
                    fontSize = TextUnit(TextStylingConstants.EXTRA_SMALL_TEXT_SIZE, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    item: MediaStoreData,
    appBarsVisible: MutableState<Boolean>,
    shouldAutoPlay: Boolean,
    lastWasMuted: MutableState<Boolean>,
    isTouchLocked: MutableState<Boolean>,
    window: Window,
    shouldPlay: State<Boolean>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val isSecuredMedia = item.absolutePath.startsWith(context.appSecureFolderDir)
    var videoSource by remember { mutableStateOf(item.uri) }

    if (isSecuredMedia) {
        var securedMediaProgress by remember { mutableFloatStateOf(0f) }
        var continueToVideo by remember { mutableStateOf(!isSecuredMedia) }

        val applicationDatabase = LocalAppDatabase.current
        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                val iv = applicationDatabase.securedItemEntityDao()
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

    val isPlaying = rememberSaveable { mutableStateOf(false) }
    val lastIsPlaying = rememberSaveable { mutableStateOf(isPlaying.value) }

    val isMuted = rememberSaveable { mutableStateOf(lastWasMuted.value) }

    /** In Seconds */
    val currentVideoPosition = rememberSaveable { mutableFloatStateOf(0f) }
    val duration = rememberSaveable { mutableFloatStateOf(0f) }

    val exoPlayer = rememberExoPlayerWithLifeCycle(
        videoSource,
        item.absolutePath,
        isPlaying,
        duration,
        currentVideoPosition
    )
    val playerView = rememberPlayerView(exoPlayer, context as Activity, item.absolutePath)

    val mainViewModel = LocalMainViewModel.current
    val muteVideoOnStart by mainViewModel.settings.Video.getMuteOnStart()
        .collectAsStateWithLifecycle(initialValue = true)

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
        lastWasMuted.value = muteVideoOnStart

        exoPlayer.stop()
        exoPlayer.release()

        navController.popBackStack()
    }

    val localConfig = LocalConfiguration.current

    LaunchedEffect(isPlaying.value, localConfig.orientation, shouldPlay.value) {
        if (!isPlaying.value || !shouldPlay.value) {
            controlsVisible.value = true
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            if (localConfig.orientation != Configuration.ORIENTATION_LANDSCAPE) {
                setBarVisibility(
                    visible = true,
                    window = window
                ) {
                    appBarsVisible.value = true
                }
            }

            exoPlayer.isScrubbingModeEnabled = true
            exoPlayer.pause()
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            exoPlayer.isScrubbingModeEnabled = false
            exoPlayer.play()
        }

        lastIsPlaying.value = isPlaying.value

        currentVideoPosition.floatValue = exoPlayer.currentPosition / 1000f
        if (kotlin.math.ceil(currentVideoPosition.floatValue) >= kotlin.math.ceil(duration.floatValue) && duration.floatValue != 0f && !isPlaying.value) {
            delay(1000)
            exoPlayer.pause()
            exoPlayer.seekTo(0)
            currentVideoPosition.floatValue = 0f
            isPlaying.value = false
        }

        while (isPlaying.value && shouldPlay.value) {
            currentVideoPosition.floatValue = exoPlayer.currentPosition / 1000f

            delay(1000)
        }
    }


    LaunchedEffect(controlsVisible.value) {
        if (controlsVisible.value) showVideoPlayerControlsTimeout += 1
    }

    LaunchedEffect(isMuted.value) {
        lastWasMuted.value = isMuted.value

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
                            if (!isTouchLocked.value && doubleTapDisplayTimeMillis == 0) {
                                setBarVisibility(
                                    visible = if (isLandscape) false else !controlsVisible.value,
                                    window = window
                                ) {
                                    appBarsVisible.value = it
                                }

                                controlsVisible.value = !controlsVisible.value
                            } else if (!isTouchLocked.value && doubleTapDisplayTimeMillis != 0) {
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
                            if (!isTouchLocked.value && position.x < size.width / 2) {
                                if (doubleTapDisplayTimeMillis > 0) doubleTapDisplayTimeMillis = 0
                                doubleTapDisplayTimeMillis -= 1000

                                val prev = isPlaying.value
                                exoPlayer.seekBack()
                                isPlaying.value = prev
                            } else if (!isTouchLocked.value && position.x >= size.width / 2) {
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
                onAnyTap = {
                    showVideoPlayerControlsTimeout += 1
                },
                modifier = Modifier
                    .fillMaxSize(1f)
            )
        }

        if ((isTouchLocked.value || controlsVisible.value) && localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
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
                    checked = isTouchLocked.value,
                    onCheckedChange = {
                        isTouchLocked.value = it
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
                        painter = painterResource(id = if (isTouchLocked.value) R.drawable.secure_folder else R.drawable.unlock),
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
                                isTouchLocked.value = false
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerSeekbar(
    currentPosition: Float,
    duration: Float,
    modifier: Modifier = Modifier,
    onValueChange: (position: Float) -> Unit
) {
    val localInteractionSource = remember { MutableInteractionSource() }

    var isDraggingSlider by remember { mutableStateOf(false) }
    LaunchedEffect(localInteractionSource.interactions) {
        localInteractionSource.interactions.collect {
            isDraggingSlider = it is DragInteraction.Start || it is PressInteraction.Press
        }
    }

    val animatedPosition by animateFloatAsState(
        targetValue = currentPosition,
        animationSpec = tween(
            durationMillis = if (isDraggingSlider) 0 else AnimationConstants.DURATION,
            easing = LinearEasing
        )
    )

    Slider(
        value = animatedPosition,
        valueRange = 0f..duration,
        onValueChange = onValueChange,
        // steps = (duration.roundToInt() - 1).coerceAtLeast(0),
        thumb = {
            SliderDefaults.Thumb(
                interactionSource = localInteractionSource,
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
        interactionSource = localInteractionSource,
        modifier = modifier
            .height(32.dp)
    )
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun rememberExoPlayerWithLifeCycle(
    videoSource: Uri,
    absolutePath: String?,
    isPlaying: MutableState<Boolean>,
    duration: MutableFloatState,
    currentVideoPosition: MutableFloatState
): ExoPlayer {
    val context = LocalContext.current

    val exoPlayer = remember {
        createExoPlayer(
            videoSource,
            context,
            isPlaying,
            currentVideoPosition,
            duration
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner.lifecycle.currentStateAsState().value) {
        val lifecycleObserver =
            getExoPlayerLifecycleObserver(exoPlayer, isPlaying, context as Activity, absolutePath)

        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }
    return exoPlayer
}

@androidx.annotation.OptIn(UnstableApi::class)
fun createExoPlayer(
    videoSource: Uri,
    context: Context,
    isPlaying: MutableState<Boolean>,
    currentVideoPosition: MutableFloatState,
    duration: MutableFloatState
): ExoPlayer {
    val exoPlayer = ExoPlayer.Builder(context).apply {
        setLoadControl(
            DefaultLoadControl.Builder().apply {
                setBufferDurationsMs(
                    1000,
                    5000,
                    1000,
                    1000
                )

                setBackBuffer(
                    1000,
                    false
                )

                setPrioritizeTimeOverSizeThresholds(false)
            }.build()
        )
        setSeekBackIncrementMs(5000)
        setSeekForwardIncrementMs(5000)

        setPauseAtEndOfMediaItems(true)

        setAudioAttributes(
            AudioAttributes.Builder().apply {
                setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                AudioAttributes.DEFAULT
                setAllowedCapturePolicy(C.ALLOW_CAPTURE_BY_ALL)
            }.build(),
            false
        )

        setHandleAudioBecomingNoisy(true)
    }.build()
        .apply {
            videoScalingMode = VIDEO_SCALING_MODE_SCALE_TO_FIT
            repeatMode = ExoPlayer.REPEAT_MODE_ONE

            val defaultDataSourceFactory = DefaultDataSource.Factory(context)
            val dataSourceFactory = DefaultDataSource.Factory(
                context,
                defaultDataSourceFactory
            )

            val source = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(videoSource))

            setMediaSource(source)
            prepare()
        }

    val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)

            if (playbackState == ExoPlayer.STATE_READY) {
                duration.floatValue = exoPlayer.duration / 1000f
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            super.onPositionDiscontinuity(oldPosition, newPosition, reason)
            currentVideoPosition.floatValue = newPosition.positionMs / 1000f
        }

        override fun onIsPlayingChanged(playerIsPlaying: Boolean) {
            super.onIsPlayingChanged(playerIsPlaying)

            isPlaying.value = playerIsPlaying
        }
    }
    exoPlayer.addListener(listener)

    return exoPlayer
}

@UnstableApi
fun getExoPlayerLifecycleObserver(
    exoPlayer: ExoPlayer,
    isPlaying: MutableState<Boolean>,
    activity: Activity,
    absolutePath: String?
): LifecycleEventObserver =
    LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_PAUSE -> {
                exoPlayer.playWhenReady = false
                isPlaying.value = false
            }

            Lifecycle.Event.ON_DESTROY -> {
                isPlaying.value = false

                if (!activity.isChangingConfigurations) {
                    exoPlayer.stop()
                    exoPlayer.release()

                    if (absolutePath != null) {
                        // delete decrypted video if exists
                        getSecureDecryptedVideoFile(
                            name = File(absolutePath).name,
                            context = activity.applicationContext
                        ).apply {
                            if (exists()) delete()
                        }
                    }
                }
            }

            else -> {}
        }
    }


@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun rememberPlayerView(
    exoPlayer: ExoPlayer,
    activity: Activity,
    absolutePath: String?,
    useTextureView: Boolean = false
): PlayerView {
    val context = LocalContext.current
    val resources = LocalResources.current

    val backgroundColor = MaterialTheme.colorScheme.background.toArgb()

    val playerView = remember {
        PlayerView(
            context,
            if (useTextureView) resources.getXml(R.xml.custom_player_view).let {
                it.next()
                it.nextTag()
                Xml.asAttributeSet(it)
            } else {
                null
            }
        ).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            useController = false
            player = exoPlayer

            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)

            setBackgroundColor(backgroundColor)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (!activity.isChangingConfigurations) {
                playerView.player = null
                exoPlayer.release()

                if (absolutePath != null) {
                    // delete decrypted video if exists
                    getSecureDecryptedVideoFile(
                        name = File(absolutePath).name,
                        context = activity.applicationContext
                    ).apply {
                        if (exists()) delete()
                    }
                }
            }
        }
    }

    return playerView
}

fun Duration.formatLikeANormalPerson(): Pair<String, Boolean> {
    val longboi = this > 60.minutes
    val formatted = if (longboi) {
        this.toComponents { hours, minutes, seconds, _ ->
            String.format(
                java.util.Locale.ENGLISH,
                "%02d:%02d:%02d",
                hours,
                minutes,
                seconds
            )
        }
    } else {
        this.toComponents { minutes, seconds, _ ->
            String.format(
                java.util.Locale.ENGLISH,
                "%02d:%02d",
                minutes,
                seconds
            )
        }
    }
    return Pair(formatted, longboi)
}
