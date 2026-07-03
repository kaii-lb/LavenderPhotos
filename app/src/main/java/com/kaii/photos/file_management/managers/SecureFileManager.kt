package com.kaii.photos.file_management.managers

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import com.kaii.photos.R
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.daos.SecuredMediaItemEntityDao
import com.kaii.photos.database.daos.SyncTaskDao
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.file_management.secure.LocalSecureManager
import com.kaii.photos.helpers.EncryptionManager
import com.kaii.photos.helpers.appSecureFolderDir
import com.kaii.photos.helpers.getDecryptCacheForFile
import com.kaii.photos.helpers.getSecureDecryptedVideoFile
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.grid_management.toSecureMedia
import com.kaii.photos.helpers.secureThumbnailImage
import com.kaii.photos.mediastore.LAVENDER_FILE_PROVIDER_AUTHORITY
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.getIv
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import java.io.File

class SecureFileManager(
    private val secureDao: SecuredMediaItemEntityDao,
    override val mediaDao: MediaDao,
    override val customDao: CustomEntityDao,
    override val syncTaskDao: SyncTaskDao,
    override val assetClient: AssetsClient,
    override val albumsClient: AlbumsClient
) : GenericFileManager {
    companion object {
        private val TAG = SecureFileManager::class.qualifiedName
    }

    private val secureManager = LocalSecureManager(secureDao, mediaDao)

    override suspend fun setTrashed(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        trashed: Boolean,
        albumId: String?,
        immichId: String?,
        taskId: Int?,
        onItemDone: (totaCount: Int) -> Unit
    ): Boolean {
        throw NotImplementedError("Cannot trash items in secure folder")
    }

    override suspend fun secure(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ): Boolean {
        throw NotImplementedError("Cannot secure already secured items")
    }

    override suspend fun restore(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ) = secureManager.restore(context, list)

    override suspend fun moveItems(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: AlbumType,
        preserveDate: Boolean,
        taskId: Int?,
        origin: AlbumType?,
        onItemDone: (uri: String) -> Unit
    ): Boolean {
        throw NotImplementedError("Cannot move items in secure folder")
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
        throw NotImplementedError("Cannot copy items in secure folder")
    }

    override suspend fun share(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ) {
        val cachedPaths = emptyList<Pair<String, MediaType>>().toMutableList()
        val items = list.toSecureMedia(context = context)

        items.forEach { item ->
            val iv = item.bytes?.getIv() ?: return

            val originalFile = File(item.item.absolutePath)
            val cachedFile =
                if (item.item.type == MediaType.Video) {
                    getSecureDecryptedVideoFile(originalFile.name, context)
                } else {
                    getDecryptCacheForFile(originalFile, context)
                }

            if (!cachedFile.exists()) {
                if (item.item.type == MediaType.Video) {
                    EncryptionManager.decryptVideo(
                        absolutePath = originalFile.absolutePath,
                        context = context,
                        iv = iv,
                        progress = {}
                    )
                } else {
                    EncryptionManager.decryptInputStream(
                        inputStream = originalFile.inputStream(),
                        outputStream = cachedFile.outputStream(),
                        fileSize = originalFile.length(),
                        iv = iv
                    )
                }
            }

            cachedFile.deleteOnExit()
            cachedPaths.add(Pair(cachedFile.absolutePath, item.item.type))
        }

        val hasVideos = cachedPaths.any {
            it.second == MediaType.Video
        }

        val fileUris = ArrayList(
            cachedPaths.map {
                FileProvider.getUriForFile(
                    context,
                    LAVENDER_FILE_PROVIDER_AUTHORITY,
                    File(it.first)
                )
            }
        )

        val intent = Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            type = if (hasVideos) "video/*" else "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            putExtra(
                Intent.EXTRA_STREAM,
                fileUris
            )
        }

        context.startActivity(
            Intent.createChooser(
                intent,
                context.resources.getString(R.string.secure_share_media)
            )
        )
    }

    override suspend fun permanentlyDelete(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        taskId: Int?
    ): Boolean {
        try {
            list.forEach { item ->
                val file = File(
                    context.appSecureFolderDir,
                    item.uri.substringAfterLast("/") // filename
                )

                file.delete()
                val thumbnail = file.secureThumbnailImage(context)
                thumbnail.delete()

                secureDao.deleteEntityBySecuredPath(securedPath = file.absolutePath)
                secureDao.deleteEntityBySecuredPath(securedPath = thumbnail.absolutePath)
            }
            return true
        } catch (e: Throwable) {
            Log.e(TAG, e.toString())
            e.printStackTrace()
            return false
        }
    }
}