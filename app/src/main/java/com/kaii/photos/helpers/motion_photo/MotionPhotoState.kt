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
import com.kaii.photos.compose.videoplayer.createExoPlayer
import com.kaii.photos.compose.videoplayer.getExoPlayerLifecycleObserver
import com.kaii.photos.database.entities.MediaStoreData

@OptIn(UnstableApi::class)
class MotionPhotoState(
    uri: Uri,
    accessToken: String,
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
        item = MediaStoreData.dummyItem.copy(uri = uri.toString()),
        accessToken = accessToken,
        context = context,
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
fun rememberMotionPhotoState(uri: Uri, accessToken: String): MotionPhotoState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    return remember(uri, accessToken) {
        MotionPhotoState(
            uri = uri,
            accessToken = accessToken,
            context = context,
            lifecycle = lifecycleOwner.lifecycle
        )
    }
}