package com.kaii.photos.compose.single_photo

import android.widget.FrameLayout.LayoutParams
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.kaii.photos.R
import com.kaii.photos.compose.CustomMaterialTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerControls(
	exoPlayer: ExoPlayer,
    showControls: MutableState<Boolean>,
    isPlaying: MutableState<Boolean>,
    isMuted: MutableState<Boolean>,
    currentVideoPosition: MutableState<Float>,
    modifier: Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    AnimatedVisibility(
        visible = showControls.value,
        enter = expandIn(
            animationSpec = tween(
                durationMillis = 500
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = 500
            )
        ),
        exit = shrinkOut(
            animationSpec = tween(
                durationMillis = 500
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = 500
            )
        ),
        modifier = modifier.then(Modifier.background(Color.Transparent))
    ) {
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
                            text = "00:00",
                            style = TextStyle(
                                fontSize = TextUnit(12f, TextUnitType.Sp),
                                color = CustomMaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                            ),
                        )
                    }

                    Spacer (modifier = Modifier.width(8.dp))

					var duration = (exoPlayer.duration / 1000).toFloat().roundToInt().toFloat()
					duration = if (duration < 0) 0f else duration
					println("DURATION IS $duration")
                    Slider (
                        value = currentVideoPosition.value,
                        valueRange = 0f..duration,
                        onValueChange = { pos ->
                            currentVideoPosition.value = pos
                        },
                        steps = duration.toInt(),
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
                            text = "02:34",
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
                        currentVideoPosition.value = currentVideoPosition.value - 5f
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
                        currentVideoPosition.value = currentVideoPosition.value + 5f
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
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(exoPlayer: ExoPlayer, modifier: Modifier) {
    val context = LocalContext.current

	Column (
		modifier = modifier,
	) {
	    AndroidView(
	        factory = {
	            PlayerView(context).apply{
	                player = exoPlayer
	                layoutParams = LayoutParams(
	                    LayoutParams.MATCH_PARENT,
	                    LayoutParams.MATCH_PARENT
	                )
	                
	                useController = false
	                setShutterBackgroundColor(Color.Transparent.toArgb())
	                controllerAutoShow = false
	                setControllerShowTimeoutMs(0)
	                setControllerAnimationEnabled(false)
	            }
	        }
	    )
	}
}
