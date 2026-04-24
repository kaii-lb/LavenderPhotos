package com.kaii.photos.file_management.sync

import android.content.Context
import android.provider.MediaStore
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.datastore.preferences.SettingsAlbumsListImpl
import com.kaii.photos.helpers.appCloudFolderDir
import com.kaii.photos.mediastore.getAbsolutePathFromUri
import com.kaii.photos.mediastore.insertMedia
import com.kaii.photos.mediastore.setDateForMedia
import com.kaii.photos.mediastore.toContentId
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import kotlinx.coroutines.flow.first
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CloudSyncHandler(
    override val mediaDao: MediaDao,
    override val customDao: CustomEntityDao,
    override val assetClient: AssetsClient,
    override val albumsClient: AlbumsClient,
    override val info: ImmichBasicInfo,
    override val albums: SettingsAlbumsListImpl
) : GenericSyncHandler {
    override suspend fun upload(context: Context, id: String): Boolean {
        throw IllegalAccessError("Cannot upload an already uploaded album.")
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun download(context: Context, id: String) {
        val oldAlbum = albums.get().first().find { it.id == id } ?: return

        val path = appCloudFolderDir + "/" + oldAlbum.name

        if (oldAlbum is AlbumType.Cloud) {
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
        }

        val media = customDao.getMediaInAlbum(album = id)

        val items = media.filter { it.isCloud }
        val newItems = mutableMapOf<Long, MediaStoreData?>()

        items.forEach { item ->
            val bytes = assetClient.download(
                id = Uuid.parse(item.immichId!!),
                accessToken = info.accessToken
            )

            if (bytes == null) return@forEach

            context.contentResolver.insertMedia(
                context = context,
                media = item,
                destination = path,
                overrideDisplayName = null,
                currentVolumes = MediaStore.getExternalVolumeNames(context),
                preserveDate = true,
                onInsert = { _, new ->
                    val newId = new.toContentId(contentResolver = context.contentResolver, type = item.type)

                    context.contentResolver.openOutputStream(new)?.use {
                        if (bytes.size <= 8 * 1024) it.write(bytes)
                        else it.buffered().write(bytes)

                        it.flush()
                        it.close()
                    }

                    newItems[item.id] = null
                    context.contentResolver.getAbsolutePathFromUri(new)?.let { absolutePath ->
                        newItems[item.id] = item.copy(
                            id = newId,
                            uri = new.toString(),
                            absolutePath = absolutePath,
                            parentPath = path
                        )
                    }

                    context.contentResolver.setDateForMedia(
                        uri = new,
                        type = item.type,
                        dateTaken = item.dateTaken
                    )
                }
            )
        }

        newItems.forEach { (oldId, item) ->
            mediaDao.delete(id = oldId)
            item?.let { mediaDao.insert(it) }
        }
    }
}