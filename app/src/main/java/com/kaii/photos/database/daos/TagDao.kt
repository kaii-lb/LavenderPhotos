package com.kaii.photos.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.kaii.photos.database.entities.Tag

@Dao
interface TagDao {
    @Query(value = "SELECT * FROM tags WHERE id = :id")
    suspend fun get(id: Int): Tag

    @Query(value = "SELECT * FROM tags")
    suspend fun getAll(): List<Tag>

    @Upsert
    suspend fun upsert(vararg tags: Tag)

    @Delete
    suspend fun delete(vararg tags: Tag)
}