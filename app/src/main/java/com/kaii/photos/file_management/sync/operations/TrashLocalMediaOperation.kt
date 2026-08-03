package com.kaii.photos.file_management.sync.operations

import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.file_management.managers.impl.LocalFileManager
import com.kaii.photos.mediastore.MediaType
import javax.inject.Inject

class TrashLocalMediaOperation @Inject constructor(
    private val fileManager: LocalFileManager
) {
    suspend fun execute(
        media: List<MediaStoreData>,
        albumId: String
    ): Result<Unit, FileOperationError> {
        if (media.isEmpty()) return Result.Success(Unit)

        var result: Result<Unit, FileOperationError> = Result.Success(Unit)

        fileManager.trashFiles(
            files = media.map {
                FileOperationItemMetadata(
                    id = it.id,
                    uri = it.uri,
                    absolutePath = it.absolutePath,
                    isImage = it.type == MediaType.Image,
                    immichUrl = it.immichUrl
                )
            },
            isTrashed = true,
            albumId = albumId,
            immichId = null
        ).collect { progress ->
            if (progress is FileOperationProgress.Finished) {
                result = progress.result
            }
        }

        return result
    }
}