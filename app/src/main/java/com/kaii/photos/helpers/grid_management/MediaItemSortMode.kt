package com.kaii.photos.helpers.grid_management

enum class MediaItemSortMode {
    DateTaken,
    MonthTaken,
    DateModified,
    Disabled,
    DisabledLastModified;

    val isDisabled: Boolean
        get() = this == Disabled || this == DisabledLastModified

    val isDateModified: Boolean
        get() = this == DisabledLastModified || this == DateModified
}
