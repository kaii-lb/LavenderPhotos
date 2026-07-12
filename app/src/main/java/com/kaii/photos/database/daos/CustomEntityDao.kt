package com.kaii.photos.database.daos

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.MapColumn
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.kaii.photos.database.entities.CustomItem
import com.kaii.photos.database.entities.ExifData
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.AlbumSortMode
import com.kaii.photos.datastore.state.AlbumGridState
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.mediastore.signature

@Dao
interface CustomEntityDao {
    @Query(value = "SELECT media.* FROM media JOIN custom_items ON custom_items.id = media.id WHERE album = :album ORDER BY dateTaken DESC")
    fun getPagedMediaDateTaken(album: String): PagingSource<Int, MediaStoreData>

    @Query(value = "SELECT media.* FROM media JOIN custom_items ON custom_items.id = media.id WHERE album = :album ORDER BY dateModified DESC")
    fun getPagedMediaDateModified(album: String): PagingSource<Int, MediaStoreData>

    @Query(value = "SELECT COUNT(id) FROM custom_items WHERE album = :album")
    fun countMediaInAlbum(album: String): Int

    @Query(value = "SELECT SUM(media.size) FROM media JOIN custom_items ON custom_items.id = media.id WHERE album = :album")
    fun mediaSize(album: String): Long

    @Query(
        value = "SELECT media.id as keyId, media.id, media.uri, media.immichUrl, media.parentPath, " +
                "CASE WHEN media.type = 'Image' THEN 1 ELSE 0 END as isImage " +
                "FROM media " +
                "JOIN custom_items ON media.id = custom_items.id " +
                "WHERE " +
                "CASE WHEN :dateModified = 1 THEN media.dateModified ELSE media.dateTaken END " +
                "BETWEEN :timestamp AND :timestamp+86400 AND custom_items.album = :album LIMIT 2000"
    )
    fun mediaInDateRange(timestamp: Long, album: String, dateModified: Boolean): Map<
            @MapColumn(columnName = "keyId") Long,
            SelectionManager.SelectedItem>

    @Query(value = "SELECT id FROM custom_items WHERE album = :album")
    suspend fun getAllIdsIn(album: String): List<Long>

    @Query(value = "SELECT media.* FROM media JOIN custom_items ON custom_items.id = media.id WHERE album = :album")
    suspend fun getMediaInAlbum(album: String): List<MediaStoreData>

    @Transaction
    @Query(value = "SELECT * FROM media_exif_data WHERE mediaId = :id")
    suspend fun getExifData(id: Long): ExifData?

    @Query(value = "SELECT * FROM custom_items ORDER BY id ASC LIMIT :chunkSize OFFSET :offset")
    suspend fun getChunked(chunkSize: Int, offset: Int): List<CustomItem>

    @Query(
        value = """
        SELECT custom_items.album AS albumId, media.* FROM custom_items 
        JOIN media ON media.id = custom_items.id 
        WHERE albumId IN (:albumIds) 
        GROUP BY albumId 
        HAVING dateTaken = MAX(dateTaken)
        """
    )
    suspend fun getThumbnailsDateTaken(albumIds: List<String>): Map<@MapColumn("albumId") String, MediaStoreData>

    @Query(
        value = """
        SELECT custom_items.album AS albumId, media.* FROM custom_items 
        JOIN media ON media.id = custom_items.id 
        WHERE albumId IN (:albumIds) 
        GROUP BY albumId 
        HAVING dateModified = MAX(dateModified)
        """
    )
    suspend fun getThumbnailsDateModified(albumIds: List<String>): Map<@MapColumn("albumId") String, MediaStoreData>

    suspend fun getThumbnails(
        albumIds: List<String>,
        sortMode: MediaItemSortMode,
        albumSortMode: AlbumSortMode
    ): Map<String, AlbumGridState.Info.Thumbnail> {
        val customThumbnails =
            if (albumSortMode == AlbumSortMode.LastModified) {
                getThumbnailsDateModified(albumIds = albumIds)
            } else {
                getThumbnailsDateTaken(albumIds = albumIds)
            }

        val emptyMedia = MediaStoreData.dummyItem
        val missing = albumIds.filter { id ->
            id !in customThumbnails
        }.associateWith { id ->
            AlbumGridState.Info.Thumbnail(
                uri = emptyMedia.uri,
                date = emptyMedia.dateTaken,
                signature = emptyMedia.signature(),
                albumId = id,
                isGif = false
            )
        }

        return missing + customThumbnails.mapValues { (id, media) ->
            AlbumGridState.Info.Thumbnail(
                uri = media.uri,
                date = if (sortMode.isDateModified) media.dateModified else media.dateTaken,
                signature = media.signature(),
                albumId = id,
                isGif = media.displayName.endsWith(".gif")
            )
        }
    }

    @Upsert
    suspend fun upsertAll(items: List<CustomItem>)

    @Query(value = "DELETE FROM custom_items WHERE id IN (:ids) AND album = :album")
    suspend fun deleteAll(ids: Set<Long>, album: String)

    @Query(value = "DELETE FROM custom_items WHERE album = :album")
    suspend fun deleteAlbum(album: String)
}
