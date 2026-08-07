package com.kaii.photos.compose.single_photo

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.kaii.photos.helpers.AnimationConstants

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun BlurredBackdrop(
    model: Any?,
    signature: () -> ObjectKey,
    useCache: () -> Boolean,
    modifier: Modifier = Modifier,
    scaleFactor: Int = 6
) {
    var targetAlpha by remember { mutableFloatStateOf(0f) }
    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(
            durationMillis = AnimationConstants.DURATION
        )
    )

    LaunchedEffect(Unit) {
        targetAlpha = 0.5f
    }

    // scale up a smaller blurred area for performance reasons
    val windowSize = LocalWindowInfo.current.containerSize / scaleFactor
    GlideImage(
        model = model,
        contentScale = ContentScale.Crop,
        contentDescription = null,
        modifier = modifier
            .graphicsLayer {
                scaleX = scaleFactor.toFloat()
                scaleY = scaleFactor.toFloat()
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .size(
                with(LocalDensity.current) {
                    DpSize(
                        windowSize.width.toDp(),
                        windowSize.height.toDp()
                    )
                }
            )
            .blur(12.dp)
            .alpha(animatedAlpha)
    ) {
        it.signature(signature())
            .diskCacheStrategy(if (useCache()) DiskCacheStrategy.ALL else DiskCacheStrategy.NONE)
            .override(windowSize.width, windowSize.height)
    }
}