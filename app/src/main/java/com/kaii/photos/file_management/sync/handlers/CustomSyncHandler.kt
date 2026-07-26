package com.kaii.photos.file_management.sync.handlers

import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.file_management.sync.operations.SyncAlbumOperation
import com.kaii.photos.helpers.appCloudFolderDir
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CustomSyncHandler(
    private val syncAlbum: SyncAlbumOperation,
    private val customDao: CustomEntityDao,
    private val assetsClient: AssetsClient
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend fun sync(
        album: AlbumType.Custom
    ) {
        val immichId = album.immichId ?: return
        val cloudMedia = assetsClient.getForAlbum(albumId = Uuid.parse(immichId)) ?: emptyList()
        val localMedia = customDao.getMediaInAlbum(album = album.id)

        syncAlbum.execute(
            cloudMedia = cloudMedia,
            localMedia = localMedia,
            originAlbumId = album.id,
            originImmichId = immichId,
            destinationPath = appCloudFolderDir.absolutePath.removeSuffix("/")
        )

    }
}