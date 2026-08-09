package com.kaii.photos.file_management.managers.operations

import android.util.Log
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.getMediaFromMetadata
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FileOperationError
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
        emit(
            value = FileOperationProgress.Started(
                action = FileOperationAction.LongOperationType.Secure,
                fileCount = files.size
            )
        )

        val media = mediaDao.getMediaFromMetadata(files)

        var operationError: FileOperationError? = null
        val filesToBeDeleted = mutableListOf<FileOperationItemMetadata>()

        media.forEach { item ->
            when (val secureResult = secureManager.secure(mediaItem = item)) {
                is Result.Error -> {
                    if (operationError == null || operationError == FileOperationError.Failed) {
                        operationError = secureResult.error
                    }

                    Log.e(LocalEncryptOperation::class.qualifiedName, "Securing ${item.uri} failed: ${secureResult.error}")
                }

                is Result.Success -> {
                    filesToBeDeleted.add(secureResult.data)

                    emit(
                        value = FileOperationProgress.ItemDone(
                            uri = item.uri
                        )
                    )
                }
            }
        }

        if (operationError != null) {
            emit(
                value = FileOperationProgress.Finished(
                    result = Result.Error(operationError)
                )
            )

            return@flow
        }

        val deleteResult = gateway.delete(filesToBeDeleted)

        if (deleteResult is Result.Error) {
            Log.e(
                LocalEncryptOperation::class.qualifiedName,
                "Deleting original files after securing them failed: ${deleteResult.error}"
            )
        }

        emit(
            value = FileOperationProgress.Finished(
                result = deleteResult
            )
        )
    }.flowOn(Dispatchers.IO)
}