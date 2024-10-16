package com.kaii.photos.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kaii.photos.database.entities.TrashedItemEntity

@Dao
interface TrashedItemEntityDao {
    @Query("SELECT * FROM trasheditementity WHERE trashed_path = :path")
    fun getFromTrashedPath(path: String) : TrashedItemEntity

    @Query("SELECT * FROM trasheditementity WHERE originalPath = :path")
    fun getFromOriginalPath(path: String) : TrashedItemEntity

    @Query("SELECT date_taken FROM trasheditementity WHERE trashed_path = :path")
    fun getDateTakenFromTrashedPath(path: String) : Long

    @Query("SELECT mime_type FROM trasheditementity WHERE trashed_path = :path")
    fun getMimeTypeFromTrashedPath(path: String) : String

    @Query("SELECT originalPath FROM trasheditementity WHERE trashed_path = :trashedPath")
    fun getOriginalPathFrom(trashedPath: String) : String

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEntity(vararg entity: TrashedItemEntity)

    @Delete(entity = TrashedItemEntity::class)
    fun deleteEntity(entity: TrashedItemEntity)

    @Query("DELETE FROM trasheditementity WHERE originalpath = :path")
    fun deleteEntityByPath(path: String)
}
