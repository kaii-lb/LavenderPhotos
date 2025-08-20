package com.kaii.photos.compose.single_photo.editing_view

import android.app.Activity
import android.net.Uri
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.VideoEditorBottomBar
import com.kaii.photos.compose.app_bars.VideoEditorTopBar
import com.kaii.photos.compose.single_photo.rememberExoPlayerWithLifeCycle
import com.kaii.photos.compose.single_photo.rememberPlayerView
import com.kaii.photos.helpers.VideoPlayerConstants
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditor(
    uri: Uri,
    absolutePath: String,
    window: Window
) {
    val isPlaying = remember { mutableStateOf(false) }

    /** In Seconds */
    val currentVideoPosition = remember { mutableFloatStateOf(0f) }
    val duration = remember { mutableFloatStateOf(0f) }

    val exoPlayer = rememberExoPlayerWithLifeCycle(
        videoSource = uri,
        absolutePath = absolutePath,
        isPlaying = isPlaying,
        duration = duration,
        currentVideoPosition = currentVideoPosition
    )

    var controlsVisible by remember { mutableStateOf(true) }
    var taps by remember { mutableIntStateOf(0) }

    LaunchedEffect(taps) {
        delay(VideoPlayerConstants.CONTROLS_HIDE_TIMEOUT_SHORT)
        controlsVisible = false

        taps = 0
    }

    val leftTrimPosition = remember { mutableFloatStateOf(0f) }
    val rightTrimPosition = remember { mutableFloatStateOf(duration.floatValue) }

    LaunchedEffect(duration.floatValue) {
        rightTrimPosition.floatValue = duration.floatValue
    }

    LaunchedEffect(isPlaying.value) {
        if (!isPlaying.value) {
            controlsVisible = true
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            exoPlayer.pause()
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            exoPlayer.play()
        }

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

    val modifications = remember { mutableStateListOf<VideoModification>() }
    val lastSavedModCount =
        remember { mutableIntStateOf(2) } // starts at 2 cuz of below launchedeffect, and then 1 more for setting rightposition to duration

    LaunchedEffect(leftTrimPosition.floatValue, rightTrimPosition.floatValue) {
        modifications.add(
            VideoModification.VideoTrimModification(
                start = leftTrimPosition.floatValue,
                end = rightTrimPosition.floatValue
            )
        )
    }

    Scaffold(
        topBar = {
            VideoEditorTopBar(
                uri = uri,
                absolutePath = absolutePath,
                modifications = modifications,
                lastSavedModCount = lastSavedModCount
            )
        },
        bottomBar = {
            VideoEditorBottomBar(
                currentPosition = currentVideoPosition,
                duration = duration,
                absolutePath = absolutePath,
                exoPlayer = exoPlayer,
                leftPosition = leftTrimPosition,
                rightPosition = rightTrimPosition
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
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(1f)
            ) {
                val context = LocalContext.current
                val playerView = rememberPlayerView(
                    exoPlayer = exoPlayer,
                    activity = context as Activity,
                    absolutePath = absolutePath
                )

                AndroidView(
                    factory = {
                        playerView
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                )

                FilledTonalIconButton(
                    onClick = {
                        isPlaying.value = !isPlaying.value
                        taps += 1
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                ) {
                    Icon(
                        painter = painterResource(id = if (isPlaying.value) R.drawable.pause else R.drawable.play_arrow),
                        contentDescription = stringResource(id = R.string.video_play_toggle)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

