package com.kaii.photos.helpers

enum class MediaItemSortMode {
    DateTaken,
    MonthTaken,
    DateModified,
    Disabled,
    DisabledLastModified;

    companion object {
        val MediaItemSortMode.presentableName: String
            get() = name.split(Regex("(?=\\p{Lu})")).joinToString(" ")
    }

    val isDisabled: Boolean
        get() = this == Disabled || this == DisabledLastModified

    val isDateModified: Boolean
        get() = this == DisabledLastModified || this == DateModified
}
