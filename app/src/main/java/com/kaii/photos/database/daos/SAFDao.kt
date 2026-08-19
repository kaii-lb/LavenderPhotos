package com.kaii.photos.database.daos

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.MapColumn
import androidx.room.Query
import androidx.room.Upsert
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.AlbumSortMode
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.state.AlbumGridState
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.mediastore.signature

@Dao
interface SAFDao {
    @Query(value = "SELECT * FROM MEDIA WHERE id = :id")
    fun getById(id: Long): MediaStoreData?

    @Query(value = "SELECT * FROM media WHERE parentPath = :treeUri ORDER BY dateTaken DESC")
    fun getPagedMediaDateTaken(treeUri: String): PagingSource<Int, MediaStoreData>

    @Query(value = "SELECT * FROM media WHERE parentPath = :treeUri ORDER BY dateModified DESC")
    fun getPagedMediaDateModified(treeUri: String): PagingSource<Int, MediaStoreData>

    @Query(value = "SELECT COUNT(id) FROM media WHERE parentPath = :treeUri")
    fun countMediaInPaths(treeUri: String): Int

    @Query(value = "SELECT SUM(size) FROM media WHERE parentPath = :treeUri")
    fun mediaSize(treeUri: String): Long

    @Query(value = "SELECT id FROM media WHERE parentPath = :treeUri")
    fun getAllIdsIn(treeUri: String): List<Long>

    @Query(
        value = "SELECT id as keyId, id, uri, immichUrl, absolutePath, parentPath, " +
                "CASE WHEN type = 'Image' THEN 1 ELSE 0 END as isImage " +
                "from media WHERE " +
                "CASE WHEN :dateModified = 1 THEN dateModified ELSE dateTaken END " +
                "BETWEEN :timestamp AND :timestamp+86400 AND parentPath = :treeUri LIMIT 2000"
    )
    fun mediaInDateRange(timestamp: Long, treeUri: String, dateModified: Boolean): Map<
            @MapColumn(columnName = "keyId") Long,
            SelectionManager.SelectedItem>

    @Query(
        value = """
        SELECT parentPath AS albumPath, media.* FROM media 
        WHERE albumPath IN (:treeUris) 
        GROUP BY albumPath 
        HAVING dateModified = MAX(dateModified)
        """
    )
    suspend fun getBatchFolderThumbnailsDateModified(treeUris: List<String>): Map<@MapColumn("albumPath") String, MediaStoreData>

    @Query(
        value = """
        SELECT parentPath AS albumPath, media.* FROM media 
        WHERE albumPath IN (:treeUris)
        GROUP BY albumPath
        HAVING dateTaken = MAX(dateTaken)
        """
    )
    suspend fun getBatchFolderThumbnailsDateTaken(treeUris: List<String>): Map<@MapColumn("albumPath") String, MediaStoreData>

    suspend fun getFolderThumbnails(
        folders: List<AlbumType.SAFFolder>,
        sortMode: MediaItemSortMode,
        albumSortMode: AlbumSortMode
    ): Map<String, AlbumGridState.Info.Thumbnail> {
        if (folders.isEmpty()) return emptyMap()

        val pathThumbnails = if (albumSortMode == AlbumSortMode.LastModified) {
            getBatchFolderThumbnailsDateModified(treeUris = folders.map { it.base64TreeUri })
        } else {
            getBatchFolderThumbnailsDateTaken(treeUris = folders.map { it.base64TreeUri })
        }

        val result = mutableMapOf<String, AlbumGridState.Info.Thumbnail>()

        folders.forEach { folder ->
            val thumbnail = pathThumbnails[folder.base64TreeUri] ?: MediaStoreData.dummyItem

            result[folder.id] = AlbumGridState.Info.Thumbnail(
                uri = thumbnail.uri,
                date = if (sortMode.isDateModified) thumbnail.dateModified else thumbnail.dateTaken,
                signature = thumbnail.signature(),
                albumId = folder.id,
                isGif = thumbnail.displayName.endsWith(".gif")
            )
        }

        val emptyMedia = MediaStoreData.dummyItem
        val missing = folders.filter {
            it.id !in result
        }.associate { album ->
            album.id to AlbumGridState.Info.Thumbnail(
                uri = emptyMedia.uri,
                date = emptyMedia.dateTaken,
                signature = emptyMedia.signature(),
                albumId = album.id,
                isGif = false
            )
        }

        return result + missing
    }

    @Upsert
    suspend fun upsertAll(items: List<MediaStoreData>)

    @Query(value = "DELETE FROM media WHERE id IN (:ids)")
    suspend fun deleteAll(ids: List<Long>)
}