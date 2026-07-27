package com.kaii.photos.database

import androidx.compose.ui.util.fastMap
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.daos.SyncTaskDao
import com.kaii.photos.database.entities.SyncTask
import com.kaii.photos.database.entities.SyncTaskItem
import com.kaii.photos.database.entities.SyncTaskStatus
import com.kaii.photos.database.entities.SyncTaskType
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata
import kotlin.time.Clock

suspend fun MediaDao.getMediaFromMetadata(
    files: List<FileOperationItemMetadata>
) = files.chunked(500).flatMap { chunk ->
    this.getMedia(ids = chunk.fastMap { it.id })
}

suspend fun <T> SyncTaskDao.track(
    existingTaskId: Int?,
    type: SyncTaskType,
    destination: String?,
    ids: List<Long>,
    block: suspend () -> Result<T, FileOperationError>
): Result<T, FileOperationError> {
    val taskId = existingTaskId ?: insert(
        task = SyncTask(
            dateModified = Clock.System.now().epochSeconds,
            status = SyncTaskStatus.Processing,
            type = type,
            destination = destination,
            extraData = destination
        )
    ).toInt()

    if (ids.isNotEmpty()) insert(items = ids.map { SyncTaskItem(mediaId = it, taskId = taskId) })

    val result = block()

    updateTaskStatus(
        id = taskId,
        status =
            if (result is Result.Success) SyncTaskStatus.Synced
            else SyncTaskStatus.Waiting
    )

    return result
}
