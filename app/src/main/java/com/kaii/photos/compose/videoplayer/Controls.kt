package com.kaii.photos.compose.videoplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import com.kaii.photos.R
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.helpers.TextStylingConstants

@Composable
fun VideoPlayerControls(
    exoPlayer: ExoPlayer,
    isPlaying: MutableState<Boolean>,
    isMuted: MutableState<Boolean>,
    currentVideoPosition: MutableFloatState,
    duration: MutableFloatState,
    title: String,
    modifier: Modifier,
    onAnyTap: () -> Unit,
    setLastWasMuted: (Boolean) -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .background(Color.Transparent)
    ) {
        val isLandscape by rememberDeviceOrientation()

        if (isLandscape) {
            // title box
            @Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = if (title == "") stringResource(id = R.string.media) else title,
                    fontSize = TextStylingConstants.EXTRA_SMALL_TEXT_SIZE.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .widthIn(max = this@BoxWithConstraints.maxWidth / 2)
                        .wrapContentSize()
                        .clip(CircleShape)
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
                onAnyTap = onAnyTap,
                setLastWasMuted = setLastWasMuted
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