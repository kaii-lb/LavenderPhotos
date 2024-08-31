package com.kaii.photos.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.kaii.photos.database.entities.MediaEntity

@Dao
interface MediaEntityDao {
    @Query("SELECT * FROM mediaentity WHERE uri LIKE :uri")
    fun getFromUri(uri: String) : MediaEntity

    @Query("SELECT date_taken FROM mediaentity WHERE uri LIKE :uri")
    fun getDateTaken(uri: String) : Long

    @Insert
    fun insertEntity(vararg entity: MediaEntity)

    @Delete
    fun deleteEntity(entity: MediaEntity)
}