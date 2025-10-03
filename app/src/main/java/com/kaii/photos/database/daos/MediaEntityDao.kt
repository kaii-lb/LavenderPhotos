package com.kaii.photos.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kaii.photos.database.entities.MediaEntity

@Dao
interface MediaEntityDao {
    @Query("SELECT * FROM mediaentity WHERE id LIKE :id")
    fun getFromId(id: Long) : MediaEntity?

    @Query("SELECT date_taken FROM mediaentity WHERE id = :id")
    fun getDateTaken(id: Long) : Long

    @Query("SELECT mime_type FROM mediaentity WHERE id = :id")
    fun getMimeType(id: Long) : String

    @Query("SELECT * FROM mediaentity WHERE mime_type = :mimetype")
    fun getFromMimeType(mimetype: String) : List<MediaEntity>

    @Query("SELECT * FROM mediaentity WHERE date_taken = :dateTaken")
    fun getFromDateTaken(dateTaken: Long) : List<MediaEntity>

    @Query("SELECT * FROM mediaentity WHERE display_name = :displayName")
    fun getFromDisplayName(displayName: String) : List<MediaEntity>

    @Query("SELECT * from mediaentity")
    fun getAll(): List<MediaEntity>

	// maybe try ignore and see performance difference?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEntity(vararg entity: MediaEntity)

    @Delete(entity = MediaEntity::class)
    fun deleteEntity(entity: MediaEntity)

    @Query("DELETE FROM mediaentity WHERE id = :id")
    fun deleteEntityById(id: Long)
}
