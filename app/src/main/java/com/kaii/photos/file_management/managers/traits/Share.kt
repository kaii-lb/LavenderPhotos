package com.kaii.photos.file_management.managers.traits

import android.content.Intent
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.domain.files.FileOperationProgress
import kotlinx.coroutines.flow.Flow

interface Share {
    suspend fun shareFiles(
        files: List<FileOperationItemMetadata>
    ): Flow<FileOperationProgress<Intent>>
}