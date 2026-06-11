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
import com.kaii.photos.helpers.permanentlyDeletePhotoList
import com.kaii.photos.helpers.secureThumbnailImage
import com.kaii.photos.helpers.secureVideoThumbnailImage
import com.kaii.photos.helpers.uniqueSecureDestination
import com.kaii.photos.mediastore.LAVENDER_FILE_PROVIDER_AUTHORITY
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.copyUriToUri
import com.kaii.photos.mediastore.getIv
import com.kaii.photos.mediastore.getMediaStoreDataForIds
import com.kaii.photos.mediastore.getOriginalPath
import com.kaii.photos.mediastore.insertMedia
import com.kaii.photos.mediastore.setDateForMedia
import com.kaii.photos.repositories.SecureRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
            // reuse this original's existing secured path if it's already been secured (re-secure after
            // a cancelled delete), so we overwrite one file + REPLACE one row instead of orphaning the
            // first ciphertext. only dedup the name for a genuinely new original colliding on disk
            val destinationFile = secureDao.getSecuredPathFromOriginalPath(mediaItem.absolutePath)
                ?.let { File(it) }
                ?: uniqueSecureDestination(context, fileToBeHidden.name)
            val copyToPath = destinationFile.absolutePath
            // only set to ByteArray(0) if the encryption itself failed; keep null initially so we
            // don't overwrite a valid iv when a later step (thumbnail generation) throws
            var iv: ByteArray? = null
            try {
                // set last modified so item shows up in correct place in locked folder
                fileToBeHidden.setLastModified(lastModified)

                context.contentResolver.setDateForMedia(
                    uri = mediaItem.uri.toUri(),
                    type = mediaItem.type,
                    dateTaken = mediaItem.dateTaken,
                    overwriteLastModified = false
                )

                // encrypt file data and write to secure folder path
                iv =
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

                // only store an empty iv if encryption itself produced nothing; don't overwrite a
                // valid iv already persisted above when a later step (thumbnail generation) fails
                secureDao.insertEntity(
                    SecuredItemEntity(
                        originalPath = mediaItem.absolutePath,
                        securedPath = copyToPath,
                        iv = iv ?: ByteArray(0)
                    )
                )
            }
        }

        return toBeDeleted
    }

    /**
     * Bulk-secure already-resolved media (used by the secure-folder migration flow). Unlike [secure],
     * the caller supplies [MediaStoreData] directly and each source is deleted on success rather than
     * returned as a to-delete list.
     */
    suspend fun moveMediaToSecureFolder(
        context: Context,
        list: List<MediaStoreData>,
        onDone: () -> Unit
    ) = withContext(Dispatchers.IO) {
        // sort the batch newest-first; the incoming set is unordered so don't trust iteration order.
        // videos can report dateTaken == 0, so fall back to dateModified then absolutePath
        val ordered = list.sortedWith(
            compareByDescending<MediaStoreData> { it.dateTaken }
                .thenByDescending { it.dateModified }
                .thenByDescending { it.absolutePath }
        )

        // the secure grid sorts by each encrypted file's lastModified (truncated to whole seconds via /1000),
        // so stamp destinations 1000ms apart, all above the highest existing mtime, to keep the batch order
        // and float the most recently secured items to the top
        val existingMaxMtime = File(context.appSecureFolderDir).listFiles()?.maxOfOrNull { it.lastModified() } ?: 0L
        val baseModified = maxOf(System.currentTimeMillis(), existingMaxMtime + ordered.size * 1000L)

        val metadataRetriever = MediaMetadataRetriever()

        ordered.forEachIndexed { index, mediaItem ->
            val fileToBeHidden = File(mediaItem.absolutePath)
            // reuse this original's existing secured path if already secured, else dedup a new name
            // (see secureImpl) so we never orphan a row-less ciphertext file
            val destinationFile = secureDao.getSecuredPathFromOriginalPath(mediaItem.absolutePath)
                ?.let { File(it) }
                ?: uniqueSecureDestination(context, fileToBeHidden.name)
            val copyToPath = destinationFile.absolutePath
            // only set to ByteArray(0) if the encryption itself failed; keep null initially so we
            // don't overwrite a valid iv when a later step (thumbnail generation) throws
            var iv: ByteArray? = null
            try {
                context.contentResolver.setDateForMedia(
                    uri = mediaItem.uri.toUri(),
                    type = mediaItem.type,
                    dateTaken = mediaItem.dateTaken,
                    overwriteLastModified = false
                )

                // encrypt file data and write to secure folder path
                iv =
                    EncryptionManager.encryptInputStream(
                        inputStream = fileToBeHidden.inputStream(),
                        outputStream = destinationFile.outputStream(),
                        fileSize = fileToBeHidden.length()
                    )

                // stamp the encrypted file (what the grid sorts by) with this item's slot, newest = highest.
                // must run after the encrypt above sets its own write-time mtime, else it gets overwritten
                destinationFile.setLastModified(baseModified - index * 1000L)

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

                    // passing secureVideoThumbnailImage for an image is intentional: addEncryptedThumbnail
                    // re-applies secureThumbnailImage() and both collapse to the same secure_thumbnail_cache
                    // png the grid reads. don't "simplify" to destinationFile without also changing that
                    SecureRepository.addEncryptedThumbnail(
                        context = context,
                        thumbnail = thumbnail,
                        file = destinationFile.secureVideoThumbnailImage(context),
                        dao = secureDao
                    )
                }

                // cleanup
                permanentlyDeletePhotoList(
                    context = context,
                    list = listOf(mediaItem.uri.toUri())
                )
            } catch (e: Throwable) {
                Log.e(TAG, e.toString())
                e.printStackTrace()

                // only store an empty iv if encryption itself produced nothing; don't overwrite a
                // valid iv already persisted above when a later step (thumbnail generation) fails
                secureDao.insertEntity(
                    SecuredItemEntity(
                        originalPath = mediaItem.absolutePath,
                        securedPath = copyToPath,
                        iv = iv ?: ByteArray(0)
                    )
                )
            }
        }

        onDone()
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
            // a missing iv with an existing db row means the file is encrypted but its iv couldn't be
            // recovered. raw-copying then deleting (the else branch) would write ciphertext into the
            // gallery and destroy the only copy, so refuse: skip this item and leave it in the vault
            if (iv == null && secureDao.getIvFromSecuredPath(fileToBeRestored.absolutePath) != null) {
                Log.e(TAG, "Refusing to restore ${media.item.displayName}: iv unrecoverable, keeping it secured")
                return@forEach
            }
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