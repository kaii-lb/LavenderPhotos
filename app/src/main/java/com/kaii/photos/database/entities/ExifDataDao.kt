package com.kaii.photos.database.entities

import androidx.room.Dao
import androidx.room.Upsert

@Dao
interface ExifDataDao {
    @Upsert
    suspend fun upsertAll(items: List<ExifData>)
}