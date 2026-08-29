package com.kaii.photos.file_management.managers.operations

import androidx.compose.ui.util.fastMap
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.entities.SyncOperation
import com.kaii.photos.database.sync.SyncTaskRecorder
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.domain.files.FileOperationProgress
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import kotlin.uuid.Uuid

class CloudTrashOperation @Inject constructor(
    private val customDao: CustomEntityDao,
    private val albumsClient: AlbumsClient,
    private val assetsClient: AssetsClient,
    private val delete: CloudDeleteOperation,
    private val recorder: SyncTaskRecorder
) {
    fun execute(
        files: List<FileOperationItemMetadata>,
        isTrashed: Boolean,
        albumId: String,
        immichId: String?
    ): Flow<FileOperationProgress<Unit>> = flow {
        if (files.isEmpty()) return@flow

        if (immichId == null && isTrashed) {
            delete.execute(files, albumId).collect { emit(it) }
            return@flow
        } else if (immichId == null) {
            restore(files).collect { emit(it) }
            return@flow
        }

        check(isTrashed) {
            "Cannot restore files to cloud albums!!"
        }

        emit(
            value = FileOperationProgress.Started(
                action = FileOperationAction.LongOperationType.TrashDelete,
                fileCount = files.size
            )
        )

        val ids = files.fastMap { it.id }

        recorder.record(
            operation = SyncOperation.RemoveFromAlbum(
                sourceAlbumId = albumId,
                immichAlbumId = immichId
            ),
            mediaIds = ids,
            immichIds = files.associate { it.id to it.immichId },
            applyLocally = {
                customDao.deleteAll(
                    ids = ids.toSet(),
                    album = immichId
                )
            },
            attemptRemote = {
                val targets = files.mapNotNull { it.immichId }

                if (targets.isEmpty()) {
                    Result.Success(Unit)
                } else {
                    val success = albumsClient.removeAssets(
                        albumId = Uuid.parse(immichId),
                        assetIds = targets.map { Uuid.parse(it) }
                    )

                    if (success) Result.Success(Unit)
                    else Result.Error(FileOperationError.Failed)
                }
            }
        )

        files.forEach {
            emit(FileOperationProgress.ItemDone(uri = it.uri))
        }

        emit(FileOperationProgress.Finished(result = Result.Success(Unit)))
    }.flowOn(Dispatchers.IO)

    private fun restore(
        files: List<FileOperationItemMetadata>
    ) = flow {
        emit(
            value = FileOperationProgress.Started(
                action = FileOperationAction.LongOperationType.Delete,
                fileCount = files.size
            )
        )

        val result = assetsClient.restore(
            ids = files.map {
                Uuid.parse(it.immichId!!)
            }
        ) != null

        emit(
            value =
                FileOperationProgress.Finished(
                    result =
                        if (result) Result.Success(Unit)
                        else Result.Error(error = FileOperationError.Failed)
                )
        )
    }
}