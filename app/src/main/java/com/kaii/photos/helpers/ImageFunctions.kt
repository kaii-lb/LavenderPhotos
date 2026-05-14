package com.kaii.photos.helpers

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.compose.ui.util.fastMap
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.database.entities.SecuredItemEntity
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.copyUriToUri
import com.kaii.photos.mediastore.getMediaStoreDataForIds
import com.kaii.photos.mediastore.insertMedia
import com.kaii.photos.mediastore.setDateForMedia
import com.kaii.photos.repositories.SecureRepository
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

            // encrypt file data and write to secure folder path
            val iv =
                EncryptionManager.encryptInputStream(
                    inputStream = fileToBeHidden.inputStream(),
                    outputStream = destinationFile.outputStream(),
                    fileSize = fileToBeHidden.length()
                )

            applicationDatabase.securedItemEntityDao().insertEntity(
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
                        dao = applicationDatabase.securedItemEntityDao()
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
                    dao = applicationDatabase.securedItemEntityDao()
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