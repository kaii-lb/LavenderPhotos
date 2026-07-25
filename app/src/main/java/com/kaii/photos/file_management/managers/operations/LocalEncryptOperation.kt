package com.kaii.photos.file_management.managers.operations

import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.getMediaByIds
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.file_management.secure.LocalSecureManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class LocalEncryptOperation(
    private val mediaDao: MediaDao,
    private val secureManager: LocalSecureManager
) {
    fun execute(
        files: List<FileOperationItemMetadata>
    ): Flow<FileOperationProgress<Unit>> = flow {
        val media = mediaDao.getMediaByIds(files)

        media.forEach { item ->
            val result = secureManager.secure(mediaItem = item)

            if (result is Result.Error) {
                emit(
                    value = FileOperationProgress.Finished(
                        result = Result.Error(result.error)
                    )
                )
            } else {
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
    }
}