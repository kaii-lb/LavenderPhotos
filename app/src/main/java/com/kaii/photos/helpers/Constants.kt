package com.kaii.photos.helpers

import androidx.compose.animation.core.EaseInOutExpo
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

sealed class PhotoGridConstants {
    companion object {
        const val LOADING_TIME = 5000L
        const val UPDATE_TIME = 200L
    }
}

sealed class AnimationConstants {
    companion object {
        fun <T> expressiveSpring() = spring<T>(
            dampingRatio = 0.6f,
            stiffness = 1000f
        )

        fun <T> expressiveTween(durationMillis: Int = DURATION_SHORT) = tween<T>(
            durationMillis = durationMillis,
            easing = EaseInOutExpo
        )

        const val DURATION_SHORT = 200
        const val DURATION = 350
        const val DURATION_LONG = 600
        const val DURATION_EXTRA_LONG = 900
    }
}

sealed class VideoPlayerConstants {
    companion object {
        const val CONTROLS_HIDE_TIMEOUT = 5000L
        const val TRIM_THUMBNAIL_COUNT = 8
    }
}

sealed class TextStylingConstants {
    companion object {
        const val EXTRA_SMALL_TEXT_SIZE = 12f
        const val SMALL_TEXT_SIZE = 14f
        const val MEDIUM_TEXT_SIZE = 16f
        const val LARGE_TEXT_SIZE = 18f
        const val EXTRA_LARGE_TEXT_SIZE = 20f
        const val EXTRA_EXTRA_LARGE_TEXT_SIZE = 22f
    }
}