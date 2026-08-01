package com.kaii.photos.file_management.managers.operations

import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.SyncTaskDao
import com.kaii.photos.database.entities.SyncTaskType
import com.kaii.photos.database.track
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.domain.files.FileOperationProgress
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import kotlin.uuid.Uuid

class CloudTrashOperation @Inject constructor(
    private val syncTaskDao: SyncTaskDao,
    private val customDao: CustomEntityDao,
    private val albumsClient: AlbumsClient,
    private val delete: CloudDeleteOperation
) {
    fun execute(
        files: List<FileOperationItemMetadata>,
        isTrashed: Boolean,
        albumId: String,
        immichId: String?,
        existingTaskId: Int?
    ): Flow<FileOperationProgress<Unit>> = flow<FileOperationProgress<Unit>> {
        check(isTrashed) {
            "Cannot restore files to cloud albums!!"
        }

        if (files.isEmpty()) return@flow

        if (immichId == null) {
            delete.execute(files, albumId, existingTaskId).collect {
                emit(value = it)
            }

            return@flow
        }

        emit(
            value = FileOperationProgress.Started(
                action = FileOperationAction.LongOperationType.TrashDelete,
                fileCount = files.size
            )
        )

        val result = syncTaskDao.track(
            existingTaskId = existingTaskId,
            type = SyncTaskType.Trash,
            destination = albumId,
            ids = files.map { it.id }
        ) {
            customDao.deleteAll(ids = files.map { it.id }.toSet(), album = immichId)

            val success = albumsClient.removeAssets(
                albumId = Uuid.parse(immichId),
                assetIds = files.map { Uuid.parse(it.immichId!!) }
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