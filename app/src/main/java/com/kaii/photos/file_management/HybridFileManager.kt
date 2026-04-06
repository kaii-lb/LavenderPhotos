package com.kaii.photos.file_management

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
    isCustom: Boolean,
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

    private val otherFileManager =
        if (isCustom) {
            CustomFileManager(
                mediaDao = mediaDao,
                customDao = customDao,
                syncTaskDao = syncTaskDao,
                assetClient = assetClient,
                albumsClient = albumsClient,
                info = info
            )
        } else {
            LocalFileManager(
                mediaDao = mediaDao,
                customDao = customDao,
                syncTaskDao = syncTaskDao,
                assetClient = assetClient,
                albumsClient = albumsClient,
                info = info
            )
        }

    override suspend fun setFavourite(
        context: Context,
        favourite: Boolean,
        list: List<SelectionManager.SelectedItem>,
        taskId: Int?
    ): PendingIntent? {
        val immich = list.filter { it.immichUrl != null }
        val local = list.filter { !it.isCloud }

        val success = otherFileManager.setFavourite(context, favourite, local, taskId)
        cloudFileManager.setFavourite(context, favourite, immich, taskId)

        return success
    }

    override suspend fun setTrashed(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        trashed: Boolean,
        albumId: String?,
        taskId: Int?,
        onItemDone: (totaCount: Int) -> Unit
    ): Boolean {
        val immich = list.filter { it.immichUrl != null }
        val local = list.filter { !it.isCloud }

        val localSuccess = otherFileManager.setTrashed(context, local, trashed, albumId, taskId, onItemDone)
        val cloudSuccess = cloudFileManager.setTrashed(context, immich, trashed, albumId, taskId, onItemDone)

        return localSuccess && cloudSuccess
    }

    override suspend fun permanentlyDelete(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        taskId: Int?
    ) {
        val immich = list.filter { it.immichUrl != null }
        val local = list.filter { !it.isCloud }

        otherFileManager.permanentlyDelete(context, local, taskId)
        cloudFileManager.permanentlyDelete(context, immich, taskId)
    }

    override fun renameItem(
        context: Context,
        uri: String,
        newName: String
    ): IntentSender? {
        if (uri.startsWith("http")) {
            throw IllegalArgumentException("Cannot rename immich media!")
        }

        return otherFileManager.renameItem(context, uri, newName)
    }

    override suspend fun renameAlbum(
        context: Context,
        album: AlbumType,
        newName: String,
        taskId: Int?
    ) {
        if (album.immichId != null) {
            cloudFileManager.renameAlbum(
                context = context,
                album = album,
                newName = newName,
                taskId = taskId
            )
        } else {
            otherFileManager.renameAlbum(
                context = context,
                album = album,
                newName = newName,
                taskId = taskId
            )
        }
    }

    override suspend fun moveItems(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: AlbumType,
        preserveDate: Boolean,
        taskId: Int?,
        origin: AlbumType?,
        onItemDone: (uri: String) -> Unit
    ): Boolean {
        if (origin != null) {
            throw IllegalArgumentException("${HybridFileManager::class.simpleName} cannot move with a given origin! It should always be null.")
        }

        val immich = list.filter { it.isCloud }
        val local = list - immich.toSet()

        val localSuccess = otherFileManager.moveItems(context, local, destination, preserveDate, taskId, origin, onItemDone)
        val immichSuccess = cloudFileManager.copyItems(context, immich, destination, preserveDate, null, taskId, onItemDone).size == immich.size

        if (immichSuccess && destination.immichId != null) {
            cloudFileManager.setTrashed(
                context = context,
                list = immich,
                trashed = true,
                albumId = destination.immichId,
                taskId = null,
                onItemDone = {}
            )
        }

        return localSuccess && immichSuccess
    }

    override suspend fun copyItems(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: AlbumType,
        preserveDate: Boolean,
        overrideDisplayName: ((displayName: String) -> String)?,
        taskId: Int?,
        onItemDone: (uri: String) -> Unit
    ): List<GenericFileManager.CopyResult> {
        val immich = list.filter { it.isCloud }
        val local = list - immich.toSet()

        val localResult = otherFileManager.copyItems(context, local, destination, preserveDate, overrideDisplayName, taskId, onItemDone)
        val cloudResult = cloudFileManager.copyItems(context, immich, destination, preserveDate, overrideDisplayName, taskId, onItemDone)

        return localResult + cloudResult
    }
}