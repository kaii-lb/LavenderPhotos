package com.kaii.photos.helpers.file_management

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.compose.ui.util.fastMap
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.SyncTaskDao
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.helpers.EXTERNAL_DOCUMENTS_AUTHORITY
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.toBasePath
import com.kaii.photos.mediastore.copyUriToUri
import com.kaii.photos.mediastore.getMediaStoreDataForIds
import com.kaii.photos.mediastore.getPathsFromUriList
import com.kaii.photos.mediastore.getTrashPathsFromUriList
import com.kaii.photos.mediastore.insertMedia
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.reflect.KClass
import kotlin.uuid.ExperimentalUuidApi

class LocalFileManager(
    override val customDao: CustomEntityDao,
    override val syncTaskDao: SyncTaskDao,
    override val assetClient: AssetsClient,
    override val albumsClient: AlbumsClient,
    override val accessToken: String
) : GenericFileManager {
    companion object {
        private const val TAG = "com.kaii.photos.helpers.file_management.LocalFileManager"
    }

    override suspend fun setFavourite(
        context: Context,
        favourite: Boolean,
        list: List<String>,
        onItemDone: (totaCount: Int) -> Unit
    ) {
        if (list.isNotEmpty()) {
            val favRequest = MediaStore.createFavoriteRequest(
                context.contentResolver,
                list.fastMap { it.toUri() },
                favourite
            )

            (context as Activity).startIntentSenderForResult(
                favRequest.intentSender,
                9998,
                null,
                0,
                0,
                0
            )

            onItemDone(list.size)
        }
    }

    override suspend fun setTrashed(
        context: Context,
        list: List<String>,
        trashed: Boolean,
        onItemDone: (totaCount: Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver

        val currentTimeMillis = System.currentTimeMillis()
        val trashedValues = ContentValues().apply {
            put(MediaStore.MediaColumns.IS_TRASHED, trashed)
            put(MediaStore.MediaColumns.DATE_MODIFIED, currentTimeMillis)
        }

        try {
            setFavourite(
                context = context,
                favourite = false,
                list = list,
                onItemDone = {}
            )
        } catch (e: Throwable) {
            Log.d(TAG, "Failed setting fav on trash list. ${e.message}")
            e.printStackTrace()
        }

        try {
            val list = list.fastMap { it.toUri() }
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
        } catch (e: Throwable) {
            Log.e(TAG, "Setting trashed $trashed on photo list failed.")
            e.printStackTrace()
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

    /** @param path should be of the form $basePath:relativePath */
    override suspend fun renameDirectory(
        context: Context,
        path: String,
        newName: String
    ) {
        try {
            val dir =
                DocumentsContract.buildTreeDocumentUri(
                    EXTERNAL_DOCUMENTS_AUTHORITY,
                    path
                )

            Log.d(TAG, "Dir is $dir")

            val newDirectory = DocumentFile.fromTreeUri(context, dir)
            newDirectory?.renameTo(newName)
        } catch (e: Throwable) {
            Log.e(TAG, "Couldn't rename directory $path to $newName")
            e.printStackTrace()
        }
    }

    override suspend fun moveItems(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        origin: String,
        originType: KClass<out AlbumType>,
        destination: String,
        preserveDate: Boolean,
        onItemDone: (totalCount: Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver

        val items = getMediaStoreDataForIds(
            ids = list.fastMap { it.id }.toSet(),
            context = context
        )

        var count = 1
        items.forEachIndexed { index, media ->
            contentResolver.insertMedia(
                context = context,
                media = media,
                destination = destination,
                basePath = media.absolutePath.toBasePath(),
                currentVolumes = MediaStore.getExternalVolumeNames(context),
                preserveDate = preserveDate,
                onInsert = { original, new ->
                    contentResolver.copyUriToUri(original, new)
                }
            )?.let {
                count += index
                onItemDone(count)
                contentResolver.delete(media.uri.toUri(), null)
            }
        }

        return@withContext count == items.size
    }

    /** @param overrideDisplayName should not contain file extension */
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun copyItems(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        origin: String,
        originType: KClass<out AlbumType>,
        destination: String,
        preserveDate: Boolean,
        overrideDisplayName: ((displayName: String) -> String)?,
        onItemDone: (totaCount: Int) -> Unit
    ): List<GenericFileManager.CopyResult> = withContext(Dispatchers.IO) {
        when (originType) {
            AlbumType.Folder::class -> {
                copyToLocal(context, list, origin, destination, preserveDate, overrideDisplayName, onItemDone)
            }

            AlbumType.Custom::class -> {
                copyToCustom(context, list, origin, destination, onItemDone)
            }

            AlbumType.Custom::class -> {
                copyToCloud(context, list, destination, onItemDone)
            }

            else -> {
                emptyList()
            }
        }
    }
}