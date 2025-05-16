package com.aks_labs.tulsi.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aks_labs.tulsi.database.entities.SecuredItemEntity

@Dao
interface SecuredMediaItemEntityDao {
    @Query("SELECT originalPath FROM secureditementity WHERE secured_path = :securedPath")
    fun getOriginalPathFromSecuredPath(securedPath: String) : String?

	@Query("SELECT iv FROM secureditementity WHERE secured_path = :securedPath")
	fun getIvFromSecuredPath(securedPath: String): ByteArray?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEntity(vararg entity: SecuredItemEntity)

    @Query("DELETE FROM secureditementity WHERE secured_path = :securedPath")
    fun deleteEntityBySecuredPath(securedPath: String)
}


