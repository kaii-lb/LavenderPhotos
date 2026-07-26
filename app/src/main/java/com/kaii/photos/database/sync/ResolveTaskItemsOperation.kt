package com.kaii.photos.database.sync

import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.mediastore.MediaType
import javax.inject.Inject

class ResolveTaskItemsOperation @Inject constructor(
    private val mediaDao: MediaDao
) {
    suspend fun execute(
        ids: List<Long>
    ): List<FileOperationItemMetadata> = ids.chunked(500).flatMap { chunk ->
        mediaDao.getMedia(ids = chunk).map {
            FileOperationItemMetadata(
                id = it.id,
                uri = it.uri,
                absolutePath = it.absolutePath,
                isImage = it.type == MediaType.Image,
                immichUrl = it.immichUrl
            )
        }
    }
}