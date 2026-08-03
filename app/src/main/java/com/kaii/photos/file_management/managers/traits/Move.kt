package com.kaii.photos.file_management.managers.traits

import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.domain.files.FileOperationCopyResult
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.domain.files.FileOperationProgress
import kotlinx.coroutines.flow.Flow

interface Move {
    suspend fun moveFiles(
        files: List<FileOperationItemMetadata>,
        destination: AlbumType,
        origin: AlbumType?
    ): Flow<FileOperationProgress<List<FileOperationCopyResult>>>
}