package com.kaii.photos.database.daos

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.kaii.photos.database.entities.MediaStoreData

@Dao
interface MediaDao {
    @Query("SELECT * FROM media ORDER BY dateTaken DESC")
    fun getPagedMedia(): PagingSource<Int, MediaStoreData>

    @Query("SELECT * from media")
    fun getAll(): List<MediaStoreData>

    @Query("UPDATE media SET immichUrl = :immichUrl, immichThumbnail = :immichThumbnail WHERE hash = :hash")
    suspend fun linkToImmich(
        hash: String,
        immichUrl: String,
        immichThumbnail: String
    )

    // maybe try ignore and see performance difference?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg items: MediaStoreData)

    @Upsert
    fun upsertAll(items: List<MediaStoreData>)

    @Query("DELETE FROM media WHERE id = :id")
    fun deleteById(id: Long)

    @Delete
    fun deleteEntities(items: List<MediaStoreData>)
}
