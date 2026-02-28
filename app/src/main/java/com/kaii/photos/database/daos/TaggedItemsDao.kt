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
    @Query(value = "SELECT media.* FROM media JOIN tagged_items ON tagged_items.mediaId = media.id " +
            "WHERE tagged_items.tag IN (:tags) " +
            "ORDER BY media.dateTaken DESC"
    )
    fun getAllInTagDateTaken(tags: List<Int>): PagingSource<Int, MediaStoreData>

    @Query(value = "SELECT media.* FROM media JOIN tagged_items ON tagged_items.mediaId = media.id " +
            "WHERE tagged_items.tag IN (:tags) " +
            "ORDER BY media.dateModified DESC"
    )
    fun getAllInTagDateModified(tags: List<Int>): PagingSource<Int, MediaStoreData>

    @Query(value = "SELECT media.* FROM media JOIN tagged_items ON tagged_items.mediaId = media.id " +
            "WHERE tagged_items.tag IN (:tags) AND media.displayName LIKE :query " +
            "ORDER BY media.dateTaken DESC"
    )
    fun searchInTagDateTaken(query: String, tags: List<Int>): PagingSource<Int, MediaStoreData>

    @Query(value = "SELECT media.* FROM media JOIN tagged_items ON tagged_items.mediaId = media.id " +
            "WHERE tagged_items.tag IN (:tags) AND media.displayName LIKE :query " +
            "ORDER BY media.dateModified DESC"
    )
    fun searchInTagDateModified(query: String, tags: List<Int>): PagingSource<Int, MediaStoreData>

    @Query(value = "SELECT * FROM media WHERE id = :id")
    suspend fun getItem(id: Long): MediaStoreData?

    @Upsert
    suspend fun upsert(vararg items: TaggedItem)

    @Delete
    suspend fun remove(vararg items: TaggedItem)
}