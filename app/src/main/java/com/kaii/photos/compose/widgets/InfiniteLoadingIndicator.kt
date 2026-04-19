package com.kaii.photos.compose.widgets

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.kaii.photos.helpers.AnimationConstants

fun Modifier.infiniteLoadingIndicator(
    loading: () -> Boolean
) = composed {
    val loadingIndicatorColor = MaterialTheme.colorScheme.primary
    val infiniteTransition = rememberInfiniteTransition()
    val phaseRatio by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        )
    )

    val animatedPadding by animateDpAsState(
        targetValue = if (loading()) 8.dp else 0.dp,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
    )
    val animatedStrokeWidth by animateDpAsState(
        targetValue = if (loading()) 4.dp else 0.dp,
        animationSpec = AnimationConstants.expressiveSpring()
    )

    this.then(
        Modifier
            .drawBehind {
                if (!loading()) return@drawBehind

                val strokeWidth = animatedStrokeWidth.toPx()

                val left = strokeWidth / 2f
                val top = strokeWidth / 2f
                val right = size.width - strokeWidth / 2f
                val bottom = size.height - strokeWidth / 2f

                val cornerRadius = CornerRadius((bottom - top) / 2f)
                val path = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = left, top = top,
                            right = right, bottom = bottom,
                            cornerRadius = cornerRadius
                        )
                    )
                }

                val pathMeasure = PathMeasure().apply { setPath(path, false) }
                val perimeter = pathMeasure.length

                val dashLength = perimeter * 0.4f
                val gapLength = perimeter - dashLength
                val currentPhase = -(phaseRatio * perimeter)

                val pathEffect = PathEffect.dashPathEffect(
                    intervals = floatArrayOf(dashLength, gapLength),
                    phase = currentPhase
                )

                drawPath(
                    path = path,
                    color = loadingIndicatorColor,
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = pathEffect,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
            .padding(horizontal = animatedPadding.coerceAtLeast(0.dp), vertical = 8.dp)
    )
}