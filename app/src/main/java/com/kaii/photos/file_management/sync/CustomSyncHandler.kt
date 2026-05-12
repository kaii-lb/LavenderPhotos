package com.kaii.photos.file_management.sync

import android.content.Context
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.preferences.SettingsAlbumsListImpl
import com.kaii.photos.file_management.managers.CustomFileManager
import com.kaii.photos.helpers.appCloudFolderDir
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CustomSyncHandler(
    override val fileManager: CustomFileManager,
    override val progressManager: ProgressManager,
    override val albums: SettingsAlbumsListImpl
) : GenericSyncHandler {
    @OptIn(ExperimentalUuidApi::class)
    suspend fun sync(
        context: Context,
        album: AlbumType.Custom
    ) {
        if (album.immichId == null) return

        val cloudMedia = fileManager.albumsClient.get(
            id = Uuid.parse(album.immichId)
        )?.assets ?: return

        val localMedia = fileManager.customDao.getMediaInAlbum(album = album.id)

        super.sync(
            context = context,
            cloudMedia = cloudMedia,
            localMedia = localMedia,
            originId = album.immichId,
            destinationPath = appCloudFolderDir
        )
    }
}