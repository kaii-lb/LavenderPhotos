package com.kaii.photos.file_management.managers.operations

import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.getMediaFromMetadata
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.file_management.managers.gateways.MediaStoreGateway
import com.kaii.photos.helpers.exif.MediaData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LocalGetExifOperation @Inject constructor(
    val mediaDao: MediaDao,
    val gateway: MediaStoreGateway
) {
    suspend fun execute(
        file: FileOperationItemMetadata
    ): Result<Map<MediaData, String>, FileOperationError> = withContext(Dispatchers.IO) {
        val media = mediaDao.getMediaFromMetadata(
            files = listOf(file)
        ).firstOrNull() ?: return@withContext Result.Error(FileOperationError.Failed)

        gateway.getExifData(media)
    }
}