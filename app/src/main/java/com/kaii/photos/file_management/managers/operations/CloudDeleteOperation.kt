package com.kaii.photos.file_management.managers.operations

import androidx.compose.ui.util.fastMap
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.entities.SyncOperation
import com.kaii.photos.database.sync.SyncTaskRecorder
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
    private val assetsClient: AssetsClient,
    private val recorder: SyncTaskRecorder
) {
    fun execute(
        files: List<FileOperationItemMetadata>,
        albumId: String
    ): Flow<FileOperationProgress<Unit>> = flow {
        if (files.isEmpty()) return@flow

        emit(
            value = FileOperationProgress.Started(
                action = FileOperationAction.LongOperationType.Delete,
                fileCount = files.size
            )
        )

        val ids = files.fastMap { it.id }

        recorder.record(
            operation = SyncOperation.Delete(
                sourceAlbumId = albumId
            ),
            mediaIds = ids,
            immichIds = files.associate { it.id to it.immichId },
            applyLocally = {
                ids.chunked(500).forEach { chunk ->
                    mediaDao.deleteAll(ids = chunk)
                    customDao.deleteAll(ids = chunk.toSet(), album = albumId)
                }
            },
            attemptRemote = {
                val targets = files.mapNotNull { it.immichId }

                if (targets.isEmpty()) {
                    Result.Success(Unit)
                } else {
                    val success = assetsClient.delete(ids = targets.map { Uuid.parse(it) }, force = albumId == "trash")
                    if (success) Result.Success(Unit) else Result.Error(FileOperationError.Failed)
                }
            }
        )

        files.forEach {
            emit(FileOperationProgress.ItemDone(uri = it.uri))
        }

        emit(FileOperationProgress.Finished(result = Result.Success(Unit)))
    }.flowOn(Dispatchers.IO)
}