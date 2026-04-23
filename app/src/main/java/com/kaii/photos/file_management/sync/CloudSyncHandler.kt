package com.kaii.photos.file_management.sync

import android.content.Context
import android.provider.MediaStore
import androidx.compose.ui.util.fastMap
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.daos.SyncTaskDao
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.datastore.preferences.SettingsAlbumsListImpl
import com.kaii.photos.helpers.appCloudFolderDir
import com.kaii.photos.mediastore.copyUriToUri
import com.kaii.photos.mediastore.insertMedia
import com.kaii.photos.mediastore.toContentId
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import kotlinx.coroutines.flow.first

class CloudSyncHandler(
    override val mediaDao: MediaDao,
    override val customDao: CustomEntityDao,
    override val syncTaskDao: SyncTaskDao,
    override val assetClient: AssetsClient,
    override val albumsClient: AlbumsClient,
    override val info: ImmichBasicInfo,
    override val albums: SettingsAlbumsListImpl

) : GenericSyncHandler {
    override suspend fun upload(context: Context, id: String): Boolean {
        throw IllegalAccessError("Cannot upload an already uploaded album.")
    }

    override suspend fun download(context: Context, id: String) {
        val path = appCloudFolderDir

        val oldAlbum = albums.get().first().find { it.id == id } ?: return
        albums.remove(albumId = id)
        albums.add(
            listOf(
                AlbumType.Folder(
                    id = id,
                    name = oldAlbum.name,
                    pinned = oldAlbum.pinned,
                    immichId = id,
                    paths = setOf(path)
                )
            )
        )

        val media = customDao.getMediaInAlbum(album = id)
        val existing = media.chunked(500).flatMap { chunk ->
            mediaDao.getMedia(ids = chunk.fastMap { it.id })
        }

        val items = media - existing.toSet()
        val newItems = mutableMapOf<Long, MediaStoreData>()

        items.forEach { item ->
            context.contentResolver.insertMedia(
                context = context,
                media = item,
                destination = path,
                overrideDisplayName = null,
                currentVolumes = MediaStore.getExternalVolumeNames(context),
                preserveDate = true,
                onInsert = { original, new ->
                    context.contentResolver.copyUriToUri(original, new)

                    val id = new.toContentId(
                        contentResolver = context.contentResolver,
                        type = item.type
                    )

                    newItems[item.id] = (item.copy(id = id))
                }
            )
        }

        newItems.forEach { (oldId, item) ->
            mediaDao.delete(id = oldId)
            mediaDao.insert(item)
        }
    }
}