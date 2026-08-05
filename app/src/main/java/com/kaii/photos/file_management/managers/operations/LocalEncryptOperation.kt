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
        val media = mediaDao.getMediaFromMetadata(files)

        emit(
            value = FileOperationProgress.Started(
                action = FileOperationAction.LongOperationType.Secure,
                fileCount = files.size
            )
        )

        var operationError: FileOperationError? = null

        media.forEach { item ->
            when (val secureResult = secureManager.secure(mediaItem = item)) {
                is Result.Error -> {
                    if (operationError == null || operationError == FileOperationError.Failed) {
                        operationError = secureResult.error
                    }

                    Log.e(LocalEncryptOperation::class.qualifiedName, "Securing ${item.uri} failed: ${secureResult.error}")
                }

                is Result.Success -> {
                    val originalItem = secureResult.data
                    val deleteResult = gateway.delete(listOf(originalItem))

                    if (deleteResult is Result.Error) {
                        if (operationError == null || operationError == FileOperationError.Failed) {
                            operationError = deleteResult.error
                        }

                        Log.e(
                            LocalEncryptOperation::class.qualifiedName,
                            "Deleting original ${originalItem.uri} after securing it failed: ${deleteResult.error}"
                        )
                    }

                    emit(
                        value = FileOperationProgress.ItemDone(
                            uri = originalItem.uri
                        )
                    )
                }
            }
        }

        emit(
            value = FileOperationProgress.Finished(
                result =
                    if (operationError == null) Result.Success(data = Unit)
                    else Result.Error(operationError)
            )
        )
    }.flowOn(Dispatchers.IO)
}