package com.kaii.photos.helpers

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.compose.ui.util.fastMap
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.kaii.photos.R
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.database.entities.SecuredItemEntity
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.paging.PhotoLibraryUIModel
import com.kaii.photos.mediastore.LAVENDER_FILE_PROVIDER_AUTHORITY
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.copyUriToUri
import com.kaii.photos.mediastore.getIv
import com.kaii.photos.mediastore.getMediaStoreDataForIds
import com.kaii.photos.mediastore.getOriginalPath
import com.kaii.photos.mediastore.insertMedia
import com.kaii.photos.mediastore.setDateForMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "com.kaii.photos.helpers.ImageFunctions"

fun permanentlyDeletePhotoList(context: Context, list: List<Uri>) {
    if (list.isNotEmpty()) {
        val deleteRequest = MediaStore.createDeleteRequest(
            context.contentResolver,
            list
        )

        (context as Activity).startIntentSenderForResult(
            deleteRequest.intentSender,
            9997,
            null,
            0,
            0,
            0
        )
    }
}

fun shareImage(uri: Uri, context: Context, mimeType: String? = null) {
    val contentResolver = context.contentResolver

    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = mimeType ?: contentResolver.getType(uri)
        putExtra(Intent.EXTRA_STREAM, uri)
    }

    val chooserIntent = Intent.createChooser(shareIntent, null)
    context.startActivity(chooserIntent)
}

fun shareSecuredImage(absolutePath: String, context: Context) {
    val uri =
        FileProvider.getUriForFile(context, LAVENDER_FILE_PROVIDER_AUTHORITY, File(absolutePath))

    val intent = Intent().apply {
        action = Intent.ACTION_SEND
        type = context.contentResolver.getType(uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra(Intent.EXTRA_STREAM, uri)
    }

    context.startActivity(
        Intent.createChooser(
            intent,
            context.resources.getString(R.string.secure_share_media)
        )
    )
}

/** @param paths is a list of absolute paths and [MediaType]s of items */
fun shareMultipleSecuredImages(
    paths: List<Pair<String, MediaType>>,
    context: Context
) {
    val hasVideos = paths.any {
        it.second == MediaType.Video
    }

    val fileUris = ArrayList(
        paths.map {
            FileProvider.getUriForFile(context, LAVENDER_FILE_PROVIDER_AUTHORITY, File(it.first))
        }
    )

    val intent = Intent().apply {
        action = Intent.ACTION_SEND_MULTIPLE
        type = if (hasVideos) "video/*" else "image/*"
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra(Intent.EXTRA_STREAM, fileUris)
    }

    context.startActivity(
        Intent.createChooser(
            intent,
            context.resources.getString(R.string.secure_share_media)
        )
    )
}

@JvmName("moveSelectedItemsToSecureFolder")
suspend fun moveMediaToSecureFolder(
    list: List<SelectionManager.SelectedItem>,
    context: Context,
    applicationDatabase: MediaDatabase,
    onDone: () -> Unit
) = withContext(Dispatchers.IO) {
    val media = getMediaStoreDataForIds(
        ids = list.fastMap { it.id }.toSet(),
        context = context
    )

    moveMediaToSecureFolder(
        list = media.toList(),
        context = context,
        applicationDatabase = applicationDatabase,
        onDone = onDone
    )
}

@JvmName("moveMediaToSecureFolder")
suspend fun moveMediaToSecureFolder(
    list: List<MediaStoreData>,
    context: Context,
    applicationDatabase: MediaDatabase,
    onDone: () -> Unit
) = withContext(Dispatchers.IO) {
    val lastModified = System.currentTimeMillis()
    val metadataRetriever = MediaMetadataRetriever()

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

            addSecuredCachedMediaThumbnail(
                context = context,
                mediaItem = mediaItem,
                file = fileToBeHidden,
                metadataRetriever = metadataRetriever,
                applicationDatabase = applicationDatabase
            )

            // encrypt file data and write to secure folder path
            val iv =
                EncryptionManager.encryptInputStream(
                    fileToBeHidden.inputStream(),
                    destinationFile.outputStream()
                )

            applicationDatabase.securedItemEntityDao().insertEntity(
                SecuredItemEntity(
                    originalPath = mediaItem.absolutePath,
                    securedPath = copyToPath,
                    iv = iv
                )
            )

            // cleanup
            permanentlyDeletePhotoList(
                context = context,
                list = listOf(mediaItem.uri.toUri())
            )
        } catch (e: Throwable) {
            Log.e(TAG, e.toString())
            e.printStackTrace()

            applicationDatabase.securedItemEntityDao().insertEntity(
                SecuredItemEntity(
                    originalPath = mediaItem.absolutePath,
                    securedPath = copyToPath,
                    iv = ByteArray(0)
                )
            )
        }
    }

    onDone()
}

