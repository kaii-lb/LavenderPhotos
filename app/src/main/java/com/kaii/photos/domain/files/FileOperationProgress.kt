package com.kaii.photos.domain.files

import com.kaii.photos.domain.Result

sealed interface FileOperationProgress <T> {
    data class ItemDone<T>(val uri: String) : FileOperationProgress<T>

    data class Finished<T>(
        val result: Result<T, FileOperationError>
    ) : FileOperationProgress<T>
}