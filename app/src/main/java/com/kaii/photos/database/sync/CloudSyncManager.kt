package com.kaii.photos.database.sync

import android.content.Context
import com.kaii.photos.database.daos.SyncTaskDao
import com.kaii.photos.database.entities.SyncTask
import com.kaii.photos.database.entities.SyncTaskType
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.di.appModule
import com.kaii.photos.file_management.managers.CloudFileManager
import com.kaii.photos.file_management.managers.CustomFileManager
import com.kaii.photos.file_management.managers.LocalFileManager
import com.kaii.photos.file_management.sync.CloudCleanupHandler
import com.kaii.photos.file_management.sync.CustomSyncHandler
import com.kaii.photos.file_management.sync.LocalSyncHandler
import com.kaii.photos.file_management.sync.ProgressManager
import com.kaii.photos.helpers.grid_management.SelectionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class CloudSyncManager(
    private val taskDao: SyncTaskDao,
    private val context: Context,
    private val cloudFileManager: CloudFileManager,
    private val localFileManager: LocalFileManager,
    customFileManager: CustomFileManager
) {
    private val progressManager = context.appModule.cloudProgressManager

    private val localSyncHandler = LocalSyncHandler(
        fileManager = localFileManager,
        progressManager = progressManager,
        albums = context.appModule.settings.albums
    )
    private val customSyncHandler = CustomSyncHandler(
        fileManager = customFileManager,
        progressManager = progressManager,
        albums = context.appModule.settings.albums
    )
    private val cloudCleanupHandler = CloudCleanupHandler(
        mediaDao = cloudFileManager.mediaDao,
        assetsClient = cloudFileManager.assetClient
    )

    suspend fun syncUploads() {
        val unsynced = taskDao.getUnsyncedTasks()

        val items = unsynced.associateWith { task ->
            taskDao.getTaskItems(taskId = task.id)
        }

        progressManager.startTracking(
            totalItems = items.values.flatten().size
        )

        items.forEach { (task, items) ->
            when (task.type) {
                SyncTaskType.Upload -> uploadTask(task, items)
                SyncTaskType.Trash -> trashTask(task, items)
                SyncTaskType.Favourite -> favouriteTask(task, items)
                SyncTaskType.Delete -> deleteTask(task, items)
                SyncTaskType.RenameAlbum -> renameAlbumTask(task)
                SyncTaskType.Copy -> copyTask(task, items)
            }
        }

        val albums = localSyncHandler.fetchCloudAlbums()

        if (progressManager.state == ProgressManager.State.Idle) {
            progressManager.startTracking(totalItems = 0)
        }

        albums.forEach { album ->
            if (album is AlbumType.Custom) {
                customSyncHandler.sync(context, album)
            } else if (album is AlbumType.Folder ){
                localSyncHandler.sync(context, album)
            }
        }

        progressManager.stopTracking()

        cloudCleanupHandler.cleanUp(context)
    }

    suspend fun syncFor(
        albumId: String
    ) {
        val albums = localSyncHandler.fetchCloudAlbums()

        val album = albums.find { it.id == albumId } ?: return

        if (progressManager.state == ProgressManager.State.Idle) {
            progressManager.startTracking(totalItems = 0)
        }

        if (album is AlbumType.Custom) {
            customSyncHandler.sync(context, album)
        } else if (album is AlbumType.Folder ){
            localSyncHandler.sync(context, album)
        }

        progressManager.stopTracking()
    }

    private suspend fun uploadTask(
        task: SyncTask,
        items: List<SelectionManager.SelectedItem>
    ) {
        localFileManager.copyToCloud(
            context = context,
            list = items,
            destination =
                AlbumType.Cloud(
                    id = task.destination!!,
                    name = "",
                    pinned = false
                ),
            taskId = task.id,
            onItemDone = {
                progressManager.increaseProgress()
            }
        )
    }

    private suspend fun trashTask(
        task: SyncTask,
        items: List<SelectionManager.SelectedItem>
    ) {
        cloudFileManager.setTrashed(
            context = context,
            list = items,
            trashed = true,
            albumId = task.destination,
            immichId = task.destination,
            taskId = task.id,
            onItemDone = {
                progressManager.increaseProgress()
            }
        )
    }

    private suspend fun favouriteTask(
        task: SyncTask,
        items: List<SelectionManager.SelectedItem>
    ) {
        cloudFileManager.setFavourite(
            context = context,
            favourite = task.destination!!.toBoolean(),
            list = items,
            taskId = task.id
        )
    }

    private suspend fun deleteTask(
        task: SyncTask,
        items: List<SelectionManager.SelectedItem>
    ) {
        cloudFileManager.permanentlyDelete(
            context = context,
            list = items,
            taskId = task.id
        ).let {
            if (it) {
                progressManager.increaseProgressBy(amount = items.size)
            }
        }
    }

    private suspend fun renameAlbumTask(
        task: SyncTask
    ) = withContext(Dispatchers.IO) {
        val album = context.appModule.settings.albums
            .get()
            .first()
            .first { it.id == task.destination }

        cloudFileManager.renameAlbum(
            context = context,
            album = album,
            newName = task.extraData!!,
            taskId = task.id
        )
    }

    private suspend fun copyTask(
        task: SyncTask,
        items: List<SelectionManager.SelectedItem>
    ) = withContext(Dispatchers.IO) {
        val album = context.appModule.settings.albums
            .get()
            .first()
            .first { it.id == task.destination }

        cloudFileManager.copyToCloud(
            context = context,
            list = items,
            destination = album as AlbumType.Cloud,
            taskId = task.id,
            onItemDone = {
                progressManager.increaseProgress()
            }
        )
    }
}