package com.kaii.photos.domain.files

import com.kaii.photos.domain.Result
import com.kaii.photos.domain.mapTo

sealed interface FileOperationProgress<T> {
    data class Started<T>(
        val action: FileOperationAction.LongOperationType,
        val fileCount: Int
    ) : FileOperationProgress<T>

    data class ItemDone<T>(val uri: String) : FileOperationProgress<T>

    data class Finished<T>(
        val result: Result<T, FileOperationError>
    ) : FileOperationProgress<T>

    fun toGenericProgress(): FileOperationProgress<Unit> =
        when (this) {
            is Started -> Started(action = this.action, fileCount = this.fileCount)
            is ItemDone -> ItemDone(uri = this.uri)
            is Finished -> Finished(result = this.result.mapTo(Result.Success(data = Unit)))
        }
}