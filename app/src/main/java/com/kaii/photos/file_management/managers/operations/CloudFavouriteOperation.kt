package com.kaii.photos.file_management.managers.operations

import androidx.compose.ui.util.fastMap
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.daos.SyncTaskDao
import com.kaii.photos.database.entities.SyncTaskType
import com.kaii.photos.database.track
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetFavouriteRequest
import kotlin.uuid.Uuid

class CloudFavouriteOperation(
    private val mediaDao: MediaDao,
    private val syncTaskDao: SyncTaskDao,
    private val assetsClient: AssetsClient
) {
    suspend fun execute(
        files: List<FileOperationItemMetadata>,
        isFavourite: Boolean,
        existingTaskId: Int?
    ): Result<Unit, FileOperationError> {
        if (files.isEmpty()) return Result.Success(Unit)

        val ids = files.fastMap { it.id }

        return syncTaskDao.track(
            existingTaskId = existingTaskId,
            type = SyncTaskType.Favourite,
            destination = null,
            ids = ids
        ) {
            mediaDao.setFavouriteOnMedia(
                ids = ids.toSet(),
                favourite = isFavourite
            )

            val success = assetsClient.favourite(
                request = AssetFavouriteRequest(
                    ids = files.fastMap { Uuid.parse(it.immichId!!) },
                    isFavorite = isFavourite
                )
            )

            if (success) Result.Success(Unit)
            else Result.Error(FileOperationError.Failed)
        }
    }
}