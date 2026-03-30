package com.kaii.photos.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kaii.photos.database.entities.SyncTask
import com.kaii.photos.database.entities.SyncTaskStatus

@Dao
interface SyncTaskDao {
    // sort ascendingly by time added so operations are applied in the correct order
    @Query(value = "SELECT * FROM sync_tasks WHERE status = 'Waiting' ORDER BY dateModified ASC")
    suspend fun getUnsyncedTasks(): List<SyncTask>

    @Query(value = "UPDATE sync_tasks SET status = :status WHERE id = :id")
    suspend fun updateTaskStatus(id: Int, status: SyncTaskStatus)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: SyncTask): Long
}