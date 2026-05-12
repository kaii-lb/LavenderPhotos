package com.kaii.photos.file_management.managers

import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import androidx.compose.ui.util.fastMap
import androidx.core.net.toUri
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.daos.SyncTaskDao
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.mediastore.getPathsFromUriList
import com.kaii.photos.mediastore.getTrashPathsFromUriList
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.uuid.ExperimentalUuidApi

class LocalFileManager(
    override val mediaDao: MediaDao,
    override val customDao: CustomEntityDao,
    override val syncTaskDao: SyncTaskDao,
    override val assetClient: AssetsClient,
    override val albumsClient: AlbumsClient
) : GenericFileManager {
    companion object {
        private val TAG = LocalFileManager::class.qualifiedName
    }

    override suspend fun setTrashed(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        trashed: Boolean,
        albumId: String?,
        taskId: Int?,
        onItemDone: (totaCount: Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (list.isEmpty()) return@withContext true

        if (albumId != null) {
            throw IllegalArgumentException("${LocalFileManager::class.simpleName} should not and does not handle per album media deletion!")
        }

        val contentResolver = context.contentResolver

        val currentTimeMillis = System.currentTimeMillis()
        val trashedValues = ContentValues().apply {
            put(MediaStore.MediaColumns.IS_TRASHED, trashed)
            put(MediaStore.MediaColumns.DATE_MODIFIED, currentTimeMillis)
        }

        return@withContext try {
            val list = list.fastMap { it.uri.toUri() }
            val map =
                if (trashed) context.contentResolver.getPathsFromUriList(list = list).toMap()
                else context.contentResolver.getTrashPathsFromUriList(list = list).toMap()

            list.forEachIndexed { index, uri ->
                // order is very important!
                // this WILL crash if you try to set last modified on a file that got moved from ex image.png to .trashed-{timestamp}-image.png
                File(map[uri]!!).setLastModified(currentTimeMillis)
                contentResolver.update(uri, trashedValues, null)

                onItemDone(index + 1)
            }

            true
        } catch (securityException: RecoverableSecurityException) {
            val intentSender = securityException.userAction.actionIntent.intentSender
            context.startIntentSender(intentSender, null, 0, 0, 0)

            true
        } catch (e: Throwable) {
            Log.e(TAG, "Setting trashed $trashed on photo list failed.")
            e.printStackTrace()

            false
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
    ): Boolean = withContext(Dispatchers.IO) {
        if (list.isEmpty()) return@withContext true

        if (origin != null) {
            throw IllegalArgumentException("${LocalFileManager::class.simpleName} cannot move with a given origin! It should always be null.")
        }

        if (destination !is AlbumType.Folder) {
            throw IllegalArgumentException("Cannot move items between ${AlbumType.Folder::class.simpleName} and ${destination::class.simpleName}")
        }

        var count = 0
        val toBeDeleted = mutableListOf<SelectionManager.SelectedItem>()

        copyItems(
            context = context,
            list = list,
            destination = destination,
            preserveDate = preserveDate,
            overrideDisplayName = null,
            taskId = taskId,
            onItemDone = { uri ->
                val item = list.first { it.uri == uri }
                if (!toBeDeleted.contains(item)) {
                    toBeDeleted.add(item)
                }

                count += 1
                onItemDone(uri)
            }
        )

        permanentlyDelete(
            context = context,
            list = toBeDeleted
        )

        return@withContext count == list.size
    }

    /** @param overrideDisplayName should not contain file extension */
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun copyItems(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: AlbumType,
        preserveDate: Boolean,
        overrideDisplayName: ((displayName: String) -> String)?,
        taskId: Int?,
        onItemDone: (uri: String) -> Unit
    ): List<GenericFileManager.CopyResult> = withContext(Dispatchers.IO) {
        if (list.isEmpty()) return@withContext emptyList()

        when (destination) {
            is AlbumType.Folder -> {
                copyToLocal(context, list, destination, preserveDate, overrideDisplayName, onItemDone)
            }

            is AlbumType.Custom -> {
                copyToCustom(context, list, destination, onItemDone)
            }

            is AlbumType.Cloud -> {
                copyToCloud(context, list, destination, taskId, onItemDone)
            }

            else -> {
                emptyList()
            }
        }
    }
}