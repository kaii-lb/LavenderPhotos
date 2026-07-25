package com.kaii.photos.file_management.secure

import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.helpers.paging.PhotoLibraryUIModel

interface GenericSecureManager {
    /** returns file to be permanently deleted */
    suspend fun secure(
        mediaItem: MediaStoreData
    ): Result<FileOperationItemMetadata, FileOperationError>

    /** return success state of the operation */
    suspend fun restore(
        media: PhotoLibraryUIModel.SecuredMedia
    ): Result<Unit, FileOperationError>
}