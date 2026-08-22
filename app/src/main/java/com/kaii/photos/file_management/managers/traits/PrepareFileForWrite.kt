package com.kaii.photos.file_management.managers.traits

import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata

interface PrepareFileForWrite {
    fun prepareFileForWrite(
        files: List<FileOperationItemMetadata>,
        followUpAction: FileOperationAction
    ): Result<Unit, FileOperationError>
}