package com.kaii.photos.file_management.managers.operations

import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.SyncTaskDao
import com.kaii.photos.database.entities.SyncTaskType
import com.kaii.photos.database.track
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import javax.inject.Inject
import kotlin.uuid.Uuid

class CloudTrashOperation @Inject constructor(
    private val syncTaskDao: SyncTaskDao,
    private val customDao: CustomEntityDao,
    private val albumsClient: AlbumsClient,
    private val delete: CloudDeleteOperation
) {
    suspend fun execute(
        files: List<FileOperationItemMetadata>,
        isTrashed: Boolean,
        albumId: String,
        immichId: String?,
        existingTaskId: Int?
    ): Result<Unit, FileOperationError> {
        if (files.isEmpty()) return Result.Success(Unit)
        if (!isTrashed) throw IllegalArgumentException("Cannot restore files to albums!")
        if (immichId == null) return delete.execute(files, albumId, existingTaskId) // TODO: check existingTaskId if it works with the same id for two op types

        return syncTaskDao.track(
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
    }
}