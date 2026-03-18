package com.kaii.photos.compose.videoplayer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.kaii.photos.helpers.AnimationConstants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerSeekbar(
    currentPosition: () -> Float,
    duration: () -> Float,
    modifier: Modifier = Modifier,
    onValueChangeFinished: () -> Unit = {},
    onValueChange: (Float) -> Unit
) {
    val localInteractionSource = remember { MutableInteractionSource() }
    val isDraggingSlider by localInteractionSource.collectIsDraggedAsState()

    val animatedPosition by animateFloatAsState(
        targetValue = currentPosition(),
        animationSpec =
            if (isDraggingSlider) snap()
            else tween(
                durationMillis = AnimationConstants.DURATION,
                easing = LinearEasing
            )
    )

    Slider(
        value = animatedPosition,
        valueRange = 0f..duration().coerceAtLeast(0f),
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        thumb = {
            SliderDefaults.Thumb(
                interactionSource = localInteractionSource,
                thumbSize = DpSize(6.dp, 20.dp),
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
                    .height(20.dp)
            )
        },
        interactionSource = localInteractionSource,
        modifier = modifier
            .height(32.dp)
    )
}