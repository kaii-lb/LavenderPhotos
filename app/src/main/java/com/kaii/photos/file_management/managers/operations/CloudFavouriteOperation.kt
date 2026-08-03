package com.kaii.photos.file_management.managers.operations

import androidx.compose.ui.util.fastMap
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.entities.SyncOperation
import com.kaii.photos.database.sync.SyncTaskRecorder
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetFavouriteRequest
import javax.inject.Inject
import kotlin.uuid.Uuid

class CloudFavouriteOperation @Inject constructor(
    private val mediaDao: MediaDao,
    private val assetsClient: AssetsClient,
    private val recorder: SyncTaskRecorder
) {
    suspend fun execute(
        files: List<FileOperationItemMetadata>,
        isFavourite: Boolean
    ): Result<Unit, FileOperationError> {
        if (files.isEmpty()) return Result.Success(Unit)

        return recorder.record(
            operation = SyncOperation.SetFavourite(
                isFavourite = isFavourite
            ),
            mediaIds = files.fastMap { it.id },
            immichIds = files.associate { it.id to it.immichId },
            applyLocally = {
                mediaDao.setFavouriteOnMedia(
                    ids = files.fastMap { it.id }.toSet(),
                    favourite = isFavourite
                )

                Result.Success(Unit)
            },
            attemptRemote = {
                val targets = files.mapNotNull { it.immichId }

                if (targets.isEmpty()) {
                    Result.Success(Unit)
                } else {
                    val success = assetsClient.favourite(
                        request = AssetFavouriteRequest(
                            ids = targets.map { Uuid.parse(it) },
                            isFavorite = isFavourite
                        )
                    )

                    if (success) Result.Success(Unit) else Result.Error(FileOperationError.Failed)
                }
            }
        )
    }
}