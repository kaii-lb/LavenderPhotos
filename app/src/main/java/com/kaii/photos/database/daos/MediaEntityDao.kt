package com.kaii.photos.database.daos

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.kaii.photos.database.entities.MediaStoreData
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query(value = "SELECT * FROM media WHERE parentPath IN (:paths) ORDER BY dateTaken DESC")
    fun getPagedMediaDateTaken(paths: Set<String>): PagingSource<Int, MediaStoreData>

    @Query(value = "SELECT * FROM media WHERE parentPath IN (:paths) ORDER BY dateModified DESC")
    fun getPagedMediaDateModified(paths: Set<String>): PagingSource<Int, MediaStoreData>

    @Query(value = "SELECT * from media ORDER BY dateTaken DESC")
    fun getAllMedia(): Flow<List<MediaStoreData>>

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

    @Query(value = "UPDATE media SET immichUrl = :immichUrl, immichThumbnail = :immichThumbnail WHERE hash = :hash")
    suspend fun linkToImmich(
        hash: String,
        immichUrl: String,
        immichThumbnail: String
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg items: MediaStoreData)

    @Upsert
    fun upsertAll(items: List<MediaStoreData>)

    @Query(value = "DELETE FROM media WHERE id = :id")
    fun deleteById(id: Long)

    @Delete
    fun deleteEntities(items: List<MediaStoreData>)
}
