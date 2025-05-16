package com.aks_labs.tulsi.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class TrashedItemEntity (
    @PrimaryKey val originalPath: String,
    @ColumnInfo(name = "trashed_path") val trashedPath: String,
    @ColumnInfo(name = "date_taken") val dateTaken: Long,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    @ColumnInfo(name = "display_name") val displayName: String
)


