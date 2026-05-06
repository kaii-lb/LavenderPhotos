package com.kaii.photos.file_management.sync

import android.content.Context
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.preferences.SettingsAlbumsListImpl
import com.kaii.photos.file_management.managers.LocalFileManager
import kotlinx.coroutines.flow.first
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
        id: String
    ) {
        val album = albums.get().first().find { it.id == id } as? AlbumType.Folder ?: return

        if (album.immichId == null) return

        val cloudMedia = fileManager.albumsClient.get(
            id = Uuid.parse(id),
            accessToken = fileManager.info.accessToken
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