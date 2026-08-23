package com.kaii.photos.file_management.managers.traits

import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationError

interface ClearExif {
    suspend fun clearExifData(absolutePath: String): Result<Unit, FileOperationError>
}