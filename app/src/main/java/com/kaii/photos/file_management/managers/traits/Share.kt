package com.kaii.photos.file_management.managers.traits

import android.content.Intent
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata

interface Share {
    suspend fun shareFiles(files: List<FileOperationItemMetadata>): Result<Intent, FileOperationError>
}