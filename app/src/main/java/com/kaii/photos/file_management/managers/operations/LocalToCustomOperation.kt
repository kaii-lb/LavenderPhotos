package com.kaii.photos.file_management.managers.operations

import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.entities.CustomItem
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationCopyResult
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.file_management.managers.gateways.CloudCacheGateway
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class LocalToCustomOperation @Inject constructor(
    private val customDao: CustomEntityDao,
    private val gateway: CloudCacheGateway
) {
    fun execute(
        mediaIds: List<Long>,
        destination: AlbumType.Custom
    ): Flow<FileOperationProgress<List<FileOperationCopyResult>>> = channelFlow {
        customDao.upsertAll(
            items = mediaIds.map {
                CustomItem(id = it, album = destination.id)
            }
        )

        val mediaItems =
            customDao.getMediaInAlbum(album = destination.id).filter { it.id in mediaIds }

        if (destination.immichId != null) {
            gateway.enqueueSyncWorker(destination.id)
        }

        send(
            element = FileOperationProgress.Finished(
                result = Result.Success(
                    data = mediaItems.map {
                        FileOperationCopyResult(id = it.id, immichId = it.immichId)
                    }
                )
            )
        )
    }.flowOn(Dispatchers.IO)
}