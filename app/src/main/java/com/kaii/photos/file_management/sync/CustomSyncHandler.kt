package com.kaii.photos.file_management.sync

import android.content.Context
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.preferences.SettingsAlbumsListImpl
import com.kaii.photos.file_management.managers.CustomFileManager
import com.kaii.photos.helpers.appCloudFolderDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    ) = withContext(Dispatchers.IO) {
        if (album.immichId == null) return@withContext

        val cloudMedia = fileManager.assetClient.getForAlbum(
            albumId = Uuid.parse(album.immichId)
        ) ?: emptyList()

        val localMedia = fileManager.customDao.getMediaInAlbum(album = album.id)

        super.sync(
            context = context,
            cloudMedia = cloudMedia,
            localMedia = localMedia,
            originId = album.immichId,
            destinationPath = appCloudFolderDir.absolutePath.removeSuffix("/")
        )
    }
}