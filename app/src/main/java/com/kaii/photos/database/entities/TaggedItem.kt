package com.kaii.photos.database.entities

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val color: Color
)

class ColorTypeConverter {
    @TypeConverter
    fun fromColor(color: Color) = color.toArgb()

    @TypeConverter
    fun toColor(argb: Int) = Color(argb)
}

@Entity(
    tableName = "tagged_items",
    foreignKeys = [
        ForeignKey(
            entity = Tag::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("tag"),
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MediaStoreData::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("mediaId"),
            onDelete = ForeignKey.CASCADE
        )
    ],
    primaryKeys = ["tag", "mediaId"],
    indices = [Index(value = ["mediaId"])]
)
data class TaggedItem(
    val tag: Int,
    val mediaId: Long
)