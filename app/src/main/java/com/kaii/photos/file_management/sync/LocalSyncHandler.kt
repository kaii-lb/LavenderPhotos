package com.kaii.photos.file_management.sync

import android.content.Context
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.preferences.SettingsAlbumsListImpl
import com.kaii.photos.file_management.managers.LocalFileManager
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class LocalSyncHandler(
    override val fileManager: LocalFileManager,
    override val progressManager: ProgressManager,
    override val albums: SettingsAlbumsListImpl
) : GenericSyncHandler {
    @OptIn(ExperimentalUuidApi::class)
    suspend fun sync(
        context: Context,
        album: AlbumType.Folder
    ) {
        if (album.immichId == null) return

        val cloudMedia = fileManager.albumsClient.get(
            id = Uuid.parse(album.immichId)
        )?.assets ?: return

        val localMedia = fileManager.mediaDao.getMediaInPaths(paths = album.paths)

        super.sync(
            context = context,
            cloudMedia = cloudMedia,
            localMedia = localMedia,
            originId = album.immichId,
            destinationPath = album.paths.first()
        )
    }
}