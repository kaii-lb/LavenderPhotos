package com.kaii.photos.helpers

import androidx.compose.animation.core.EaseInOutExpo
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.core.net.toUri
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType

object PhotoGridConstants {
    const val LOADING_TIME = 5000L
    const val LOADING_TIME_SHORT = 1000L
    const val UPDATE_TIME = 200L

    val placeholderItems = listOf(
        MediaStoreData(
            type = MediaType.Section,
            dateModified = 0L,
            dateTaken = 0L,
            uri = "".toUri(),
            displayName = "",
            id = 1L,
            mimeType = null,
            section = SectionItem(
                date = 0L,
                childCount = 0
            )
        ),
        MediaStoreData(id = 2L),
        MediaStoreData(id = 3L),
        MediaStoreData(id = 4L),
        MediaStoreData(id = 5L),
        MediaStoreData(id = 6L),
        MediaStoreData(id = 7L),
        MediaStoreData(
            type = MediaType.Section,
            dateModified = 0L,
            dateTaken = 0L,
            uri = "".toUri(),
            displayName = "",
            id = 8L,
            mimeType = null,
            section = SectionItem(
                date = 0L,
                childCount = 0
            )
        ),
        MediaStoreData(id = 9L),
        MediaStoreData(id = 10L),
        MediaStoreData(id = 11L),
        MediaStoreData(id = 12L),
        MediaStoreData(id = 13L),
        MediaStoreData(id = 14L),
        MediaStoreData(id = 15L),
        MediaStoreData(id = 16L),
        MediaStoreData(
            type = MediaType.Section,
            dateModified = 0L,
            dateTaken = 0L,
            uri = "".toUri(),
            displayName = "",
            id = 17L,
            mimeType = null,
            section = SectionItem(
                date = 0L,
                childCount = 0
            )
        ),
        MediaStoreData(id = 18L),
        MediaStoreData(id = 19L),
        MediaStoreData(
            type = MediaType.Section,
            dateModified = 0L,
            dateTaken = 0L,
            uri = "".toUri(),
            displayName = "",
            id = 20L,
            mimeType = null,
            section = SectionItem(
                date = 0L,
                childCount = 0
            )
        ),
        MediaStoreData(id = 21L),
        MediaStoreData(id = 22L),
        MediaStoreData(id = 23L),
        MediaStoreData(id = 24L),
        MediaStoreData(id = 25L),
        MediaStoreData(id = 26L),
        MediaStoreData(id = 27L),
        MediaStoreData(id = 28L),
        MediaStoreData(id = 29L),
        MediaStoreData(id = 30L),
    )
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

    const val DURATION_SHORT = 200
    const val DURATION = 350
    const val DURATION_LONG = 600
    const val DURATION_EXTRA_LONG = 1200
}


object VideoPlayerConstants {
    const val CONTROLS_HIDE_TIMEOUT = 4000L
    const val TRIM_THUMBNAIL_COUNT = 8
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
    const val MAX_ZOOM = 5f
}