package com.kaii.photos.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.kaii.photos.database.entities.Tag
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query(value = "SELECT * FROM tags WHERE id = :id")
    suspend fun get(id: Int): Tag

    @Query(value = "SELECT * FROM tags")
    fun getAll(): Flow<List<Tag>>

    /** if given more than 1 id, this will find the intersection of the tags applied to each of those ids */
    @Query(value = "SELECT tags.* FROM tags " +
            "JOIN tagged_items ON tagged_items.tag = tags.id " +
            "WHERE tagged_items.mediaId IN (:ids)" +
            "GROUP BY tags.id " +
            "HAVING COUNT(DISTINCT mediaId) = :idCount"
    )
    fun getAppliedToMedia(ids: List<Long>, idCount: Int): Flow<List<Tag>>

    @Insert
    suspend fun insertAndGet(tag: Tag): Long

    @Upsert
    suspend fun upsert(vararg tags: Tag)

    @Delete
    suspend fun delete(vararg tags: Tag)
}