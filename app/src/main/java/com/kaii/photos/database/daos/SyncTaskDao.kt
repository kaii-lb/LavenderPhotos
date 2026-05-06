package com.kaii.photos.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kaii.photos.database.entities.SyncTask
import com.kaii.photos.database.entities.SyncTaskStatus
import com.kaii.photos.database.entities.SyncTaskItem
import com.kaii.photos.helpers.grid_management.SelectionManager

@Dao
interface SyncTaskDao {
    // sort ascendingly by time added so operations are applied in the correct order
    @Query(value = "SELECT * FROM sync_tasks WHERE status = 'Waiting' ORDER BY dateModified ASC")
    suspend fun getUnsyncedTasks(): List<SyncTask>

    @Query(value = "UPDATE sync_tasks SET status = :status WHERE id = :id")
    suspend fun updateTaskStatus(id: Int, status: SyncTaskStatus)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: SyncTask): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(items: List<SyncTaskItem>)

    @Delete
    suspend fun delete(item: SyncTaskItem)

    @Query(value = "SELECT " +
            "media.id, " +
            "media.uri, " +
            "media.immichUrl, " +
            "CASE WHEN media.type = 'Image' THEN true ELSE false END as isImage, " +
            "media.parentPath " +
            "FROM sync_task_item " +
            "JOIN media ON media.id = sync_task_item.mediaId " +
            "WHERE taskId = :taskId"
    )
    suspend fun getTaskItems(taskId: Int): List<SelectionManager.SelectedItem>
}