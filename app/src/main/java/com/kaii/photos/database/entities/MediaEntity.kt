package com.kaii.photos.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MediaEntity (
	@PrimaryKey val id: Long,
    @ColumnInfo(name = "date_taken") val dateTaken: Long,
    @ColumnInfo(name = "mime_type") val mimeType: String
)