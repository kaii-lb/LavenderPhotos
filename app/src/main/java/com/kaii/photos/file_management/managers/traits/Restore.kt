package com.kaii.photos.file_management.managers.traits

import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.domain.files.FileOperationProgress
import kotlinx.coroutines.flow.Flow

interface Restore {
    suspend fun decryptFiles(files: List<FileOperationItemMetadata>): Flow<FileOperationProgress<Unit>>

    suspend fun clearCaches()
}