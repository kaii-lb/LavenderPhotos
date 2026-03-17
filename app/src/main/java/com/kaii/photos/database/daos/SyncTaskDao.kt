package com.kaii.photos.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kaii.photos.database.entities.SyncTask
import com.kaii.photos.database.entities.SyncTaskStatus

@Dao
interface SyncTaskDao {
    @Query(value = "SELECT * FROM sync_tasks WHERE status = 'Waiting'")
    suspend fun getUnsyncedTasks(): List<SyncTask>

    @Query(value = "UPDATE sync_tasks SET status = :status WHERE id = :id")
    suspend fun updateTaskStatus(id: Int, status: SyncTaskStatus)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: SyncTask): Long
}