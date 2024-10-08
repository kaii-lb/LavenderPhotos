package com.kaii.photos.compose.single_photo

import android.net.Uri
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C.VIDEO_SCALING_MODE_SCALE_TO_FIT
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.kaii.photos.R
import com.kaii.photos.compose.CustomMaterialTheme
import com.kaii.photos.mediastore.MediaStoreData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

// special thanks to @bedirhansaricayir on github, helped a LOT of performance
// https://github.com/bedirhansaricayir/Instagram-Reels-Jetpack-Compose/blob/master/app/src/main/java/com/reels/example/presentation/components/ExploreVideoPlayer.kt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerControls(
    exoPlayer: ExoPlayer,
    isPlaying: MutableState<Boolean>,
    isMuted: MutableState<Boolean>,
    currentVideoPosition: MutableState<Float>,
    duration: MutableState<Float>,
    modifier: Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box (
        modifier = Modifier
            .then(modifier)
            .background(Color.Transparent)
    ) {
        Row (
            modifier = Modifier
                .height(172.dp)
                .align(Alignment.BottomCenter),
            verticalAlignment = Alignment.Top
        ) {
            Row (
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .wrapContentHeight()
                    .padding(8.dp, 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Row (
                    modifier = Modifier
                        .height(32.dp)
                        .wrapContentWidth()
                        .clip(RoundedCornerShape(1000.dp))
                        .background(CustomMaterialTheme.colorScheme.secondaryContainer)
                        .padding(4.dp, 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text (
                        text = currentVideoPosition.value.roundToInt().seconds.toString(),
                        style = TextStyle(
                            fontSize = TextUnit(12f, TextUnitType.Sp),
                            color = CustomMaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                        ),
                    )
                }

                Spacer (modifier = Modifier.width(8.dp))

                duration.value = if (duration.value < 0) 0f else duration.value
                println("DURATION IS ${duration.value}")

                Slider (
                    value = currentVideoPosition.value,
                    valueRange = 0f..duration.value,
                    onValueChange = { pos ->
                        exoPlayer.seekTo((pos * 1000).toLong())
                    },
                    steps = duration.value.toInt(),
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

                Spacer (modifier = Modifier.width(8.dp))

                Row (
                    modifier = Modifier
                        .height(32.dp)
                        .wrapContentWidth()
                        .clip(RoundedCornerShape(1000.dp))
                        .background(CustomMaterialTheme.colorScheme.secondaryContainer)
                        .padding(4.dp, 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text (
                        text = duration.value.toInt().seconds.toString(),
                        style = TextStyle(
                            fontSize = TextUnit(12f, TextUnitType.Sp),
                            color = CustomMaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                        ),
                    )
                }

                Spacer (modifier = Modifier.width(8.dp))

                FilledTonalIconButton(
                    onClick = {
                        isMuted.value = !isMuted.value
                    },
                    modifier = Modifier
                        .size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(id = if(isMuted.value) R.drawable.volume_mute else R.drawable.volume_max),
                        contentDescription = "Video player mute or un-mute",
                        modifier = Modifier
                            .size(24.dp)
                    )
                }
            }
        }

        Row (
            modifier = Modifier
                .fillMaxWidth(1f)
                .wrapContentHeight()
                .align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            FilledTonalIconButton(
                onClick = {
                    exoPlayer.seekBack()
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

            Spacer (modifier = Modifier.width(48.dp))

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

            Spacer (modifier = Modifier.width(48.dp))

            FilledTonalIconButton(
                onClick = {
                    exoPlayer.seekForward()
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
    visible: Boolean,
    shouldPlay: Boolean,
    modifier: Modifier
) {
    val isPlaying = remember { mutableStateOf(false) }
    val isMuted = remember { mutableStateOf(false) }
    val currentVideoPosition = remember { mutableFloatStateOf(0f) }
	val duration = remember { mutableFloatStateOf(0f) }

    val exoPlayer = rememberExoPlayerWithLifeCycle(item.uri, isPlaying, duration, currentVideoPosition)
    val playerView = rememberPlayerView(exoPlayer)
    
    LaunchedEffect(key1 = isPlaying.value) {
        while(isPlaying.value) {
        	currentVideoPosition.value = (exoPlayer.currentPosition / 1000f).roundToInt().toFloat()
            delay(1000)
        }
    }

    Box (
        modifier = Modifier
            .fillMaxSize(1f)
    ) {
	    Column (
	    	modifier = modifier.then(Modifier.align(Alignment.Center)),
	        verticalArrangement = Arrangement.Center,
	        horizontalAlignment = Alignment.CenterHorizontally
	    ) {
	        AndroidView (
	            factory = { playerView },
	            update = {
			        exoPlayer.volume = if (isMuted.value) 0f else 1f

			        exoPlayer.playWhenReady = shouldPlay && isPlaying.value
	            },
	        )
	    }
		
		AnimatedVisibility(
	        visible = visible,
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
	            modifier = Modifier
	                .fillMaxSize(1f)
	        )
	    }	    
    }
}

@UnstableApi
@Composable
fun rememberExoPlayerWithLifeCycle(
    videoSource: Uri,
    isPlaying: MutableState<Boolean>,
    duration: MutableState<Float>,
    currentVideoPosition: MutableState<Float>
): ExoPlayer {
    val context = LocalContext.current

    val exoPlayer = remember(videoSource) {
        ExoPlayer.Builder(context).apply {
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
                // pause on video end   	
        	}
            .build()
            .apply {
                videoScalingMode = VIDEO_SCALING_MODE_SCALE_TO_FIT
                repeatMode = ExoPlayer.REPEAT_MODE_ONE

                setHandleAudioBecomingNoisy(true)

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
    }

    val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)

            if (playbackState == ExoPlayer.STATE_READY) {
                duration.value = (exoPlayer.duration / 1000f).roundToInt().toFloat()
            } else if (playbackState == ExoPlayer.STATE_ENDED) {
            	exoPlayer.pause()
                exoPlayer.seekTo(0L)
                isPlaying.value = false
                currentVideoPosition.value = 0f
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            super.onPositionDiscontinuity(oldPosition, newPosition, reason)
            currentVideoPosition.value = (newPosition.positionMs / 1000f).roundToInt().toFloat()
        }
    }
    exoPlayer.addListener(listener)

    val lifecycleOwner = LocalLifecycleOwner.current
    var appInBackground by remember { mutableStateOf(false) }

    DisposableEffect(key1 = lifecycleOwner, appInBackground) {
        val lifecycleObserver = getExoPlayerLifecycleObserver(exoPlayer, appInBackground, isPlaying) {
            appInBackground = it
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
        	// its insta-disposing for some reason
            // lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }
    return exoPlayer
}

fun getExoPlayerLifecycleObserver(
    exoPlayer: ExoPlayer,
    wasAppInBackground: Boolean,
    isPlaying: MutableState<Boolean>,
    setWasAppInBackground: (Boolean) -> Unit
): LifecycleEventObserver =
    LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                if (wasAppInBackground) {
                	exoPlayer.playWhenReady = true
                }
                isPlaying.value = true
                setWasAppInBackground(false)
            }

            Lifecycle.Event.ON_PAUSE -> {
                exoPlayer.playWhenReady = false
                isPlaying.value = false
                setWasAppInBackground(true)
            }

            Lifecycle.Event.ON_STOP -> {
                exoPlayer.playWhenReady = false
                isPlaying.value = false
                setWasAppInBackground(true)
            }

            Lifecycle.Event.ON_DESTROY -> {
            	isPlaying.value = false
            	exoPlayer.stop()
                exoPlayer.release()
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
        }
    }
    return playerView
}
