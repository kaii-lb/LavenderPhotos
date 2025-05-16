package com.aks_labs.tulsi.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aks_labs.tulsi.mediastore.MediaType

@Entity
data class FavouritedItemEntity (
	@PrimaryKey val id: Long,
    @ColumnInfo(name = "date_taken") val dateTaken: Long,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "absolute_path") val absolutePath: String,
    @ColumnInfo(name = "type") val type: MediaType,
    @ColumnInfo(name = "date_modified") val dateModified: Long,
	@ColumnInfo(name = "uri") val uri: String
)


