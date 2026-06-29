package com.kaii.photos.helpers

import androidx.compose.animation.core.EaseInOutExpo
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object PhotoGridConstants {
    const val LOADING_TIME_SHORT = 1000L
    const val UPDATE_TIME = 200L
}

object AnimationConstants {
    fun <T> expressiveSpring() = spring<T>(
        dampingRatio = 0.6f,
        stiffness = 1000f
    )

    fun <T> expressiveTween(durationMillis: Int = DURATION_SHORT) = tween<T>(
        durationMillis = durationMillis,
        easing = EaseInOutExpo
    )

    fun <T> defaultSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    const val DURATION_SHORT = 200
    const val DURATION = 350
    const val DURATION_LONG = 600
    const val DURATION_EXTRA_LONG = 1200
    const val DURATION_EXTRA_EXTRA_LONG = 2000
}

object VideoPlayerConstants {
    const val CONTROLS_HIDE_TIMEOUT = 4000L
    const val TRIM_THUMBNAIL_COUNT = 8
    const val END_OF_VIDEO_TIMEOUT = 2000L
}

object TextStylingConstants {
    const val EXTRA_SMALL_TEXT_SIZE = 12f
    const val SMALL_TEXT_SIZE = 14f
    const val MEDIUM_TEXT_SIZE = 16f
    const val LARGE_TEXT_SIZE = 18f
    const val EXTRA_LARGE_TEXT_SIZE = 20f
    const val EXTRA_EXTRA_LARGE_TEXT_SIZE = 22f
}

object SingleViewConstants {
    const val MAX_ZOOM = 10f
    const val HALF_ZOOM = 2.5f
}