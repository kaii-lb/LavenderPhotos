package com.kaii.photos.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "custom_media",
    foreignKeys = [ForeignKey(
        entity = MediaStoreData::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("mediaId"),
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE
    )],
    indices = [
        Index(value = ["mediaId"], orders = [Index.Order.DESC])
    ]
)
data class CustomItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // default value of 0 as to let room generate the key
    val mediaId: Long,
    val album: Int
)