package com.kaii.photos.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "custom_items",
    foreignKeys = [ForeignKey(
        entity = MediaStoreData::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("id"),
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index(value = ["id"], orders = [Index.Order.DESC])
    ],
    primaryKeys = ["id", "album"]
)
data class CustomItem(
    val id: Long,
    val album: Int
)