package com.kaii.photos.file_management.managers.traits

import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.domain.files.FileOperationProgress
import kotlinx.coroutines.flow.Flow

interface Delete {
    suspend fun deleteFiles(
        files: List<FileOperationItemMetadata>,
        albumId: String,
        immichId: String?
    ): Flow<FileOperationProgress<Unit>>
}