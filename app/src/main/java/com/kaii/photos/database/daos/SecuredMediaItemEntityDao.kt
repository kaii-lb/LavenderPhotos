package com.kaii.photos.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy
import com.kaii.photos.database.entities.MediaEntity
import com.kaii.photos.database.entities.SecuredItemEntity

@Dao
interface SecuredMediaItemEntityDao {
    @Query("SELECT originalPath FROM secureditementity WHERE secured_path = :securedPath")
    fun getOriginalPathFromSecuredPath(securedPath: String) : String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEntity(vararg entity: SecuredItemEntity)

    @Query("DELETE FROM secureditementity WHERE secured_path = :securedPath")
    fun deleteEntityBySecuredPath(securedPath: String)
}
