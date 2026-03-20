package com.kaii.photos.helpers.file_management

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.provider.MediaStore
import android.util.Log
import androidx.compose.ui.util.fastMap
import androidx.core.net.toUri
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.daos.SyncTaskDao
import com.kaii.photos.database.entities.CustomItem
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.di.appModule
import com.kaii.photos.helpers.grid_management.SelectionManager
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.uuid.ExperimentalUuidApi

class CustomFileManager(
    override val mediaDao: MediaDao,
    override val customDao: CustomEntityDao,
    override val syncTaskDao: SyncTaskDao,
    override val assetClient: AssetsClient,
    override val albumsClient: AlbumsClient,
    override val accessToken: String,
    override val endpoint: String
) : GenericFileManager {
    companion object {
        private const val TAG = "com.kaii.photos.helpers.file_management.CustomFileManager"
    }

    override suspend fun setFavourite(
        context: Context,
        favourite: Boolean,
        list: List<String>
    ) = withContext(Dispatchers.IO) {
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
        }
    }

    override suspend fun setTrashed(
        context: Context,
        list: List<String>,
        trashed: Boolean,
        albumId: String?,
        onItemDone: (totaCount: Int) -> Unit
    ) {
        if (!trashed) {
            throw IllegalArgumentException("${CustomFileManager::class.simpleName} cannot restore an item! This should never happen!")
        }

        customDao.deleteAll(
            ids = list.fastMap { it.toLong() }.toSet(),
            album = albumId!!
        )

        onItemDone(list.size)
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
        newName: String
    ) {
        val settings = context.applicationContext.appModule.settings.albums

        settings.edit(
            id = album.id,
            newInfo = (album as AlbumType.Custom).copy(name = newName)
        )
    }

    /** @param destination id of the destination album */
    override suspend fun moveItems(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: AlbumType,
        preserveDate: Boolean,
        onItemDone: (uri: String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        if (destination !is AlbumType.Custom) {
            throw IllegalArgumentException("Cannot move items between ${AlbumType.Custom::class.simpleName} and ${destination::class.simpleName}")
        }

        customDao.upsertAll(
            items = list.fastMap {
                CustomItem(
                    id = it.id,
                    album = destination.id
                )
            }
        )

        list.groupBy {
            it.parentPath
        }.forEach { (albumId, items) ->
            customDao.deleteAll(
                ids = items.fastMap { it.id }.toSet(),
                album = albumId
            )
        }

        list.forEach { onItemDone(it.uri) }

        true
    }

    /** @param overrideDisplayName should not contain file extension */
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun copyItems(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: AlbumType,
        preserveDate: Boolean,
        overrideDisplayName: ((displayName: String) -> String)?,
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
                copyToCloud(context, list, destination, onItemDone)
            }

            else -> {
                emptyList()
            }
        }
    }
}