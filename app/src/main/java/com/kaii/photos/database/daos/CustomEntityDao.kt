package com.kaii.photos.database.daos

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kaii.photos.database.entities.CustomItem
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.helpers.grid_management.SelectionManager

@Dao
interface CustomEntityDao {
    @Query(value = "SELECT media.* FROM media JOIN custom_items ON custom_items.id = media.id WHERE album = :album ORDER BY dateTaken DESC")
    fun getPagedMediaDateTaken(album: Int): PagingSource<Int, MediaStoreData>

    @Query(value = "SELECT media.* FROM media JOIN custom_items ON custom_items.id = media.id WHERE album = :album ORDER BY dateModified DESC")
    fun getPagedMediaDateModified(album: Int): PagingSource<Int, MediaStoreData>

    @Query(value = "SELECT COUNT(id) FROM custom_items WHERE album = :album")
    fun countMediaInAlbum(album: Int): Int

    @Query(value = "SELECT SUM(media.size) FROM media JOIN custom_items ON custom_items.id = media.id WHERE album = :album")
    fun mediaSize(album: Int): Long

    @Query(value = "SELECT media.* FROM media JOIN custom_items ON custom_items.id = media.id WHERE album = :album ORDER BY dateTaken DESC LIMIT 1")
    fun getThumbnailForAlbumDateTaken(album: Int): MediaStoreData?

    @Query(value = "SELECT media.* FROM media JOIN custom_items ON custom_items.id = media.id WHERE album = :album ORDER BY dateModified DESC LIMIT 1")
    fun getThumbnailForAlbumDateModified(album: Int): MediaStoreData?

    @Query(
        value = "SELECT media.id," +
                "CASE WHEN media.type = 'Image' THEN 1 ELSE 0 END as isImage, " +
                "media.absolutePath as parentPath " +
                "FROM media " +
                "JOIN custom_items ON media.id = custom_items.id " +
                "WHERE " +
                "CASE WHEN :dateModified = 1 THEN media.dateModified ELSE media.dateTaken END " +
                "BETWEEN :timestamp AND :timestamp+86400 AND custom_items.album = :album LIMIT 2000"
    )
    fun mediaInDateRange(timestamp: Long, album: Int, dateModified: Boolean): List<SelectionManager.SelectedItem>

    @Query(value = "SELECT id FROM custom_items WHERE album = :album")
    suspend fun getAllIdsIn(album: Int): List<Long>

    @Upsert
    suspend fun upsertAll(items: List<CustomItem>)

    @Query(value = "DELETE FROM custom_items WHERE id IN (:ids) AND album = :album")
    suspend fun deleteAll(ids: Set<Long>, album: Int)

    @Query(value = "DELETE FROM custom_items WHERE album = :album")
    suspend fun deleteAlbum(album: Int)
}
