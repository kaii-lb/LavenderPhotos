package com.kaii.photos.helpers.editing

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
sealed interface ImageModification {
    @Immutable
    @Serializable
    data class Adjustment(
        @SerialName("media_adjustment_type") val type: MediaAdjustments,
        val value: Float
    ) : ImageModification

    @Immutable
    @Serializable
    data class Crop(
        override val top: Float,
        override val left: Float,
        override val width: Float,
        override val height: Float
    ) : ImageModification, SharedModification.Crop

    @Immutable
    @Serializable
    data class Filter(
        @SerialName("media_color_filter_type") override val type: MediaColorFilters
    ) : ImageModification, SharedModification.Filter

    @Immutable
    @Serializable
    data class DrawingText(
        @SerialName("drawing_item_type") override val type: DrawingItems = DrawingItems.Text,
        override val text: DrawableText
    ) : ImageModification, SharedModification.DrawingText

    @Immutable
    @Serializable
    data class DrawingImage(
        @SerialName("drawing_item_type") override val type: DrawingItems = DrawingItems.Image,
        override val image: DrawableImage
    ) : ImageModification, SharedModification.DrawingImage
}