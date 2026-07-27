package com.kaii.photos.file_management.managers.traits

import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata

interface Delete {
    suspend fun deleteFiles(
        files: List<FileOperationItemMetadata>,
        albumId: String,
        immichId: String?,
        existingTaskId: Int?
    ): Result<Unit, FileOperationError>
}