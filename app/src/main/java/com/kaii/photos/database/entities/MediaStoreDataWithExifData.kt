package com.kaii.photos.database.entities

import androidx.room.Embedded
import androidx.room.Relation

data class MediaStoreDataWithExifData(
    @Embedded
    val media: MediaStoreData,

    @Relation(
        parentColumn = "id",
        entityColumn = "mediaId"
    )
    val exifData: ExifData
)