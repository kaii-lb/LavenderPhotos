package com.kaii.photos.helpers.editing

interface ImageModification {
    data class Adjustment(
        val type: MediaAdjustments,
        val value: Float
    ) : ImageModification

    data class Crop(
        override val top: Float,
        override val left: Float,
        override val width: Float,
        override val height: Float
    ) : ImageModification, SharedModification.Crop

    data class Filter(
        override val type: MediaColorFilters
    ) : ImageModification, SharedModification.Filter

    data class DrawingText(
        override val type: DrawingItems = DrawingItems.Text,
        override val text: DrawableText
    ) : ImageModification, SharedModification.DrawingText

    data class DrawingImage(
        override val type: DrawingItems = DrawingItems.Image,
        override val image: DrawableImage
    ) : ImageModification, SharedModification.DrawingImage
}