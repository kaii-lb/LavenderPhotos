package com.kaii.photos.helpers.motion_photo

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.kaii.photos.compose.single_photo.createExoPlayer
import com.kaii.photos.compose.single_photo.getExoPlayerLifecycleObserver

@OptIn(UnstableApi::class)
class MotionPhotoState(
    uri: Uri,
    private val context: Context,
    private val lifecycle: Lifecycle
) : RememberObserver {
    private val isPlaying = mutableStateOf(false)
    private val currentVideoPosition = mutableFloatStateOf(0f)
    private val duration = mutableFloatStateOf(0f)
    private var observer: LifecycleObserver? = null

    val playing by derivedStateOf { isPlaying.value }

    val exoPlayer = createExoPlayer(
        videoSource = uri,
        context = context,
        isPlaying = isPlaying,
        currentVideoPosition = currentVideoPosition,
        duration = duration,
        onPlaybackStateChanged = {}
    ).apply {
        repeatMode = Player.REPEAT_MODE_ONE
        pauseAtEndOfMediaItems = false
    }

    fun play() {
        isPlaying.value = true
        exoPlayer.play()
    }

    fun pause() {
        isPlaying.value = false
        exoPlayer.pause()
        exoPlayer.seekTo(0)
    }

    fun release() {
        exoPlayer.stop()
        exoPlayer.release()
    }

    @OptIn(UnstableApi::class)
    override fun onRemembered() {
        if (observer == null) {
            observer = getExoPlayerLifecycleObserver(
                exoPlayer = exoPlayer,
                isPlaying = isPlaying,
                activity = context as Activity,
                absolutePath = null
            )
            lifecycle.addObserver(observer!!)
        }
    }

    override fun onForgotten() {
        observer?.let {
            lifecycle.removeObserver(it)
            observer = null
        }
    }

    override fun onAbandoned() {
        observer?.let {
            lifecycle.removeObserver(it)
            observer = null
        }
    }
}

@Composable
fun rememberMotionPhotoState(uri: Uri): MotionPhotoState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    return remember {
        MotionPhotoState(
            uri = uri,
            context = context,
            lifecycle = lifecycleOwner.lifecycle
        )
    }
}