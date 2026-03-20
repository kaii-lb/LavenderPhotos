package com.kaii.photos.database.sync

import android.content.Context
import androidx.compose.ui.util.fastMap
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.daos.SyncTaskDao
import com.kaii.photos.database.entities.SyncTask
import com.kaii.photos.database.entities.SyncTaskType
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.helpers.file_management.CloudFileManager
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.mediastore.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class CloudSyncManager(
    private val taskDao: SyncTaskDao,
    private val mediaDao: MediaDao,
    private val context: Context,
    private val fileManager: CloudFileManager
) {
    suspend fun syncUploads() {
        val unsynced = taskDao.getUnsyncedTasks()

        unsynced.forEach { task ->
            when (task.type) {
                SyncTaskType.Upload -> uploadTask(task = task)

                SyncTaskType.Delete -> deleteOrRestoreTask(task = task)

                SyncTaskType.Restore -> deleteOrRestoreTask(task = task)

                SyncTaskType.Update -> TODO()
            }
        }
    }

    private suspend fun uploadTask(task: SyncTask) = withContext(Dispatchers.IO) {
        val items = mediaDao.getAllMediaDateTaken()
            .first()
            .filter {
                it.id.toString() in task.itemIds
            }
            .fastMap {
                SelectionManager.SelectedItem(
                    id = it.id,
                    uri = it.uri,
                    isImage = it.type == MediaType.Image,
                    parentPath = it.parentPath
                )
            }

        fileManager.copyToCloud(
            context = context,
            list = items,
            destination =
                AlbumType.Cloud(
                    id = task.destination!!,
                    name = "",
                    pinned = false
                ),
            onItemDone = {
                // TODO: update progress indicator around pfp?
            }
        )
    }

    private suspend fun deleteOrRestoreTask(task: SyncTask) = withContext(Dispatchers.IO) {
        fileManager.setTrashed(
            context = context,
            list = task.itemIds,
            trashed = task.type == SyncTaskType.Delete,
            albumId = task.destination,
            onItemDone = {
                // TODO: update progress indicator around pfp?
            }
        )
    }
}