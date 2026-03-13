package com.kaii.photos.compose.videoplayer

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.kaii.photos.R
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.helpers.formatLikeANormalPerson
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerControllerBottomControls(
    currentVideoPosition: MutableFloatState,
    duration: MutableFloatState,
    isPlaying: MutableState<Boolean>,
    exoPlayer: ExoPlayer,
    isMuted: MutableState<Boolean>,
    onAnyTap: () -> Unit,
    setLastWasMuted: (Boolean) -> Unit
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
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(1000.dp))
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
                )
            )
        }

        // mute button
        FilledTonalIconButton(
            onClick = {
                isMuted.value = !isMuted.value
                setLastWasMuted(isMuted.value)

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