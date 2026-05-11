package com.kaii.photos.file_management.managers

import android.app.PendingIntent
import android.content.Context
import android.content.IntentSender
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.daos.SyncTaskDao
import com.kaii.photos.database.sync.CloudSyncWorker
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.grid_management.SelectionManager
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient

class HybridFileManager(
    private val isCustom: Boolean,
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

    override suspend fun share(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ) {
        val immich = list.filter { it.isCloud }
        val total = (list - immich.toSet()).toMutableList()

        if (immich.isNotEmpty()) {
            total += cloudFileManager.getShareItems(context, immich)
        }

        otherFileManager.share(context, total)
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

        val otherSuccess = otherFileManager.setTrashed(context, list, trashed, albumId.takeIf { isCustom }, taskId, onItemDone)
        val cloudSuccess = cloudFileManager.setTrashed(context, immich, trashed, albumId, taskId, onItemDone)

        return otherSuccess && cloudSuccess
    }

    override suspend fun permanentlyDelete(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        taskId: Int?
    ): Boolean {
        val immich = list.filter { it.immichUrl != null }

        otherFileManager.permanentlyDelete(context, list, taskId)
        return cloudFileManager.permanentlyDelete(context, immich, taskId)
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
        val otherSuccess = otherFileManager.moveItems(context, list, destination, preserveDate, taskId, origin.takeIf { isCustom }, onItemDone)

        var immichSuccess = true
        if (destination.immichId != null) {
            val immich = list.filter { it.isCloud }
            immichSuccess = cloudFileManager.copyItems(context, immich, destination, preserveDate, null, taskId, onItemDone).size == immich.size

            if (origin?.immichId != null && immichSuccess) {
                immichSuccess = cloudFileManager.setTrashed(
                    context = context,
                    list = immich,
                    trashed = true,
                    albumId = origin.immichId,
                    taskId = null,
                    onItemDone = {}
                )
            }
        }

        if (destination.immichId != null || origin?.immichId != null) {
            CloudSyncWorker.immediateEnqueue(context = context, albumId = destination.immichId!!)
        }

        return otherSuccess && immichSuccess
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

        val otherResult = otherFileManager.copyItems(context, local, destination, preserveDate, overrideDisplayName, taskId, onItemDone)
        val immichResult = cloudFileManager.copyItems(context, immich, destination, preserveDate, null, taskId, onItemDone)

        if (destination.immichId != null) {
            CloudSyncWorker.immediateEnqueue(context = context, albumId = destination.immichId!!)
        }

        return otherResult + immichResult
    }
}