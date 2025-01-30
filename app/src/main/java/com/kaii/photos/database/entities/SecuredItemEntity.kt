package com.kaii.photos.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class SecuredItemEntity (
	@PrimaryKey val originalPath: String,
    @ColumnInfo(name = "secured_path") val securedPath: String,
    @ColumnInfo(name = "iv") val iv: ByteArray
)
