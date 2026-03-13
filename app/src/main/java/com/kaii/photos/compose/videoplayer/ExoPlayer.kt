package com.kaii.photos.compose.videoplayer

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.kaii.photos.helpers.getSecureDecryptedVideoFile
import java.io.File

@OptIn(UnstableApi::class)
@Composable
fun rememberExoPlayerWithLifeCycle(
    videoSource: Uri,
    absolutePath: String?,
    isPlaying: MutableState<Boolean>,
    duration: MutableFloatState,
    currentVideoPosition: MutableFloatState,
    onPlaybackStateChanged: (state: Int) -> Unit = {}
): ExoPlayer {
    val context = LocalContext.current

    val exoPlayer = remember {
        createExoPlayer(
            videoSource,
            context,
            currentVideoPosition,
            duration,
            onPlaybackStateChanged
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

@OptIn(UnstableApi::class)
fun createExoPlayer(
    videoSource: Uri,
    context: Context,
    currentVideoPosition: MutableFloatState,
    duration: MutableFloatState,
    onPlaybackStateChanged: (state: Int) -> Unit
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
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            repeatMode = ExoPlayer.REPEAT_MODE_ONE

            val dataSourceFactory = DefaultDataSource.Factory(context)

            val source = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(videoSource))

            setMediaSource(source)
            prepare()
        }

    val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)

            onPlaybackStateChanged(playbackState)

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