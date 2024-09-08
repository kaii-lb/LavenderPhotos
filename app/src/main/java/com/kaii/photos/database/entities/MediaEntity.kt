package com.kaii.photos.database.entities

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MediaEntity (
	@PrimaryKey val id: Long,
    @ColumnInfo(name = "date_taken") val dateTaken: Long,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    @ColumnInfo(name = "display_name") val displayName: String
)
