package com.kaii.photos.database.sync

import androidx.compose.ui.util.fastMap
import com.kaii.photos.database.daos.SyncTaskDao
import com.kaii.photos.database.entities.SyncTask
import com.kaii.photos.database.entities.SyncTaskType
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.preferences.SettingsAlbumsListImpl
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.domain.immich.SyncOutcome
import com.kaii.photos.domain.mapTo
import com.kaii.photos.file_management.managers.impl.CloudFileManager
import com.kaii.photos.file_management.managers.impl.LocalFileManager
import com.kaii.photos.file_management.sync.ProgressManager
import com.kaii.photos.file_management.sync.handlers.CloudCleanupHandler
import com.kaii.photos.file_management.sync.operations.SyncAllAlbumsOperation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudSyncManager @Inject constructor(
    private val syncTaskDao: SyncTaskDao,
    private val albums: SettingsAlbumsListImpl,
    private val cloudFileManager: CloudFileManager,
    private val localFileManager: LocalFileManager,
    private val resolveTaskItems: ResolveTaskItemsOperation,
    private val syncAllAlbums: SyncAllAlbumsOperation,
    private val cloudCleanup: CloudCleanupHandler,
    private val progressManager: ProgressManager
) {
    suspend fun syncUploads(): SyncOutcome {
        val unsynced = syncTaskDao.getUnsyncedTasks()
        val itemsByTask = unsynced.associateWith { task -> syncTaskDao.getTaskItems(taskId = task.id) }

        progressManager.startTracking(totalItems = itemsByTask.values.sumOf { it.size })

        val taskResults = itemsByTask.map { (task, taskItems) ->
            val files = resolveTaskItems.execute(ids = taskItems.fastMap { it.id })
            runTask(task, files)
        }

        ensureTracking()
        syncAllAlbums.execute()
        progressManager.stopTracking()

        cloudCleanup.cleanUp()

        return taskResults.toOutcome()
    }

    suspend fun syncFor(albumId: String): SyncOutcome {
        val album = albums.get().first().find { it.id == albumId } ?: return SyncOutcome.PermanentFailure

        ensureTracking()
        val outcome = when (album) {
            is AlbumType.Custom -> {
                syncAllAlbums.executeOne(album)
            }

            is AlbumType.Folder -> {
                syncAllAlbums.executeOne(album)
            }

            else -> Result.Success(Unit)
        }

        progressManager.stopTracking()

        return if (outcome is Result.Success) SyncOutcome.Success else SyncOutcome.TransientFailure
    }

    private fun ensureTracking() {
        if (progressManager.state == ProgressManager.State.Idle) progressManager.startTracking(totalItems = 0)
    }

    private suspend fun runTask(
        task: SyncTask,
        files: List<FileOperationItemMetadata>
    ): Result<Unit, FileOperationError> = when (task.type) {
        SyncTaskType.Upload -> collectAndReport(
            flow = localFileManager.copyFiles(
                files = files,
                destination = AlbumType.Cloud(
                    id = task.destination!!,
                    name = "",
                    pinned = false
                ),
                existingTaskId = task.id
            )
        )

        SyncTaskType.Trash -> collectAndReport(
            flow = cloudFileManager.trashFiles(
                files = files,
                isTrashed = true,
                albumId = task.destination!!,
                immichId = task.destination,
                existingTaskId = task.id
            )
        )

        SyncTaskType.Favourite -> cloudFileManager.favouriteFile(
            files = files,
            isFavourite = task.destination!!.toBoolean(),
            albumId = null,
            immichId = null,
            existingTaskId = task.id
        ).also { if (it is Result.Success) progressManager.increaseProgressBy(files.size) }

        SyncTaskType.Delete -> collectAndReport(
            flow = cloudFileManager.deleteFiles(
                files = files,
                albumId = task.destination!!,
                immichId = task.extraData!!,
                existingTaskId = task.id
            )
        )

        SyncTaskType.RenameAlbum -> {
            val album = albums.get().first().first { it.id == task.destination }

            cloudFileManager.renameAlbum(
                album = album,
                newName = task.extraData!!,
                existingTaskId = task.id
            )
        }

        SyncTaskType.Copy -> {
            val album = albums.get().first().first { it.id == task.destination } as AlbumType.Cloud

            collectAndReport(
                flow = cloudFileManager.copyFiles(
                    files = files,
                    destination = album,
                    existingTaskId = task.id
                )
            )
        }

        SyncTaskType.Move -> {
            val album = albums.get().first().first { it.id == task.destination } as AlbumType.Cloud

            collectAndReport(
                flow = cloudFileManager.moveFiles(
                    files = files,
                    destination = album,
                    existingTaskId = task.id,
                    origin = AlbumType.Cloud(
                        id = task.extraData!!,
                        name = "",
                        pinned = false
                    )
                )
            )
        }
    }

    private suspend fun <T> collectAndReport(
        flow: Flow<FileOperationProgress<T>>
    ): Result<Unit, FileOperationError> {
        var result: Result<Unit, FileOperationError> = Result.Error(FileOperationError.Failed)

        flow.collect { progress ->
            when (progress) {
                is FileOperationProgress.Started -> Unit
                is FileOperationProgress.ItemDone -> progressManager.increaseProgress()
                is FileOperationProgress.Finished -> result = progress.result.mapTo(Result.Success(Unit))
            }
        }

        return result
    }

    private fun List<Result<Unit, FileOperationError>>.toOutcome(): SyncOutcome = when {
        any { it is Result.Error && it.error == FileOperationError.Failed } -> SyncOutcome.TransientFailure
        all { it is Result.Success } -> SyncOutcome.Success
        else -> SyncOutcome.PermanentFailure
    }
}