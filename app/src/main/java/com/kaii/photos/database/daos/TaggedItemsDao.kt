package com.kaii.photos.database.daos

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.MapColumn
import androidx.room.Query
import androidx.room.Upsert
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.database.entities.TaggedItem
import com.kaii.photos.helpers.grid_management.SelectionManager

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

    @Query(
        value = "SELECT DISTINCT media.id as keyId, media.id, media.uri, media.immichUrl, media.absolutePath, " +
                "CASE WHEN type = 'Image' THEN 1 ELSE 0 END as isImage " +
                "FROM media " +
                "JOIN tagged_items ON tagged_items.mediaId = media.id " +
                "WHERE CASE WHEN :dateModified = 1 THEN dateModified ELSE dateTaken END " +
                "BETWEEN :timestamp AND :timestamp+86400 LIMIT 2000"
    )
    fun mediaInDateRangeWithAnyTag(timestamp: Long, dateModified: Boolean): Map<
            @MapColumn(columnName = "keyId") Long,
            SelectionManager.SelectedItem>

    @Query(
        value = "SELECT DISTINCT media.id as keyId, media.id, media.uri, media.immichUrl, media.absolutePath, " +
                "CASE WHEN media.type = 'Image' THEN 1 ELSE 0 END as isImage " +
                "FROM media " +
                "JOIN tagged_items ON tagged_items.mediaId = media.id " +
                "WHERE tagged_items.tag IN (:tagIds) " +
                "GROUP BY media.id " +
                "HAVING COUNT(DISTINCT tagged_items.tag) = :tagCount " +
                "AND CASE WHEN :dateModified = 1 THEN dateModified ELSE dateTaken END " +
                "BETWEEN :timestamp AND :timestamp+86400 LIMIT 2000 "
    )
    fun mediaInDateRangeWithTags(
        timestamp: Long,
        dateModified: Boolean,
        tagIds: List<Int>,
        tagCount: Int
    ): Map<
            @MapColumn(columnName = "keyId") Long,
            SelectionManager.SelectedItem>

    fun mediaInDateRangeWithLotsOfTags(
        timestamp: Long,
        dateModified: Boolean,
        tagIds: List<Int>
    ): Map<Long, SelectionManager.SelectedItem> =
        tagIds.chunked(500).map { chunk ->
            mediaInDateRangeWithTags(
                timestamp = timestamp,
                dateModified = dateModified,
                tagIds = chunk,
                tagCount = chunk.size
            )
        }.fold(emptyMap()) { acc, map ->
            acc + map
        }

    @Upsert
    suspend fun upsert(vararg items: TaggedItem)

    @Delete
    suspend fun remove(vararg items: TaggedItem)
}