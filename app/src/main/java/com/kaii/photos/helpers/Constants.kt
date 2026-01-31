package com.kaii.photos.helpers

import androidx.compose.animation.core.EaseInOutExpo
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.mediastore.PhotoLibraryUIModel

object PhotoGridConstants {
    const val LOADING_TIME = 5000L
    const val LOADING_TIME_SHORT = 1000L
    const val UPDATE_TIME = 200L

    val placeholderItems = listOf(
        PhotoLibraryUIModel.Section(title = ""),
        PhotoLibraryUIModel.Media(item = MediaStoreData.dummyItem.copy(id = 2L)),
        PhotoLibraryUIModel.Media(item = MediaStoreData.dummyItem.copy(id = 3L)),
        PhotoLibraryUIModel.Media(item = MediaStoreData.dummyItem.copy(id = 4L)),
        PhotoLibraryUIModel.Media(item = MediaStoreData.dummyItem.copy(id = 5L)),
        PhotoLibraryUIModel.Media(item = MediaStoreData.dummyItem.copy(id = 6L)),
        PhotoLibraryUIModel.Media(item = MediaStoreData.dummyItem.copy(id = 7L)),
        PhotoLibraryUIModel.Section(title = ""),
        PhotoLibraryUIModel.Media(item = MediaStoreData.dummyItem.copy(id = 9L)),
        PhotoLibraryUIModel.Media(item = MediaStoreData.dummyItem.copy(id = 10L)),
        PhotoLibraryUIModel.Media(item = MediaStoreData.dummyItem.copy(id = 11L)),
        PhotoLibraryUIModel.Media(item = MediaStoreData.dummyItem.copy(id = 12L)),
        PhotoLibraryUIModel.Media(item = MediaStoreData.dummyItem.copy(id = 13L)),
        PhotoLibraryUIModel.Media(item = MediaStoreData.dummyItem.copy(id = 14L)),
        PhotoLibraryUIModel.Media(item = MediaStoreData.dummyItem.copy(id = 15L)),
        PhotoLibraryUIModel.Media(item = MediaStoreData.dummyItem.copy(id = 16L)),
        PhotoLibraryUIModel.Section(title = ""),
        PhotoLibraryUIModel.Media(item = MediaStoreData.dummyItem.copy(id = 18L)),
        PhotoLibraryUIModel.Media(item = MediaStoreData.dummyItem.copy(id = 19L)),
        PhotoLibraryUIModel.Section(title = ""),
        PhotoLibraryUIModel.Media(item = MediaStoreData.dummyItem.copy(id = 21L)),
        PhotoLibraryUIModel.Media(item = MediaStoreData.dummyItem.copy(id = 22L)),
        PhotoLibraryUIModel.Media(item = MediaStoreData.dummyItem.copy(id = 23L)),
        PhotoLibraryUIModel.Media(item = MediaStoreData.dummyItem.copy(id = 24L)),
        PhotoLibraryUIModel.Media(item = MediaStoreData.dummyItem.copy(id = 25L)),
        PhotoLibraryUIModel.Media(item = MediaStoreData.dummyItem.copy(id = 26L)),
        PhotoLibraryUIModel.Media(item = MediaStoreData.dummyItem.copy(id = 27L)),
        PhotoLibraryUIModel.Media(item = MediaStoreData.dummyItem.copy(id = 28L)),
        PhotoLibraryUIModel.Media(item = MediaStoreData.dummyItem.copy(id = 29L)),
        PhotoLibraryUIModel.Media(item = MediaStoreData.dummyItem.copy(id = 30L)),
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
    const val HALF_ZOOM = 2.5f
}