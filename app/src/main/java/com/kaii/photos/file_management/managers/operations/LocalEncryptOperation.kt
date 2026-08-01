package com.kaii.photos.file_management.managers.operations

import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.getMediaFromMetadata
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.file_management.managers.gateways.MediaStoreGateway
import com.kaii.photos.file_management.secure.LocalSecureManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class LocalEncryptOperation @Inject constructor(
    private val mediaDao: MediaDao,
    private val gateway: MediaStoreGateway,
    private val secureManager: LocalSecureManager
) {
    fun execute(
        files: List<FileOperationItemMetadata>
    ): Flow<FileOperationProgress<Unit>> = flow {
        val media = mediaDao.getMediaFromMetadata(files)

        emit(value = FileOperationProgress.Started(
            action = FileOperationAction.LongOperationType.Secure,
            fileCount = files.size
        ))

        media.forEach { item ->
            val result = secureManager.secure(mediaItem = item)

            if (result is Result.Error) {
                emit(
                    value = FileOperationProgress.Finished(
                        result = Result.Error(result.error)
                    )
                )
            } else {
                gateway.delete(files)

                emit(
                    value = FileOperationProgress.ItemDone(
                        uri = (result as Result.Success).data.uri
                    )
                )
            }
        }

        emit(
            value = FileOperationProgress.Finished(
                result = Result.Success(data = Unit)
            )
        )
    }.flowOn(Dispatchers.IO)
}