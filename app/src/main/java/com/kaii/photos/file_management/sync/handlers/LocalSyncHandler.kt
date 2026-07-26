package com.kaii.photos.file_management.sync.handlers

import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.file_management.sync.operations.SyncAlbumOperation
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class LocalSyncHandler(
    private val syncAlbum: SyncAlbumOperation,
    private val mediaDao: MediaDao,
    private val assetsClient: AssetsClient
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend fun sync(
        album: AlbumType.Folder
    ): Result<Unit, FileOperationError> {
        val immichId = album.immichId ?: return Result.Error(FileOperationError.Failed)
        val cloudMedia = assetsClient.getForAlbum(albumId = Uuid.parse(immichId)) ?: emptyList()
        val localMedia = mediaDao.getMediaInPaths(paths = album.paths)

        return syncAlbum.execute(
            cloudMedia = cloudMedia,
            localMedia = localMedia,
            originAlbumId = album.id,
            originImmichId = immichId,
            destinationPath = album.paths.first()
        )
    }
}