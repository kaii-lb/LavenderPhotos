package com.kaii.photos.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kaii.photos.database.entities.FavouritedItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouritedItemEntityDao {
    @Query("SELECT * FROM favouriteditementity")
    fun getAll() : Flow<List<FavouritedItemEntity>>

    @Query("SELECT EXISTS (SELECT * FROM favouriteditementity WHERE id = :id)")
    fun isInDB(id: Long) : Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntity(vararg entity: FavouritedItemEntity)

    @Delete(entity = FavouritedItemEntity::class)
    suspend fun deleteEntity(entity: FavouritedItemEntity)

    @Query("DELETE FROM favouriteditementity WHERE id = :id")
    suspend fun deleteEntityById(id: Long)
}
