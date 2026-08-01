package com.kaii.photos.file_management.managers.operations

import androidx.compose.ui.util.fastMap
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.daos.SyncTaskDao
import com.kaii.photos.database.entities.SyncTaskType
import com.kaii.photos.database.track
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.domain.files.FileOperationProgress
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import kotlin.uuid.Uuid

class CloudDeleteOperation @Inject constructor(
    private val mediaDao: MediaDao,
    private val customDao: CustomEntityDao,
    private val syncTaskDao: SyncTaskDao,
    private val assetsClient: AssetsClient
) {
    fun execute(
        files: List<FileOperationItemMetadata>,
        albumId: String,
        existingTaskId: Int?
    ): Flow<FileOperationProgress<Unit>> = flow {
        if (files.isEmpty()) return@flow

        emit(
            value = FileOperationProgress.Started(
                action = FileOperationAction.LongOperationType.Delete,
                fileCount = files.size
            )
        )

        val ids = files.fastMap { it.id }

        val result = syncTaskDao.track(
            existingTaskId = existingTaskId,
            type = SyncTaskType.Delete,
            destination = albumId,
            ids = ids
        ) {
            mediaDao.deleteAll(ids = ids.toSet())
            customDao.deleteAll(ids = ids.toSet(), album = albumId)

            val success = assetsClient.delete(
                ids = files.fastMap { Uuid.parse(it.immichId!!) },
                force = false
            )

            if (success) Result.Success(Unit)
            else Result.Error(FileOperationError.Failed)
        }

        emit(
            value = FileOperationProgress.Finished(
                result = result
            )
        )
    }.flowOn(Dispatchers.IO)
}