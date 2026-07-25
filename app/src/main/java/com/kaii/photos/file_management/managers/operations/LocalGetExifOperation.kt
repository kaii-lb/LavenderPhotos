package com.kaii.photos.file_management.managers.operations

import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.getMediaByIds
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.file_management.managers.gateways.MediaStoreGateway
import com.kaii.photos.helpers.exif.MediaData

class LocalGetExifOperation(
    val mediaDao: MediaDao,
    val gateway: MediaStoreGateway
) {
    suspend fun execute(
        file: FileOperationItemMetadata
    ): Result<Map<MediaData, Any>, FileOperationError> {
        val media = mediaDao.getMediaByIds(
            files = listOf(file)
        ).firstOrNull() ?: return Result.Error(FileOperationError.Failed)

        return gateway.getExifData(media)
    }
}