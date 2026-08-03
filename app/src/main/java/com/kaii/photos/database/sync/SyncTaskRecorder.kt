package com.kaii.photos.database.sync

import android.util.Log
import com.kaii.photos.database.daos.SyncTaskDao
import com.kaii.photos.database.entities.SyncOperation
import com.kaii.photos.database.entities.SyncTask
import com.kaii.photos.database.entities.SyncTaskItem
import com.kaii.photos.database.entities.SyncTaskStatus
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

@Singleton
class SyncTaskRecorder @Inject constructor(
    private val syncTaskDao: SyncTaskDao,
    private val networkMonitor: NetworkMonitor
) {
    /** @param applyLocally should immediately perform this operation
     * @param attemptRemote should try to sync to the cloud and be idempotent */
    suspend fun <T> record(
        operation: SyncOperation,
        mediaIds: List<Long>,
        immichIds: Map<Long, String?> = emptyMap(),
        applyLocally: suspend () -> T,
        attemptRemote: suspend () -> Result<Unit, FileOperationError>
    ): T = withContext(Dispatchers.IO) {
        val localResult = applyLocally()

        val taskId = syncTaskDao.insertTask(
            SyncTask(
                createdAt = Clock.System.now().epochSeconds,
                status = SyncTaskStatus.Waiting,
                operation = operation,
                isRemoval = operation.isRemoval
            )
        ).toInt()

        if (mediaIds.isNotEmpty()) {
            syncTaskDao.insertItems(
                items = mediaIds.map { mediaId ->
                    SyncTaskItem(
                        mediaId = mediaId,
                        taskId = taskId,
                        immichId = immichIds[mediaId]
                    )
                }
            )
        }

        if (networkMonitor.isOnline()) {
            val remoteResult = try {
                attemptRemote()
            } catch (e: Throwable) {
                Log.e(SyncTask::class.qualifiedName, e.message.toString())
                Result.Error(FileOperationError.Failed)
            }

            when (remoteResult) {
                is Result.Success -> syncTaskDao.markSynced(taskId)
                is Result.Error -> syncTaskDao.markFailedAttempt(taskId, remoteResult.error.toString())
            }
        }

        localResult
    }
}