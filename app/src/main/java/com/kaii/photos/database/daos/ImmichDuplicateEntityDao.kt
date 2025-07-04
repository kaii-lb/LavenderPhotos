package com.kaii.photos.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kaii.photos.database.entities.ImmichDuplicateEntity

@Dao
interface ImmichDuplicateEntityDao {
    @Query("SELECT * FROM immichduplicateentity")
    fun getAll(): List<ImmichDuplicateEntity>

    @Query("SELECT dupes FROM immichduplicateentity WHERE albumId = :albumId")
    fun getFor(albumId: String): List<String>

    @Query("SELECT EXISTS (SELECT * FROM immichduplicateentity WHERE albumId = :albumId)")
    fun isInDB(albumId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntity(vararg entity: ImmichDuplicateEntity)

    @Delete(entity = ImmichDuplicateEntity::class)
    suspend fun deleteEntity(entity: ImmichDuplicateEntity)

    @Query("DELETE FROM immichduplicateentity WHERE albumId = :albumId")
    suspend fun deleteEntityById(albumId: Long)
}
