package com.kaii.photos.domain.files

import com.kaii.photos.domain.Error

data class FilePermissionError(
    val action: FileOperationAction
) : Error