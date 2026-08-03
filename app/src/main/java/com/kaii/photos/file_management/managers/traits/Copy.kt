package com.kaii.photos.file_management.managers.traits

import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.domain.files.FileOperationCopyResult
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.domain.files.FileOperationProgress
import kotlinx.coroutines.flow.Flow

interface Copy {
    suspend fun copyFiles(
        files: List<FileOperationItemMetadata>,
        destination: AlbumType
    ): Flow<FileOperationProgress<List<FileOperationCopyResult>>>
}