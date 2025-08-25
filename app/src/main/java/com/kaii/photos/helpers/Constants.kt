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

        fun <T> expressiveTween() = tween<T>(
            durationMillis = 200,
            easing = EaseInOutExpo
        )

        const val DURATION = 350
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
        const val SMALL_TEXT_SIZE = 12f
    }
}