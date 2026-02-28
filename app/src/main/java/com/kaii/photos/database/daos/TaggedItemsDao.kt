package com.kaii.photos.database.daos

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.database.entities.TaggedItem

@Dao
interface TaggedItemsDao {
    @Query(
        value = "SELECT DISTINCT media.* FROM media " +
                "JOIN tagged_items ON tagged_items.mediaId = media.id " +
                "WHERE tagged_items.tag IN (:tags) " +
                "GROUP BY media.id " +
                "HAVING COUNT(DISTINCT tagged_items.tag) = :tagCount " +
                "ORDER BY media.dateTaken DESC"
    )
    fun getAllInTagsDateTaken(tags: List<Int>, tagCount: Int): PagingSource<Int, MediaStoreData>

    @Query(
        value = "SELECT DISTINCT media.* FROM media " +
                "JOIN tagged_items ON tagged_items.mediaId = media.id " +
                "WHERE tagged_items.tag IN (:tags) " +
                "GROUP BY media.id " +
                "HAVING COUNT(DISTINCT tagged_items.tag) = :tagCount " +
                "ORDER BY media.dateModified DESC"
    )
    fun getAllInTagsDateModified(tags: List<Int>, tagCount: Int): PagingSource<Int, MediaStoreData>

    @Query(
        value = "SELECT DISTINCT media.* FROM media " +
                "JOIN tagged_items ON tagged_items.mediaId = media.id " +
                "WHERE tagged_items.tag IN (:tags) " +
                "GROUP BY media.id " +
                "HAVING COUNT(DISTINCT tagged_items.tag) = :tagCount AND media.displayName LIKE :query " +
                "ORDER BY media.dateTaken DESC"
    )
    fun searchInTagsDateTaken(query: String, tags: List<Int>, tagCount: Int): PagingSource<Int, MediaStoreData>

    @Query(
        value = "SELECT DISTINCT media.* FROM media " +
                "JOIN tagged_items ON tagged_items.mediaId = media.id " +
                "WHERE tagged_items.tag IN (:tags) " +
                "GROUP BY media.id " +
                "HAVING COUNT(DISTINCT tagged_items.tag) = :tagCount AND media.displayName LIKE :query " +
                "ORDER BY media.dateModified DESC"
    )
    fun searchInTagsDateModified(query: String, tags: List<Int>, tagCount: Int): PagingSource<Int, MediaStoreData>

    @Query(
        value = "SELECT DISTINCT media.* FROM media JOIN tagged_items ON tagged_items.mediaId = media.id " +
                "WHERE media.displayName LIKE :query " +
                "ORDER BY media.dateTaken DESC"
    )
    fun searchDateTaken(query: String): PagingSource<Int, MediaStoreData>

    @Query(
        value = "SELECT DISTINCT media.* FROM media JOIN tagged_items ON tagged_items.mediaId = media.id " +
                "WHERE media.displayName LIKE :query " +
                "ORDER BY media.dateModified DESC"
    )
    fun searchDateModified(query: String): PagingSource<Int, MediaStoreData>

    @Query(
        value = "SELECT DISTINCT media.* FROM media JOIN tagged_items ON tagged_items.mediaId = media.id " +
                "ORDER BY media.dateTaken DESC"
    )
    fun getAllDateTaken(): PagingSource<Int, MediaStoreData>

    @Query(
        value = "SELECT DISTINCT media.* FROM media JOIN tagged_items ON tagged_items.mediaId = media.id " +
                "ORDER BY media.dateModified DESC"
    )
    fun getAllDateModified(): PagingSource<Int, MediaStoreData>

    @Query(value = "SELECT * FROM media WHERE id = :id")
    suspend fun getItem(id: Long): MediaStoreData?

    @Upsert
    suspend fun upsert(vararg items: TaggedItem)

    @Delete
    suspend fun remove(vararg items: TaggedItem)
}