package com.kaii.photos.database.daos

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kaii.photos.database.entities.CustomItemEntity
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.helpers.grid_management.SelectionManager

@Dao
interface CustomEntityDao {
    @Query(value = "SELECT media.* FROM media JOIN custom_media ON custom_media.mediaId = media.id WHERE album = :album ORDER BY dateTaken DESC")
    fun getPagedMediaDateTaken(album: Int): PagingSource<Int, MediaStoreData>

    @Query(value = "SELECT media.* FROM media JOIN custom_media ON custom_media.mediaId = media.id WHERE album = :album ORDER BY dateModified DESC")
    fun getPagedMediaDateModified(album: Int): PagingSource<Int, MediaStoreData>

    @Query(value = "SELECT COUNT(id) FROM custom_media WHERE album = :album")
    fun countMediaInAlbum(album: Int): Int

    @Query(value = "SELECT media.* FROM media JOIN custom_media ON custom_media.mediaId = media.id WHERE album = :album ORDER BY dateTaken DESC LIMIT 1")
    fun getThumbnailForAlbumDateTaken(album: Int): MediaStoreData?

    @Query(value = "SELECT media.* FROM media JOIN custom_media ON custom_media.mediaId = media.id WHERE album = :album ORDER BY dateModified DESC LIMIT 1")
    fun getThumbnailForAlbumDateModified(album: Int): MediaStoreData?

    @Query(
        value = "SELECT media.id," +
                "CASE WHEN media.type = 'Image' THEN 1 ELSE 0 END as isImage, " +
                "media.absolutePath as parentPath " +
                "FROM media " +
                "JOIN custom_media ON media.id = custom_media.mediaId " +
                "WHERE " +
                "CASE WHEN :dateModified = 1 THEN media.dateModified ELSE media.dateTaken END " +
                "BETWEEN :timestamp AND :timestamp+86400 AND custom_media.album = :album LIMIT 2000"
    )
    fun mediaInDateRange(timestamp: Long, album: Int, dateModified: Boolean): List<SelectionManager.SelectedItem>

    @Upsert
    fun upsertAll(items: List<CustomItemEntity>)

    @Query(value = "DELETE FROM custom_media WHERE mediaId IN (:ids) AND album = :album")
    fun deleteAll(ids: Set<Long>, album: Int)

    @Query(value = "DELETE FROM custom_media WHERE album = :album")
    fun deleteAlbum(album: Int)
}
