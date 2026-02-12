package com.kaii.photos.database.daos

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.helpers.grid_management.SelectionManager
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao : BaseDao {
    @Query(value = "SELECT * FROM media WHERE parentPath IN (:paths) ORDER BY dateTaken DESC")
    override fun getPagedMediaDateTaken(paths: Set<String>): PagingSource<Int, MediaStoreData>

    @Query(value = "SELECT * FROM media WHERE parentPath IN (:paths) ORDER BY dateModified DESC")
    override fun getPagedMediaDateModified(paths: Set<String>): PagingSource<Int, MediaStoreData>

    @Query(value = "SELECT * from media ORDER BY dateTaken DESC")
    fun getAllMediaDateTaken(): Flow<List<MediaStoreData>>

    @Query(value = "SELECT * from media ORDER BY dateModified DESC")
    fun getAllMediaDateModified(): Flow<List<MediaStoreData>>

    @Query(value = "SELECT id from media")
    fun getAllMediaIds(): List<Long>

    @Query(value = "SELECT * from media WHERE favourited = 1 ORDER BY dateTaken DESC")
    fun getPagedFavouritesDateTaken(): PagingSource<Int, MediaStoreData>

    @Query(value = "SELECT * from media WHERE favourited = 1 ORDER BY dateModified DESC")
    fun getPagedFavouritesDateModified(): PagingSource<Int, MediaStoreData>

    @Query(value = "SELECT * from media WHERE favourited = 1 ORDER BY dateTaken DESC")
    fun getAllFavourites(): List<MediaStoreData>

    @Query(value = "SELECT * from media WHERE parentPath IN (:paths) ORDER BY dateTaken DESC LIMIT 1")
    fun getThumbnailForAlbumDateTaken(paths: Set<String>): MediaStoreData?

    @Query(value = "SELECT * from media WHERE parentPath IN (:paths) ORDER BY dateModified DESC LIMIT 1")
    fun getThumbnailForAlbumDateModified(paths: Set<String>): MediaStoreData?

    @Query(
        value = "SELECT id," +
                "CASE WHEN type = 'Image' THEN 1 ELSE 0 END as isImage " +
                "from media WHERE " +
                "CASE WHEN :dateModified = 1 THEN dateModified ELSE dateTaken END " +
                "BETWEEN :timestamp AND :timestamp+86400 AND parentPath in (:paths)"
    )
    override fun mediaInDateRange(timestamp: Long, paths: Set<String>, dateModified: Boolean): List<SelectionManager.SelectedItem>

    @Query(
        value = "SELECT id," +
                "CASE WHEN type = 'Image' THEN 1 ELSE 0 END as isImage " +
                "from media WHERE " +
                "CASE WHEN :dateModified = 1 THEN dateModified ELSE dateTaken END " +
                "BETWEEN :timestamp AND :timestamp+86400"
    )
    override fun mediaInDateRange(timestamp: Long, dateModified: Boolean): List<SelectionManager.SelectedItem>


    @Query(value = "UPDATE media SET immichUrl = :immichUrl, immichThumbnail = :immichThumbnail WHERE hash = :hash")
    suspend fun linkToImmich(
        hash: String,
        immichUrl: String,
        immichThumbnail: String
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg items: MediaStoreData)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(items: Set<MediaStoreData>)

    @Upsert
    fun upsertAll(items: List<MediaStoreData>)

    @Query(value = "DELETE FROM media WHERE id = :id")
    fun deleteById(id: Long)

    @Delete
    fun deleteEntities(items: List<MediaStoreData>)

    @Query(value = "DELETE FROM media WHERE id IN (:ids)")
    fun deleteAll(ids: Set<Long>)
}
