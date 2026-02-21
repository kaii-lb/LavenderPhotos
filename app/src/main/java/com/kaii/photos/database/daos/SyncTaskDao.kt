package com.kaii.photos.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kaii.photos.database.entities.SyncTask

@Dao
interface SyncTaskDao {
    @Query(value = "SELECT * FROM sync_tasks WHERE status = 'Waiting'")
    fun getUnsyncedTasks(): List<SyncTask>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(task: SyncTask)

    @Update
    fun update(task: SyncTask)
}