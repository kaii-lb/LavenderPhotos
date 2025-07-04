package com.kaii.photos.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Entity
data class ImmichDuplicateEntity(
    @PrimaryKey val albumId: Int,
    @ColumnInfo(name = "immich_id") val immichId: String,
    @ColumnInfo(name = "dupes") val dupes: SetHolder
)

@Serializable
data class SetHolder(
    val set: Set<String>
)

class ImmichDupeConverter {
    @TypeConverter
    fun from(set: SetHolder): String = Json.encodeToString(set)

    @TypeConverter
    fun to(set: String): SetHolder = Json.decodeFromString(set)
}
