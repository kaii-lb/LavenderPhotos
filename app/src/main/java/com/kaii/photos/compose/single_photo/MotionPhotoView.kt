package com.kaii.photos.compose.single_photo

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.motion_photo.MotionPhoto
import com.kaii.photos.helpers.motion_photo.rememberMotionPhotoState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun MotionPhotoView(
    motionPhoto: MotionPhoto,
    glideImageView: @Composable (Modifier) -> Unit,
    releaseExoPlayer: MutableState<() -> Unit>?,
    modifier: Modifier = Modifier
) {
    val state = rememberMotionPhotoState(uri = motionPhoto.uri)

    LaunchedEffect(state) {
        releaseExoPlayer?.value = state::release
    }

    val context = LocalContext.current
    BackHandler {
        state.release()

        (context as Activity).finish()
    }

    val scope = rememberCoroutineScope()
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val initialDown = awaitFirstDown(requireUnconsumed = false)

                    val initialTouchHeldJob = scope.launch {
                        delay(1.seconds)
                        if (initialDown.pressed) state.play()

                        while (initialDown.pressed) {
                            delay(100.milliseconds)
                        }
                    }
                    waitForUpOrCancellation()
                    initialTouchHeldJob.cancel()

                    state.pause()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val playerView = rememberPlayerView(
            exoPlayer = state.exoPlayer,
            activity = context as Activity,
            absolutePath = null
        )

        val alpha by animateFloatAsState(
            targetValue = if (state.playing) 1f else 0f,
            animationSpec = tween(durationMillis = AnimationConstants.DURATION)
        )

        AndroidView(
            factory = {
                playerView
            },
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha)
        )

        glideImageView(Modifier.alpha(1f - alpha))
    }
}