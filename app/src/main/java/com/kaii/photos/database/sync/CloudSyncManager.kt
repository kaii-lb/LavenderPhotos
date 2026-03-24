package com.kaii.photos.database.sync

import android.content.Context
import com.kaii.photos.database.daos.SyncTaskDao
import com.kaii.photos.database.entities.SyncTask
import com.kaii.photos.database.entities.SyncTaskType
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.di.appModule
import com.kaii.photos.helpers.file_management.CloudFileManager
import com.kaii.photos.helpers.file_management.LocalFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class CloudSyncManager(
    private val taskDao: SyncTaskDao,
    private val context: Context,
    private val cloudFileManager: CloudFileManager,
    private val localFileManager: LocalFileManager
) {
    suspend fun syncUploads() {
        val unsynced = taskDao.getUnsyncedTasks()

        unsynced.forEach { task ->
            println("TASK $task")

            when (task.type) {
                SyncTaskType.Upload -> uploadTask(task = task)
                SyncTaskType.Trash -> trashTask(task = task)
                SyncTaskType.Favourite -> favouriteTask(task = task)
                SyncTaskType.Delete -> deleteTask(task = task)
                SyncTaskType.RenameAlbum -> renameAlbumTask(task = task)
                SyncTaskType.Move -> moveTask(task = task)
                SyncTaskType.Copy -> copyTask(task = task)
            }
        }
    }

    private suspend fun uploadTask(task: SyncTask) =
        localFileManager.copyToCloud(
            context = context,
            list = task.items,
            destination =
                AlbumType.Cloud(
                    id = task.destination!!,
                    name = "",
                    pinned = false
                ),
            taskId = task.id,
            onItemDone = {
                // TODO: update progress indicator around pfp?
            }
        ).size == task.items.size

    private suspend fun trashTask(task: SyncTask) =
        cloudFileManager.setTrashed(
            context = context,
            list = task.items,
            trashed = true,
            albumId = task.destination,
            taskId = task.id,
            onItemDone = {
                // TODO: update progress indicator around pfp?
            }
        )

    private suspend fun favouriteTask(task: SyncTask) =
        cloudFileManager.setFavourite(
            context = context,
            favourite = task.destination!!.toBoolean(),
            list = task.items,
            taskId = task.id
        )

    private suspend fun deleteTask(task: SyncTask) =
        cloudFileManager.permanentlyDelete(
            context = context,
            list = task.items,
            taskId = task.id
        )

    private suspend fun renameAlbumTask(task: SyncTask) = withContext(Dispatchers.IO) {
        val album = context.appModule.settings.albums
            .get()
            .first()
            .first { it.id == task.destination }

        cloudFileManager.renameAlbum(
            context = context,
            album = album,
            newName = task.items.first().uri,
            taskId = task.id
        )
    }

    private suspend fun moveTask(task: SyncTask) = withContext(Dispatchers.IO) {
        val album = context.appModule.settings.albums
            .get()
            .first()
            .first { it.id == task.destination }

        cloudFileManager.moveItems(
            context = context,
            list = task.items,
            destination = album,
            preserveDate = true,
            taskId = task.id,
            onItemDone = {
                // TODO: update progress indicator around pfp?
            }
        )
    }

    private suspend fun copyTask(task: SyncTask) = withContext(Dispatchers.IO) {
        val album = context.appModule.settings.albums
            .get()
            .first()
            .first { it.id == task.destination }

        cloudFileManager.copyToCloud(
            context = context,
            list = task.items,
            destination = album as AlbumType.Cloud,
            taskId = task.id,
            onItemDone = {
                // TODO: update progress indicator around pfp?
            }
        )
    }
}