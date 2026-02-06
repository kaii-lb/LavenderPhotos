package com.kaii.photos.helpers

import com.kaii.photos.database.entities.MediaStoreData

enum class MediaItemSortMode {
    DateTaken,
    MonthTaken,
    LastModified,
    Disabled,
    DisabledLastModified;

    companion object {
        val MediaItemSortMode.presentableName: String
            get() = name.split(Regex("(?=\\p{Lu})")).joinToString(" ")
    }

    val isDisabled: Boolean
        get() = this == Disabled || this == DisabledLastModified

    val isLastModified: Boolean
        get() = this == DisabledLastModified || this == LastModified

    fun toSortProp() =
        if (isLastModified) MediaStoreData::dateModified.name else MediaStoreData::dateTaken.name
}
