package com.kaii.photos.file_management.managers

import android.app.RecoverableSecurityException
import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.compose.ui.util.fastMap
import androidx.core.net.toUri
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.daos.SyncTaskDao
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.file_management.secure.LocalSecureManager
import com.kaii.photos.helpers.grid_management.SelectionManager
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.uuid.ExperimentalUuidApi


class LocalFileManager(
    override val mediaDao: MediaDao,
    override val customDao: CustomEntityDao,
    override val syncTaskDao: SyncTaskDao,
    override val assetClient: AssetsClient,
    override val albumsClient: AlbumsClient,
    private val secureManager: LocalSecureManager
) : GenericFileManager {
    companion object {
        private val TAG = LocalFileManager::class.qualifiedName
    }

    override suspend fun setTrashed(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        trashed: Boolean,
        albumId: String?,
        immichId: String?,
        taskId: Int?,
        onItemDone: (totaCount: Int) -> Unit
    ): Result<Unit, FileOperationError> = withContext(Dispatchers.IO) {
        if (list.isEmpty()) return@withContext Result.Success(Unit)

        if (albumId != null) {
            throw IllegalArgumentException("${LocalFileManager::class.simpleName} should not and does not handle per album media deletion!")
        }

        val contentResolver = context.contentResolver

        return@withContext try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                MediaStore.canManageMedia(context)
            ) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_TRASHED, trashed)
                }

                list.chunked(500) { chunk ->
                    val operations = ArrayList<ContentProviderOperation>()
                    chunk.forEach { item ->
                        val operation = ContentProviderOperation.newUpdate(item.uri.toUri())
                            .withValues(contentValues)
                            .build()

                        operations.add(operation)
                    }

                    contentResolver.applyBatch(MediaStore.AUTHORITY, operations)
                }

                onItemDone(list.size)

                Result.Success(Unit)
            } else {
                val mediaUris = list.fastMap { it.uri.toUri() }
                val pendingIntent = MediaStore.createTrashRequest(contentResolver, mediaUris, trashed)

                Result.Error(
                    FileOperationError.NeedsRequest(
                        pendingIntent = pendingIntent
                    )
                )
            }
        } catch (securityException: RecoverableSecurityException) {
            val intentSender = securityException.userAction.actionIntent.intentSender
            context.startIntentSender(intentSender, null, 0, 0, 0)

            Result.Error(
                FileOperationError.RecoverableException(
                    intentSender = intentSender
                )
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Setting trashed $trashed on photo list failed.")
            e.printStackTrace()

            Result.Error(FileOperationError.Failed)
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

        copyItems(
            context = context,
            list = list,
            destination = destination,
            preserveDate = preserveDate,
            overrideDisplayName = null,
            taskId = taskId,
            onItemDone = { uri ->
                count += 1
                onItemDone(uri)
            }
        )

        permanentlyDelete(
            context = context,
            list = list
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

    override suspend fun secure(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ) = permanentlyDelete(
        context = context,
        list = secureManager.secure(context, list)
    )

    override suspend fun restore(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ): Boolean {
        throw NotImplementedError("Cannot restore items outside secure folder")
    }
}