package com.kaii.photos.file_management.secure

import android.content.Context
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import android.util.Log
import androidx.compose.ui.util.fastMap
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.kaii.photos.database.daos.SecuredMediaItemEntityDao
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.database.entities.SecuredItemEntity
import com.kaii.photos.helpers.EncryptionManager
import com.kaii.photos.helpers.appRestoredFilesDir
import com.kaii.photos.helpers.appSecureFolderDir
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.grid_management.toSecureMedia
import com.kaii.photos.helpers.parent
import com.kaii.photos.helpers.secureThumbnailImage
import com.kaii.photos.helpers.secureVideoThumbnailImage
import com.kaii.photos.mediastore.LAVENDER_FILE_PROVIDER_AUTHORITY
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.copyUriToUri
import com.kaii.photos.mediastore.getIv
import com.kaii.photos.mediastore.getMediaStoreDataForIds
import com.kaii.photos.mediastore.getOriginalPath
import com.kaii.photos.mediastore.insertMedia
import com.kaii.photos.mediastore.setDateForMedia
import com.kaii.photos.repositories.SecureRepository
import java.io.File

class LocalSecureManager(
    private val secureDao: SecuredMediaItemEntityDao
) : GenericSecureManager {
    companion object {
        private val TAG = LocalSecureManager::class.qualifiedName
    }

    override suspend fun secure(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ): List<SelectionManager.SelectedItem> {
        val media = getMediaStoreDataForIds(
            ids = list.fastMap { it.id }.toSet(),
            context = context
        ).toList()

        return secureImpl(context, media)
    }

    /** returns files to be permanently deleted */
    private suspend fun secureImpl(
        context: Context,
        list: List<MediaStoreData>
    ): List<SelectionManager.SelectedItem> {
        val lastModified = System.currentTimeMillis()
        val metadataRetriever = MediaMetadataRetriever()

        val toBeDeleted = mutableListOf<SelectionManager.SelectedItem>()

        list.forEach { mediaItem ->
            val fileToBeHidden = File(mediaItem.absolutePath)
            val copyToPath = context.appSecureFolderDir + "/" + fileToBeHidden.name
            try {
                // set last modified so item shows up in correct place in locked folder
                fileToBeHidden.setLastModified(lastModified)

                val destinationFile = File(copyToPath)

                context.contentResolver.setDateForMedia(
                    uri = mediaItem.uri.toUri(),
                    type = mediaItem.type,
                    dateTaken = mediaItem.dateTaken,
                    overwriteLastModified = false
                )

                // encrypt file data and write to secure folder path
                val iv =
                    EncryptionManager.encryptInputStream(
                        inputStream = fileToBeHidden.inputStream(),
                        outputStream = destinationFile.outputStream(),
                        fileSize = fileToBeHidden.length()
                    )

                secureDao.insertEntity(
                    SecuredItemEntity(
                        originalPath = mediaItem.absolutePath,
                        securedPath = copyToPath,
                        iv = iv
                    )
                )

                if (mediaItem.type == MediaType.Video) {
                    metadataRetriever.setDataSource(context, mediaItem.uri.toUri())

                    metadataRetriever.getScaledFrameAtTime(
                        -1L,
                        MediaMetadataRetriever.OPTION_CLOSEST,
                        1024,
                        1024
                    )?.let { bitmap ->
                        SecureRepository.addEncryptedThumbnail(
                            context = context,
                            thumbnail = bitmap,
                            file = destinationFile.secureVideoThumbnailImage(context),
                            dao = secureDao
                        )
                    }
                } else {
                    val thumbnail = Glide
                        .with(context)
                        .asBitmap()
                        .load(mediaItem.uri)
                        .override(512)
                        .submit()
                        .get()

                    SecureRepository.addEncryptedThumbnail(
                        context = context,
                        thumbnail = thumbnail,
                        file = destinationFile.secureVideoThumbnailImage(context),
                        dao = secureDao
                    )
                }

                // cleanup
                toBeDeleted.add(
                    SelectionManager.SelectedItem(
                        id = mediaItem.id,
                        uri = mediaItem.uri,
                        immichUrl = mediaItem.immichUrl,
                        isImage = mediaItem.type == MediaType.Image,
                        parentPath = mediaItem.parentPath
                    )
                )
            } catch (e: Throwable) {
                Log.e(TAG, e.toString())
                e.printStackTrace()

                secureDao.insertEntity(
                    SecuredItemEntity(
                        originalPath = mediaItem.absolutePath,
                        securedPath = copyToPath,
                        iv = ByteArray(0)
                    )
                )
            }
        }

        return toBeDeleted
    }

    override suspend fun restore(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ): Boolean {
        val contentResolver = context.contentResolver
        val restoredFilesDir = context.appRestoredFilesDir

        val securedMedia = list.toSecureMedia(context)

        var successes = 0

        securedMedia.forEach { media ->
            val fileToBeRestored = File(media.item.absolutePath)
            val originalPath = media.bytes?.getOriginalPath() ?: restoredFilesDir

            val tempFile = File(context.cacheDir, fileToBeRestored.name)

            val iv = media.bytes?.getIv()
            try {
                if (iv != null) {
                    EncryptionManager.decryptInputStream(
                        inputStream = fileToBeRestored.inputStream(),
                        outputStream = tempFile.outputStream(),
                        fileSize = fileToBeRestored.length(),
                        iv = iv
                    )
                } else {
                    fileToBeRestored.inputStream().copyTo(tempFile.outputStream())
                }
            } catch (e: Throwable) {
                Log.e(TAG, e.toString())
                e.printStackTrace()
            }

            contentResolver.insertMedia(
                context = context,
                media = media.item.copy(
                    uri = FileProvider.getUriForFile(
                        context,
                        LAVENDER_FILE_PROVIDER_AUTHORITY,
                        tempFile
                    ).toString()
                ),
                destination = originalPath.parent(),
                currentVolumes = MediaStore.getExternalVolumeNames(context),
                preserveDate = true,
                onInsert = { original, new ->
                    contentResolver.copyUriToUri(original, new)
                }
            )?.let {
                try {
                    fileToBeRestored.delete()
                    tempFile.delete()
                    secureDao.deleteEntityBySecuredPath(media.item.absolutePath)

                    val thumbnailFile = fileToBeRestored.secureThumbnailImage(context)
                    thumbnailFile.delete()
                    secureDao.deleteEntityBySecuredPath(thumbnailFile.absolutePath)

                    fileToBeRestored.secureVideoThumbnailImage(context).let {
                        if (it.exists()) it.delete()
                    }

                    successes += 1
                } catch (e: Throwable) {
                    Log.e(TAG, e.toString())
                    e.printStackTrace()
                }
            }
        }

        return successes == securedMedia.size
    }
}