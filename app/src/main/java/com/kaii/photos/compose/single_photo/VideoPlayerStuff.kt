package com.kaii.photos.compose.single_photo

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.navigation.NavHostController
import com.kaii.photos.R
import com.kaii.photos.MainActivity.Companion.mainViewModel
import com.kaii.photos.compose.setBarVisibility
import com.kaii.photos.datastore.Video
import com.kaii.photos.helpers.CustomMaterialTheme
import com.kaii.photos.mediastore.MediaStoreData
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// special thanks to @bedirhansaricayir on github, helped with a LOT of performance stuff
// https://github.com/bedirhansaricayir/Instagram-Reels-Jetpack-Compose/blob/master/app/src/main/java/com/reels/example/presentation/components/ExploreVideoPlayer.kt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerControls(
    exoPlayer: ExoPlayer,
    isPlaying: MutableState<Boolean>,
    isMuted: MutableState<Boolean>,
    currentVideoPosition: MutableFloatState,
    duration: MutableFloatState,
    title: String,
    modifier: Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    BoxWithConstraints(
        modifier = Modifier
            .then(modifier)
            .background(Color.Transparent)
    ) {
        val localConfig = LocalConfiguration.current
        var isLandscape by remember { mutableStateOf(localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) }

        LaunchedEffect(localConfig) {
            isLandscape = localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        }

        if (isLandscape) {
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = if (title == "") "Media" else title,
                    fontSize = TextUnit(12f, TextUnitType.Sp),
                    color = CustomMaterialTheme.colorScheme.onSecondaryContainer,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .widthIn(max = this@BoxWithConstraints.maxWidth / 2)
                        .wrapContentSize()
                        .clip(RoundedCornerShape(1000.dp))
                        .background(CustomMaterialTheme.colorScheme.secondaryContainer)
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
            Row(
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .wrapContentHeight()
                    .padding(16.dp, 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                val currentDurationFormatted = currentVideoPosition.floatValue.roundToInt().seconds.formatLikeANormalPerson()

                Row(
                    modifier = Modifier
                        .height(32.dp)
                        .width(if (currentDurationFormatted.second) 72.dp else 48.dp)
                        .clip(RoundedCornerShape(1000.dp))
                        .background(CustomMaterialTheme.colorScheme.secondaryContainer)
                        .padding(4.dp, 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = currentDurationFormatted.first,
                        style = TextStyle(
                            fontSize = TextUnit(12f, TextUnitType.Sp),
                            color = CustomMaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                        ),
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                duration.floatValue = duration.floatValue.coerceAtLeast(0f)

                Slider(
                    value = currentVideoPosition.floatValue,
                    valueRange = 0f..duration.floatValue,
                    onValueChange = { pos ->
                        val prev = isPlaying.value
                        exoPlayer.seekTo(
                            (pos * 1000f).coerceAtMost(duration.floatValue * 1000f).toLong()
                        )
                        isPlaying.value = prev
                    },
                    steps = (duration.floatValue.roundToInt() - 1).coerceAtLeast(0),
                    thumb = {
                        SliderDefaults.Thumb(
                            interactionSource = interactionSource,
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
                    // interactionSource = interactionSource,
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                val formattedDuration = duration.floatValue.roundToInt().seconds.formatLikeANormalPerson()

                Row(
                    modifier = Modifier
                        .height(32.dp)
                        .width(if (formattedDuration.second) 72.dp else 48.dp)
                        .clip(RoundedCornerShape(1000.dp))
                        .background(CustomMaterialTheme.colorScheme.secondaryContainer)
                        .padding(4.dp, 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = formattedDuration.first,
                        style = TextStyle(
                            fontSize = TextUnit(12f, TextUnitType.Sp),
                            color = CustomMaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                        ),
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                FilledTonalIconButton(
                    onClick = {
                        isMuted.value = !isMuted.value
                    },
                    modifier = Modifier
                        .size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(id = if (isMuted.value) R.drawable.volume_mute else R.drawable.volume_max),
                        contentDescription = "Video player mute or un-mute",
                        modifier = Modifier
                            .size(24.dp)
                    )
                }
            }
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
                },
                modifier = Modifier
                    .size(48.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.fast_rewind),
                    contentDescription = "Video player skip back 5 seconds",
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 2.dp, 0.dp)
                )
            }

            Spacer(modifier = Modifier.width(48.dp))

            FilledTonalIconButton(
                onClick = {
                    isPlaying.value = !isPlaying.value
                },
                modifier = Modifier
                    .size(48.dp)
            ) {
                Icon(
                    painter = painterResource(id = if (!isPlaying.value) R.drawable.play_arrow else R.drawable.pause),
                    contentDescription = "Video player play or pause"
                )
            }

            Spacer(modifier = Modifier.width(48.dp))

            FilledTonalIconButton(
                onClick = {
                    val prev = isPlaying.value
                    exoPlayer.seekForward()
                    isPlaying.value = prev
                },
                modifier = Modifier
                    .size(48.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.fast_forward),
                    contentDescription = "Video player skip forward 5 seconds",
                    modifier = Modifier
                        .padding(2.dp, 0.dp, 0.dp, 0.dp)
                )
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    item: MediaStoreData,
    controlsVisible: MutableState<Boolean>,
    appBarsVisible: MutableState<Boolean>,
    shouldPlay: Boolean,
    shouldAutoPlay: Boolean,
    lastWasMuted: MutableState<Boolean>,
    navController: NavHostController,
    canFadeControls: MutableState<Boolean>,
    isTouchLocked: MutableState<Boolean>,
    window: Window,
    modifier: Modifier
) {
    val isPlaying = rememberSaveable { mutableStateOf(false) }
    val lastIsPlaying = rememberSaveable { mutableStateOf(isPlaying.value) }

    val isMuted = rememberSaveable { mutableStateOf(lastWasMuted.value) }

    /** In Seconds */
    val currentVideoPosition = rememberSaveable { mutableFloatStateOf(0f) }
    val duration = rememberSaveable { mutableFloatStateOf(0f) }

    var exoPlayer = rememberExoPlayerWithLifeCycle(item.uri, isPlaying, duration, currentVideoPosition)
    val playerView = rememberPlayerView(exoPlayer)

    BackHandler {
        isPlaying.value = false
        currentVideoPosition.floatValue = 0f
        duration.floatValue = 0f

        exoPlayer.stop()
        exoPlayer.release()

        navController.popBackStack()
    }

    val localConfig = LocalConfiguration.current

    LaunchedEffect(key1 = isPlaying.value, localConfig.orientation) {
        if (!isPlaying.value) {
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
            exoPlayer.pause()
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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

        while (isPlaying.value) {
            currentVideoPosition.floatValue = exoPlayer.currentPosition / 1000f

            delay(1000)
        }
    }


    LaunchedEffect(controlsVisible.value) {
        if (controlsVisible.value) canFadeControls.value = true
    }

    LaunchedEffect(isMuted.value) {
    	lastWasMuted.value = isMuted.value

		exoPlayer.volume = if (isMuted.value) 0f else 1f
   		exoPlayer.setHandleAudioBecomingNoisy(!isMuted.value)
    }

    LaunchedEffect(shouldAutoPlay, shouldPlay) {
    	exoPlayer.playWhenReady = shouldAutoPlay && shouldPlay
    }

    Box(
        modifier = Modifier
            .fillMaxSize(1f)
    ) {
        Column(
            modifier = modifier.then(Modifier.align(Alignment.Center)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AndroidView(
                factory = {
                	playerView
                }
            )
        }

        val title = remember { File(item.absolutePath).nameWithoutExtension }
        AnimatedVisibility(
            visible = controlsVisible.value,
            enter = expandIn(
                animationSpec = tween(
                    durationMillis = 350
                )
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = 350
                )
            ),
            exit = shrinkOut(
                animationSpec = tween(
                    durationMillis = 350
                )
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = 350
                )
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
                    .fillMaxSize(1f)
            )
        }

        if ((isTouchLocked.value || controlsVisible.value) && localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                FilledTonalIconToggleButton(
                    checked = isTouchLocked.value,
                    onCheckedChange = {
                        isTouchLocked.value = it
                        canFadeControls.value = true
                    },
                    colors = IconButtonDefaults.filledTonalIconToggleButtonColors().copy(
                        checkedContainerColor = CustomMaterialTheme.colorScheme.primary,
                        checkedContentColor = CustomMaterialTheme.colorScheme.onPrimary,
                        containerColor = CustomMaterialTheme.colorScheme.secondaryContainer,
                        contentColor = CustomMaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.Center)
                ) {
                    Icon(
                        painter = painterResource(id = if (isTouchLocked.value) R.drawable.locked_folder else R.drawable.unlock),
                        contentDescription = "Lock the screen preventing miss-touch",
                        modifier = Modifier
                            .size(20.dp)
                    )
                }
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun rememberExoPlayerWithLifeCycle(
    videoSource: Uri,
    isPlaying: MutableState<Boolean>,
    duration: MutableFloatState,
    currentVideoPosition: MutableFloatState
): ExoPlayer {
    val context = LocalContext.current

    var exoPlayer = remember(videoSource) {
        createExoPlayer(
            videoSource,
            context,
            isPlaying,
            currentVideoPosition,
            duration
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(key1 = lifecycleOwner.lifecycle.currentState) {
        val lifecycleObserver = getExoPlayerLifecycleObserver(exoPlayer, isPlaying, context as Activity)

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
            true
        )
    }
        .build()
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
    activity: Activity
): LifecycleEventObserver =
    LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_PAUSE -> {
                exoPlayer.playWhenReady = false
                isPlaying.value = false
            }

            Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> {
                isPlaying.value = false

                if (!activity.isChangingConfigurations) {
                    exoPlayer.stop()
                    exoPlayer.release()
                }
            }

            else -> {}
        }
    }


@UnstableApi
@Composable
fun rememberPlayerView(exoPlayer: ExoPlayer): PlayerView {
    val context = LocalContext.current

    val playerView = remember {
        PlayerView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            useController = false
            // resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            player = exoPlayer

            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
        }
    }
    DisposableEffect(key1 = true) {
        onDispose {
            playerView.player = null
            exoPlayer.release()
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
