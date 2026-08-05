package com.kaii.photos.file_management.managers.operations

import android.util.Log
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.getMediaFromMetadata
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FileOperationCopyResult
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.file_management.managers.gateways.AndroidMediaStoreGateway
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class LocalToLocalOperation @Inject constructor(
    private val mediaDao: MediaDao,
    private val gateway: AndroidMediaStoreGateway
) {
    fun execute(
        files: List<FileOperationItemMetadata>,
        destination: AlbumType.Folder
    ): Flow<FileOperationProgress<List<FileOperationCopyResult>>> = channelFlow {
        send(
            element = FileOperationProgress.Started(
                action = FileOperationAction.LongOperationType.Copy,
                fileCount = files.size
            )
        )

        val mediaItems = mediaDao.getMediaFromMetadata(files)

        val newItems = mutableListOf<FileOperationCopyResult>()
        var operationError: FileOperationError? = null

        mediaItems.forEach { media ->
            destination.paths.forEach { path ->
                when (val result = gateway.copy(media, path)) {
                    is Result.Success -> newItems.add(result.data)

                    is Result.Error -> {
                        if (operationError == null || operationError == FileOperationError.Failed) {
                            operationError = result.error
                        }

                        Log.e(LocalToLocalOperation::class.qualifiedName, "Copying ${media.uri} to $path failed: ${result.error}")
                    }
                }

                send(
                    element = FileOperationProgress.ItemDone(
                        uri = media.uri
                    )
                )
            }
        }

        if (destination.immichId != null) {
            gateway.enqueueSyncWorker(albumId = destination.id)
        }

        val result =
            if (operationError == null) {
                Result.Success(data = newItems.toList())
            } else {
                Result.Error(operationError)
            }

        send(
            element = FileOperationProgress.Finished(
                result = result
            )
        )
    }.flowOn(Dispatchers.IO)
}