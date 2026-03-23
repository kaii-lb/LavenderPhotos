package com.kaii.photos.helpers.file_management

import android.app.PendingIntent
import android.content.Context
import android.content.IntentSender
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.daos.SyncTaskDao
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.grid_management.SelectionManager
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient

class HybridFileManager(
    override val mediaDao: MediaDao,
    override val customDao: CustomEntityDao,
    override val syncTaskDao: SyncTaskDao,
    override val assetClient: AssetsClient,
    override val albumsClient: AlbumsClient,
    override val info: ImmichBasicInfo
) : GenericFileManager {
    private val cloudFileManager = CloudFileManager(
        mediaDao = mediaDao,
        customDao = customDao,
        syncTaskDao = syncTaskDao,
        assetClient = assetClient,
        albumsClient = albumsClient,
        info = info
    )

    private val localFileManager = LocalFileManager(
        mediaDao = mediaDao,
        customDao = customDao,
        syncTaskDao = syncTaskDao,
        assetClient = assetClient,
        albumsClient = albumsClient,
        info = info
    )

    override suspend fun setFavourite(
        context: Context,
        favourite: Boolean,
        list: List<SelectionManager.SelectedItem>
    ): PendingIntent? {
        val immich = list.filter { it.isCloud }
        val local = list - immich.toSet()

        cloudFileManager.setFavourite(context, favourite, immich)
        return localFileManager.setFavourite(context, favourite, local)
    }

    override suspend fun setTrashed(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        trashed: Boolean,
        albumId: String?,
        onItemDone: (totaCount: Int) -> Unit
    ) {
        val immich = list.filter { it.isCloud }
        val local = list - immich.toSet()

        cloudFileManager.setTrashed(context, immich, trashed, albumId, onItemDone)
        localFileManager.setTrashed(context, local, trashed, albumId, onItemDone)
    }

    override fun renameItem(
        context: Context,
        uri: String,
        newName: String
    ): IntentSender? {
        if (uri.startsWith("http")) {
            throw IllegalArgumentException("Cannot rename immich media!")
        }

        return localFileManager.renameItem(context, uri, newName)
    }

    override suspend fun renameAlbum(
        context: Context,
        album: AlbumType,
        newName: String
    ) {
        throw NotImplementedError(
            "Despite the name, there is not one scenario in which this should be called from main pages. " +
                    "Do not try to rename albums from any multi-album view."
        )
    }

    override suspend fun moveItems(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: AlbumType,
        preserveDate: Boolean,
        onItemDone: (uri: String) -> Unit
    ): Boolean {
        val immich = list.filter { it.isCloud }
        val local = list - immich.toSet()

        return cloudFileManager.copyItems(context, immich, destination, preserveDate, null, onItemDone).size == immich.size &&
                localFileManager.moveItems(context, local, destination, preserveDate, onItemDone)
    }

    override suspend fun copyItems(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: AlbumType,
        preserveDate: Boolean,
        overrideDisplayName: ((displayName: String) -> String)?,
        onItemDone: (uri: String) -> Unit
    ): List<GenericFileManager.CopyResult> {
        val immich = list.filter { it.isCloud }
        val local = list - immich.toSet()

        return cloudFileManager.copyItems(context, immich, destination, preserveDate, overrideDisplayName, onItemDone) +
                localFileManager.copyItems(context, local, destination, preserveDate, overrideDisplayName, onItemDone)
    }
}