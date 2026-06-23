package com.kaii.photos.compose.modifiers

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

/** @param ratio the % of the item's width to wiggle */
fun Modifier.wiggle(
    start: Boolean,
    ratio: Float = 0.1f,
    durationMillis: Int = 500,
    times: Int = 6
) = composed {
    val wiggle = remember { Animatable(0f) }

    val animationSpec = tween<Float>(
        durationMillis = durationMillis / times,
        easing = CubicBezierEasing(0.38f, 0.51f, 0.34f, 1.45f)
    )

    LaunchedEffect(start) {
        if (!start) return@LaunchedEffect

        for (i in 1..times) {
            when (i % 2) {
                0 -> wiggle.animateTo(
                    targetValue = 1f,
                    animationSpec = animationSpec
                )

                else -> wiggle.animateTo(
                    targetValue = -1f,
                    animationSpec = animationSpec
                )
            }
        }

        wiggle.animateTo(
            targetValue = 0f,
            animationSpec = animationSpec
        )
    }

    var translateX by remember { mutableIntStateOf(0) }

    this
        .onGloballyPositioned {
            translateX = (it.size.width * ratio).roundToInt()
        }
        .offset {
            IntOffset(
                (wiggle.value * translateX).roundToInt(),
                0
            )
        }
}