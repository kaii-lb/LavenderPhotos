package com.kaii.photos.helpers.file_management

import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.compose.ui.util.fastMap
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.daos.SyncTaskDao
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.di.appModule
import com.kaii.photos.helpers.EXTERNAL_DOCUMENTS_AUTHORITY
import com.kaii.photos.helpers.baseInternalStorageDirectory
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.toBasePath
import com.kaii.photos.helpers.toRelativePath
import com.kaii.photos.mediastore.getExternalStorageContentUriFromAbsolutePath
import com.kaii.photos.mediastore.getPathsFromUriList
import com.kaii.photos.mediastore.getTrashPathsFromUriList
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.uuid.ExperimentalUuidApi

class LocalFileManager(
    override val mediaDao: MediaDao,
    override val customDao: CustomEntityDao,
    override val syncTaskDao: SyncTaskDao,
    override val assetClient: AssetsClient,
    override val albumsClient: AlbumsClient,
    override val info: ImmichBasicInfo
) : GenericFileManager {
    companion object {
        private const val TAG = "com.kaii.photos.helpers.file_management.LocalFileManager"
    }

    override suspend fun setTrashed(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        trashed: Boolean,
        albumId: String?,
        taskId: Int?,
        onItemDone: (totaCount: Int) -> Unit
    ) = withContext(Dispatchers.IO) {
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
        } catch (e: Throwable) {
            Log.e(TAG, "Setting trashed $trashed on photo list failed.")
            e.printStackTrace()

            false
        }
    }

    /** returns null if the operation succeeded, otherwise lets the caller handle the [RecoverableSecurityException] */
    override fun renameItem(
        context: Context,
        uri: String,
        newName: String
    ): IntentSender? {
        val contentResolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, newName)
        }

        try {
            contentResolver.update(uri.toUri(), contentValues, null)
            contentResolver.notifyChange(uri.toUri(), null)

            return null
        } catch (securityException: SecurityException) {
            Log.e(TAG, securityException.toString())
            securityException.printStackTrace()

            val recoverableSecurityException =
                securityException as? RecoverableSecurityException ?: throw RuntimeException(securityException.message, securityException)

            val intentSender = recoverableSecurityException.userAction.actionIntent.intentSender
            return intentSender
        }
    }

    override suspend fun renameAlbum(
        context: Context,
        album: AlbumType,
        newName: String,
        taskId: Int?
    ): Unit = withContext(Dispatchers.IO) {
        if (album is AlbumType.Folder && album.paths.size == 1) {
            val basePath = album.paths.first().toBasePath()
            val currentVolumes = MediaStore.getExternalVolumeNames(context)
            val volumeName =
                if (basePath == baseInternalStorageDirectory) "primary"
                else currentVolumes.find {
                    val possible =
                        basePath.replace("/storage/", "").removeSuffix("/")
                    it == possible || it == possible.lowercase()
                }

            val relativePath = album.paths.first().toRelativePath(true)
            try {
                val dir =
                    DocumentsContract.buildTreeDocumentUri(
                        EXTERNAL_DOCUMENTS_AUTHORITY,
                        "$volumeName:$relativePath"
                    )

                Log.d(TAG, "Dir is $dir")

                val newDirectory = DocumentFile.fromTreeUri(context, dir)
                newDirectory?.renameTo(newName)
            } catch (e: Throwable) {
                Log.e(TAG, "Couldn't rename directory $volumeName:$relativePath to $newName")
                e.printStackTrace()
            }

            val newInfo = album.copy(
                name = newName,
                paths = setOf(album.paths.first().replace(album.name, newName))
            )

            val albums = context.appModule.settings.albums.get().first()
            albums.filterIsInstance<AlbumType.Folder>().filter { child ->
                child.paths.any { it.startsWith(album.paths.first()) }
            }.forEach { child ->
                context.appModule.settings.albums.edit(
                    child.id,
                    child.copy(
                        paths = child.paths.map {
                            if (it.startsWith(album.paths.first())) it.replace(album.paths.first(), newInfo.paths.first())
                            else it
                        }.toSet()
                    )
                )
            }

            context.appModule.settings.albums.edit(album.id, newInfo)

            try {
                context.contentResolver.releasePersistableUriPermission(
                    context.getExternalStorageContentUriFromAbsolutePath(
                        absolutePath = newInfo.paths.first(),
                        trimDoc = true
                    ),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Throwable) {
                Log.d(TAG, "Couldn't release permission for ${newInfo.paths.first()}")
                e.printStackTrace()
            }
        } else {
            context.appModule.settings.albums.edit(
                id = album.id,
                newInfo = (album as AlbumType.Folder).copy(name = newName)
            )
        }
    }

    override suspend fun moveItems(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: AlbumType,
        preserveDate: Boolean,
        taskId: Int?,
        onItemDone: (uri: String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        if (list.isEmpty()) return@withContext true

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