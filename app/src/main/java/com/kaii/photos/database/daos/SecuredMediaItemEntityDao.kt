package com.kaii.photos.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kaii.photos.database.entities.SecuredItemEntity

@Dao
interface SecuredMediaItemEntityDao {
    @Query(value = "SELECT originalPath FROM secureditementity WHERE secured_path = :securedPath")
    fun getOriginalPathFromSecuredPath(securedPath: String) : String?

	@Query(value = "SELECT iv FROM secureditementity WHERE secured_path = :securedPath")
	fun getIvFromSecuredPath(securedPath: String): ByteArray?

    @Query(value = "SELECT secured_path FROM secureditementity WHERE originalPath = :originalPath")
    fun getSecuredPathFromOriginalPath(originalPath: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEntity(vararg entity: SecuredItemEntity)

    @Query(value = "DELETE FROM secureditementity WHERE secured_path = :securedPath")
    fun deleteEntityBySecuredPath(securedPath: String)
}
