package com.kaii.photos.file_management.sync

import android.content.Context
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.daos.SyncTaskDao
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.datastore.preferences.SettingsAlbumsListImpl
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import kotlinx.coroutines.flow.first
import kotlin.uuid.ExperimentalUuidApi

class LocalSyncHandler(
    override val mediaDao: MediaDao,
    override val customDao: CustomEntityDao,
    override val syncTaskDao: SyncTaskDao,
    override val assetClient: AssetsClient,
    override val albumsClient: AlbumsClient,
    override val info: ImmichBasicInfo,
    override val albums: SettingsAlbumsListImpl
) : GenericSyncHandler {
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun upload(
        context: Context,
        id: String
    ): Boolean {
        val album = albums.get().first().find { it.id == id } as? AlbumType.Folder ?: return false

        val immichId = getAlbumId(album) ?: return false

        albums.edit(
            id = id,
            newInfo = album.copy(
                immichId = immichId
            )
        )

        return uploadMedia(
            context = context,
            media = mediaDao.getMediaInPaths(paths = album.paths),
            albumImmichId = immichId
        )
    }

    override suspend fun download(context: Context, id: String) {
        throw IllegalAccessError("Cannot download an already downloaded album.")
    }
}