package com.kaii.photos.file_management.managers.traits

import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.domain.files.FileOperationProgress
import kotlinx.coroutines.flow.Flow

interface Secure {
    suspend fun encryptFiles(files: List<FileOperationItemMetadata>): Flow<FileOperationProgress<Unit>>

    fun prepareEncryptFiles(files: List<FileOperationItemMetadata>): Result<Unit, FileOperationError>
}