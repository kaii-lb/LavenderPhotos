package com.kaii.photos.helpers

enum class MediaItemSortMode {
    DateTaken,
    MonthTaken,
    LastModified,
    Disabled;

    companion object {
        val MediaItemSortMode.presentableName: String
            get() = name.split(Regex("(?=\\p{Lu})")).joinToString(" ")
    }
}
