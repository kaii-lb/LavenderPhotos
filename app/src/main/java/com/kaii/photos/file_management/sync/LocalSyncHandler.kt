package com.kaii.photos.file_management.sync

import android.content.Context
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.preferences.SettingsAlbumsListImpl
import com.kaii.photos.file_management.managers.LocalFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    ) = withContext(Dispatchers.IO) {
        if (album.immichId == null) return@withContext

        val cloudMedia = fileManager.assetClient.getForAlbum(
            albumId = Uuid.parse(album.immichId)
        ) ?: emptyList()

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