package com.kaii.photos.database.sync

import com.kaii.photos.database.daos.SyncTaskDao
import com.kaii.photos.database.entities.SyncOperation
import com.kaii.photos.database.entities.SyncTask
import com.kaii.photos.database.entities.SyncTaskItem
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.preferences.SettingsAlbumsListImpl
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.immich.SyncOutcome
import com.kaii.photos.file_management.managers.operations.LocalToCloudOperation
import com.kaii.photos.file_management.sync.ProgressManager
import com.kaii.photos.file_management.sync.handlers.CloudCleanupHandler
import com.kaii.photos.file_management.sync.operations.SyncAllAlbumsOperation
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import io.github.kaii_lb.lavender.immichintegration.serialization.albums.AlbumUpdateDto
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetFavouriteRequest
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.uuid.Uuid

@Singleton
class CloudSyncManager @Inject constructor(
    private val syncTaskDao: SyncTaskDao,
    private val albums: SettingsAlbumsListImpl,
    private val albumsClient: AlbumsClient,
    private val assetsClient: AssetsClient,
    private val localToCloud: LocalToCloudOperation,
    private val syncAllAlbums: SyncAllAlbumsOperation,
    private val cloudCleanup: CloudCleanupHandler,
    private val progressManager: ProgressManager
) {
    suspend fun syncUploads(): SyncOutcome {
        val unsynced = syncTaskDao.getUnsyncedTasks()

        val itemsByTaskId = if (unsynced.isEmpty()) {
            emptyMap()
        } else {
            syncTaskDao.getTaskItemRowsForTasks(unsynced.map { it.id }).groupBy { it.taskId }
        }

        progressManager.startTracking(totalItems = itemsByTaskId.values.sumOf { it.size })

        var anyFailure = false

        for (task in unsynced) {
            val items = itemsByTaskId[task.id].orEmpty()

            val result = try {
                runTask(task, items)
            } catch (_: Throwable) {
                Result.Error(FileOperationError.Failed)
            }

            when (result) {
                is Result.Success -> syncTaskDao.markSynced(task.id)
                is Result.Error -> {
                    syncTaskDao.markFailedAttempt(task.id, result.error.toString())
                    anyFailure = true
                }
            }

            progressManager.increaseProgressBy(items.size)
        }

        ensureTracking()

        syncAllAlbums.execute()
        progressManager.stopTracking()

        cloudCleanup.cleanUp()
        syncTaskDao.pruneSyncedTasks()

        return if (anyFailure) SyncOutcome.TransientFailure else SyncOutcome.Success
    }

    suspend fun syncFor(albumId: String): SyncOutcome {
        val album = albums.get().first().find { it.id == albumId } ?: return SyncOutcome.PermanentFailure

        ensureTracking()
        val outcome = when (album) {
            is AlbumType.Custom -> syncAllAlbums.executeOne(album)
            is AlbumType.Folder -> syncAllAlbums.executeOne(album)
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
        items: List<SyncTaskItem>
    ): Result<Unit, FileOperationError> {
        val immichIds = items.mapNotNull {
            it.immichId
        }.map {
            Uuid.parse(it)
        }

        return when (val operation = task.operation) {
            is SyncOperation.Upload -> {
                val files = syncTaskDao.getTaskItemsWithLocalFile(task.id)

                if (files.isEmpty()) return Result.Success(Unit)

                when (val result = localToCloud.uploadAndLink(
                    files = files,
                    destinationAlbumId = operation.destinationAlbumId
                )) {
                    is Result.Success -> Result.Success(Unit)
                    is Result.Error -> Result.Error(result.error)
                }
            }

            is SyncOperation.AddToAlbum -> {
                if (immichIds.isEmpty()) return Result.Success(Unit)

                val success = albumsClient.addAssets(
                    albumId = Uuid.parse(operation.destinationAlbumId),
                    assetIds = immichIds
                )

                if (success) Result.Success(Unit) else Result.Error(FileOperationError.Failed)
            }

            is SyncOperation.RemoveFromAlbum -> {
                if (immichIds.isEmpty()) return Result.Success(Unit)

                val success = albumsClient.removeAssets(
                    albumId = Uuid.parse(operation.immichAlbumId),
                    assetIds = immichIds
                )

                if (success) Result.Success(Unit) else Result.Error(FileOperationError.Failed)
            }

            is SyncOperation.Delete -> {
                if (immichIds.isEmpty()) return Result.Success(Unit)

                val success = assetsClient.delete(
                    ids = immichIds,
                    force = false
                )

                if (success) Result.Success(Unit) else Result.Error(FileOperationError.Failed)
            }

            is SyncOperation.SetFavourite -> {
                if (immichIds.isEmpty()) return Result.Success(Unit)

                val success = assetsClient.favourite(
                    request = AssetFavouriteRequest(
                        ids = immichIds, isFavorite
                        = operation.isFavourite
                    )
                )

                if (success) Result.Success(Unit) else Result.Error(FileOperationError.Failed)
            }

            is SyncOperation.RenameAlbum -> {
                val album = albums.get().first().firstOrNull { it.id == operation.albumLocalId }
                    ?: return Result.Error(FileOperationError.Failed)

                val immichId = album.immichId ?: return Result.Success(Unit)

                val success = albumsClient.update(
                    id = Uuid.parse(immichId),
                    info = AlbumUpdateDto(
                        albumName = operation.newName,
                        albumThumbnailAssetId = null,
                        description = null,
                        isActivityEnabled = null,
                        order = null
                    )
                )

                if (success) Result.Success(Unit) else Result.Error(FileOperationError.Failed)
            }
        }
    }
}