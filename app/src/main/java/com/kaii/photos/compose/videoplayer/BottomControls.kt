package com.kaii.photos.compose.videoplayer

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.kaii.photos.R
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.helpers.formatLikeANormalPerson
import com.kaii.photos.screens.video.LavenderExoPlayer
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerControllerBottomControls(
    currentVideoPosition: () -> Float,
    duration: () -> Float,
    isMuted: () -> Boolean,
    isRepeatModeOn: () -> Boolean,
    playbackSpeed: () -> Float,
    audioTracks: () -> List<LavenderExoPlayer.AudioTrack>,
    selectedAudioTrack: () -> LavenderExoPlayer.AudioTrack?,
    onAnyTap: () -> Unit,
    seekTo: (position: Long) -> Unit,
    toggleMute: () -> Unit,
    toggleRepeatMode: () -> Unit,
    setPlaybackSpeed: (speed: Float) -> Unit,
    setAudioTrack: (language: String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 4.dp),
        horizontalAlignment = Alignment.End
    ) {
        Row(
            modifier = Modifier
                .wrapContentSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                space = 8.dp,
                alignment = Alignment.End
            )
        ) {
            AnimatedVisibility(
                visible = audioTracks().size > 1,
                enter = fadeIn() + scaleIn(
                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
                ),
                exit = fadeOut() + scaleOut()
            ) {
                var showTrackSelector by remember { mutableStateOf(false) }

                AudioSelectorMenu(
                    expanded = { showTrackSelector },
                    selectedTrack = selectedAudioTrack,
                    tracks = audioTracks,
                    setTrack = setAudioTrack,
                    onDismiss = {
                        showTrackSelector = false
                    }
                )

                // track selector button
                FilledTonalIconToggleButton(
                    checked = showTrackSelector,
                    onCheckedChange = {
                        showTrackSelector = true

                        onAnyTap()
                    },
                    modifier = Modifier
                        .size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.graphic_eq),
                        contentDescription = stringResource(id = R.string.video_change_audio_track),
                        modifier = Modifier
                            .size(24.dp)
                    )
                }
            }

            val isLandscape by rememberDeviceOrientation()
            Spacer(
                modifier =
                    if (!isLandscape) Modifier.weight(1f)
                    else Modifier
            )

            // repeat mode button
            FilledTonalIconToggleButton(
                checked = isRepeatModeOn(),
                onCheckedChange = {
                    toggleRepeatMode()

                    onAnyTap()
                },
                modifier = Modifier
                    .size(32.dp)
            ) {
                Icon(
                    painter = painterResource(id = if (isRepeatModeOn()) R.drawable.repeat_one else R.drawable.repeat),
                    contentDescription = stringResource(id = R.string.video_mute_toggle),
                    modifier = Modifier
                        .size(24.dp)
                )
            }

            // mute button
            FilledTonalIconToggleButton(
                checked = isMuted(),
                onCheckedChange = {
                    toggleMute()

                    onAnyTap()
                },
                modifier = Modifier
                    .size(32.dp)
            ) {
                Icon(
                    painter = painterResource(id = if (isMuted()) R.drawable.volume_mute else R.drawable.volume_max),
                    contentDescription = stringResource(id = R.string.video_mute_toggle),
                    modifier = Modifier
                        .size(24.dp)
                )
            }


            var currentPlaybackSpeed by remember { mutableFloatStateOf(playbackSpeed()) }

            // speed button
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

                    setPlaybackSpeed(new)
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val currentDurationFormatted =
                currentVideoPosition().roundToInt().seconds.formatLikeANormalPerson()

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


            VideoPlayerSeekbar(
                currentPosition = currentVideoPosition,
                duration = duration,
                modifier = Modifier
                    .weight(1f)
            ) { pos ->
                onAnyTap()

                seekTo(
                    (pos * 1000f).coerceAtMost(duration() * 1000f).toLong()
                )
            }

            val formattedDuration =
                duration().coerceAtLeast(0f).roundToInt().seconds.formatLikeANormalPerson()

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
        }
    }
}