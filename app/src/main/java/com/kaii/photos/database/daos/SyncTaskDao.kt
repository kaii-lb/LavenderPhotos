package com.kaii.photos.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kaii.photos.database.entities.SyncTask
import com.kaii.photos.database.entities.SyncTaskItem
import com.kaii.photos.domain.files.FileOperationItemMetadata
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncTaskDao {
    @Query(value = "SELECT * FROM sync_tasks WHERE status = 'Waiting' ORDER BY id ASC")
    suspend fun getUnsyncedTasks(): List<SyncTask>

    @Insert
    suspend fun insertTask(task: SyncTask): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItems(items: List<SyncTaskItem>)

    @Query(value = "UPDATE sync_tasks SET status = 'Synced' WHERE id = :id")
    suspend fun markSynced(id: Int)

    @Query(
        value = "UPDATE sync_tasks SET " +
                "attempts = attempts + 1, " +
                "lastError = :error, " +
                "status = CASE WHEN attempts + 1 >= :maxAttempts THEN 'Failed' ELSE 'Waiting' END " +
                "WHERE id = :id"
    )
    suspend fun markFailedAttempt(id: Int, error: String?, maxAttempts: Int = SyncTask.MAX_ATTEMPTS)

    @Query(value = "DELETE FROM sync_tasks WHERE status = 'Synced'")
    suspend fun pruneSyncedTasks()

    @Query(value = "SELECT * FROM sync_task_item WHERE taskId = :taskId")
    suspend fun getTaskItemRows(taskId: Int): List<SyncTaskItem>

    @Query(value = "SELECT * FROM sync_task_item WHERE taskId IN (:taskIds)")
    suspend fun getTaskItemRowsForTasks(taskIds: List<Int>): List<SyncTaskItem>

    @Query(
        value = "SELECT " +
                "media.id, " +
                "media.uri, " +
                "media.absolutePath, " +
                "media.immichUrl, " +
                "media.parentPath, " +
                "CASE WHEN media.type = 'Image' THEN true ELSE false END as isImage " +
                "FROM sync_task_item " +
                "JOIN media ON media.id = sync_task_item.mediaId " +
                "WHERE taskId = :taskId"
    )
    suspend fun getTaskItemsWithLocalFile(taskId: Int): List<FileOperationItemMetadata>

    @Query(
        value = "SELECT DISTINCT sync_task_item.immichId FROM sync_task_item " +
                "JOIN sync_tasks ON sync_tasks.id = sync_task_item.taskId " +
                "WHERE sync_tasks.status = 'Waiting' AND sync_tasks.isRemoval = 1 AND sync_task_item.immichId IS NOT NULL"
    )
    suspend fun getPendingRemovalImmichIds(): List<String>

    @Query(
        value = "SELECT DISTINCT mediaId FROM sync_task_item " +
                "JOIN sync_tasks ON sync_tasks.id = sync_task_item.taskId " +
                "WHERE sync_tasks.status = 'Waiting'"
    )
    fun observePendingMediaIds(): Flow<List<Long>>
}