suspend fun moveImageOutOfLockedFolder(
    list: List<PhotoLibraryUIModel.SecuredMedia>,
    context: Context,
    applicationDatabase: MediaDatabase,
    onDone: () -> Unit
) {
    val contentResolver = context.contentResolver
    val restoredFilesDir = context.appRestoredFilesDir

    list.forEach { media ->
        val fileToBeRestored = File(media.item.absolutePath)
        val originalPath = media.bytes?.getOriginalPath() ?: restoredFilesDir

        Log.d(TAG, "ORIGINAL PATH $originalPath")

        val tempFile = File(context.cacheDir, fileToBeRestored.name)

        val iv = media.bytes?.getIv()
        try {
            if (iv != null) {
                EncryptionManager.decryptInputStream(
                    fileToBeRestored.inputStream(),
                    tempFile.outputStream(),
                    iv
                )
            } else {
                fileToBeRestored.inputStream().copyTo(tempFile.outputStream())
            }
        } catch (e: Throwable) {
            Log.e(TAG, e.toString())
            e.printStackTrace()
        }

        Log.d(TAG, "Base path ${originalPath.toBasePath()}")

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
            basePath = originalPath.toBasePath(),
            currentVolumes = MediaStore.getExternalVolumeNames(context),
            preserveDate = true,
            onInsert = { original, new ->
                contentResolver.copyUriToUri(original, new)
            }
        )?.let {
            try {
                fileToBeRestored.delete()
                tempFile.delete()
                applicationDatabase.securedItemEntityDao().deleteEntityBySecuredPath(media.item.absolutePath)

                val thumbnailFile =
                    getSecuredCacheImageForFile(file = fileToBeRestored, context = context)
                thumbnailFile.delete()
                applicationDatabase.securedItemEntityDao()
                    .deleteEntityBySecuredPath(thumbnailFile.absolutePath)
            } catch (e: Throwable) {
                Log.e(TAG, e.toString())
                e.printStackTrace()
            }
        }
    }

    onDone()
}

/** @param list is a list of the absolute path of every image to be deleted */
suspend fun permanentlyDeleteSecureFolderImageList(list: List<String>, context: Context) = withContext(Dispatchers.IO) {
    val dao = MediaDatabase.getInstance(context).securedItemEntityDao()

    try {
        list.forEach { path ->
            File(path).let { file ->
                file.delete()
                getSecuredCacheImageForFile(file = file, context = context).delete()
                dao.deleteEntityBySecuredPath(securedPath = path)
            }
        }
    } catch (e: Throwable) {
        Log.e(TAG, e.toString())
    }
}

/** @param destination where to copy said files to, should be relative
@param overrideDisplayName should not contain file extension */
suspend fun copyImageListToPath(
    context: Context,
    list: List<SelectionManager.SelectedItem>,
    destination: String,
    overwriteDate: Boolean,
    overrideDisplayName: ((displayName: String) -> String)? = null,
    onSingleItemDone: (media: MediaStoreData) -> Unit
): MutableList<Uri> = withContext(Dispatchers.IO) {
    val contentResolver = context.contentResolver

    val items = getMediaStoreDataForIds(
        ids = list.fastMap { it.id }.toSet(),
        context = context
    )

    val newUris = mutableListOf<Uri>()
    items.forEach { media ->
        contentResolver.insertMedia(
            context = context,
            media = media,
            destination = destination,
            basePath = media.absolutePath.toBasePath(),
            overrideDisplayName = if (overrideDisplayName != null) overrideDisplayName(media.displayName) else null,
            currentVolumes = MediaStore.getExternalVolumeNames(context),
            preserveDate = overwriteDate,
            onInsert = { original, new ->
                contentResolver.copyUriToUri(original, new)
                newUris.add(new)
            }
        )?.let {
            onSingleItemDone(media)
        }
    }

    return@withContext newUris
}