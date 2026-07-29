package com.kaii.photos.file_management.managers.operations

import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.getMediaFromMetadata
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationCopyResult
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
        val mediaItems = mediaDao.getMediaFromMetadata(files)

        val newItems = mutableListOf<FileOperationCopyResult>()

        mediaItems.forEach { media ->
            destination.paths.forEach { path ->
                gateway.copy(media, path)

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

        send(
            element = FileOperationProgress.Finished(
                result = Result.Success(data = newItems.toList())
            )
        )
    }.flowOn(Dispatchers.IO)
}