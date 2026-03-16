package com.kaii.photos.helpers.file_management

import android.content.Context
import android.content.IntentSender
import androidx.compose.ui.util.fastMap
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.helpers.grid_management.SelectionManager
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetFavouriteRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.reflect.KClass
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CloudFileManager(
    override val assetClient: AssetsClient,
    override val albumsClient: AlbumsClient,
    override val customDao: CustomEntityDao,
    override val accessToken: String
) : GenericFileManager {
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun setFavourite(
        context: Context,
        favourite: Boolean,
        list: List<String>
    ) {
        assetClient.favourite(
            request = AssetFavouriteRequest(
                ids = list.fastMap { Uuid.parse(it) },
                isFavorite = favourite
            ),
            accessToken = accessToken
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun setTrashed(
        context: Context,
        list: List<String>,
        trashed: Boolean
    ) {
        if (trashed) {
            assetClient.delete(
                ids = list.fastMap { Uuid.parse(it) },
                accessToken = accessToken
            )
        } else {
            assetClient.restore(
                ids = list.fastMap { Uuid.parse(it) },
                accessToken = accessToken
            )
        }
    }

    override fun renameItem(
        context: Context,
        uri: String,
        newName: String
    ): IntentSender? {
        throw NotImplementedError("Immich does not have this functionality.")
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun renameDirectory(
        context: Context,
        path: String,
        newName: String
    ) {
        albumsClient.rename(
            id = Uuid.parse(path),
            newName = newName,
            accessToken = accessToken
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun moveItems(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        origin: String,
        originType: KClass<out AlbumType>,
        destination: String,
        preserveDate: Boolean,
        onItemDone: (totalCount: Int) -> Unit
    ): Boolean {
        val assetIds = copyItems(
            context = context,
            list = list,
            origin = origin,
            originType = originType,
            destination = destination,
            preserveDate = preserveDate,
            overrideDisplayName = null,
            onItemDone = onItemDone
        ).fastMap { Uuid.parse(it) }

        return albumsClient.removeAssets(
            albumId = Uuid.parse(origin),
            assetIds = assetIds,
            accessToken = accessToken
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun copyItems(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        origin: String,
        originType: KClass<out AlbumType>,
        destination: String,
        preserveDate: Boolean,
        overrideDisplayName: ((displayName: String) -> String)?,
        onItemDone: (totalCount: Int) -> Unit
    ): List<String> = withContext(Dispatchers.IO) {
        return@withContext when (originType) {
            AlbumType.Folder::class -> {
                copyToLocal(context, list, origin, destination, preserveDate, overrideDisplayName, onItemDone)
            }

            AlbumType.Custom::class -> {
                val ids = list.fastMap { it.id }
                val mediaItems = customDao.getMediaInAlbum(album = origin).filter {
                    it.id in ids
                }
                val copied = copyToCustom(list, destination, onItemDone)

                mediaItems.mapNotNull { media ->
                    media.immichId?.takeIf {
                        media.id.toString() in copied
                    }
                }
            }

            AlbumType.Cloud::class -> {
                copyToCloud(context, list, destination, onItemDone)
            }

            else -> {
                emptyList()
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun copyToLocal(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        origin: String,
        destination: String,
        preserveDate: Boolean,
        overrideDisplayName: ((displayName: String) -> String)?,
        onItemDone: (totalCount: Int) -> Unit
    ): List<String> = withContext(Dispatchers.IO) {
        val ids = list.fastMap { it.id }
        val mediaItems = customDao.getMediaInAlbum(album = origin).filter {
            it.id in ids
        }

        return@withContext list.mapIndexedNotNull { index, item ->
            val media = mediaItems.first { it.id == item.id }
            val uuid = media.immichId!!
            val bytes = assetClient.download(
                id = Uuid.parse(uuid),
                accessToken = accessToken
            )

            if (bytes == null) return@mapIndexedNotNull null

            onItemDone(index + 1)

            File(destination, media.displayName)
                .outputStream()
                .buffered()
                .write(bytes)

            media.immichId
        }
    }
}